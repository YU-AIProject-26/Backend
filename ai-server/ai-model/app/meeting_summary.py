import json
from typing import Any, Optional

from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_text_splitters import RecursiveCharacterTextSplitter

try:
    from .chains import create_llm
    from .config import get_settings
    from .schemas import ActionItem, MeetingSummaryOutput
except ImportError:
    from chains import create_llm
    from config import get_settings
    from schemas import ActionItem, MeetingSummaryOutput


SETTINGS = get_settings()


class MeetingSummaryChain:
    def __init__(
        self,
        model: str = SETTINGS.model_name,
        temperature: float = SETTINGS.meeting_temperature,
        chunk_size: int = SETTINGS.meeting_chunk_size,
        chunk_overlap: int = SETTINGS.meeting_chunk_overlap,
        max_chunks: int = SETTINGS.meeting_max_chunks,
        parse_retries: int = SETTINGS.meeting_parse_retries,
    ):
        self.llm = create_llm(model=model, temperature=temperature)
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
        )
        self.max_chunks = max_chunks
        self.parse_retries = parse_retries
        self.output_parser = PydanticOutputParser(pydantic_object=MeetingSummaryOutput)

        self.map_prompt = ChatPromptTemplate.from_messages(
            [
                (
                    "system",
                    (
                        "You are a precise meeting assistant. Summarize only facts in the text. "
                        "Do not invent details. Write in {language}. "
                        "If owner or due date is unknown, write '미정'."
                    ),
                ),
                (
                    "user",
                    (
                        "Chunk {chunk_index}/{chunk_total}\n"
                        "Meeting transcript chunk:\n{chunk}\n\n"
                        "Return short bullet-style notes with sections:\n"
                        "1) key points\n"
                        "2) decisions\n"
                        "3) action items (task, owner, due date)\n"
                        "4) risks\n"
                        "5) open questions"
                    ),
                ),
            ]
        )

        self.reduce_prompt = ChatPromptTemplate.from_messages(
            [
                (
                    "system",
                    (
                        "You are an expert meeting summarizer. "
                        "Combine partial summaries into one final result. "
                        "Use only the given information. "
                        "Return valid JSON only."
                    ),
                ),
                (
                    "user",
                    (
                        "Language: {language}\n\n"
                        "Partial summaries:\n{partial_summaries}\n\n"
                        "Format instructions:\n{format_instructions}\n\n"
                        "Rules:\n"
                        "- Keep summary concise.\n"
                        "- Decisions should be concrete statements.\n"
                        "- Every action item must include task, owner, due_date.\n"
                        "- If owner or due_date is unknown, use '미정'."
                    ),
                ),
            ]
        )

        self.retry_prompt = ChatPromptTemplate.from_messages(
            [
                (
                    "system",
                    "Fix the JSON so it strictly follows the format instructions.",
                ),
                (
                    "user",
                    (
                        "Invalid JSON output:\n{invalid_output}\n\n"
                        "Parser error:\n{parse_error}\n\n"
                        "Format instructions:\n{format_instructions}\n\n"
                        "Return valid JSON only."
                    ),
                ),
            ]
        )

    @staticmethod
    def _message_to_text(message: Any) -> str:
        content = getattr(message, "content", message)
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            parts: list[str] = []
            for item in content:
                if isinstance(item, dict) and item.get("text"):
                    parts.append(str(item["text"]))
                else:
                    parts.append(str(item))
            return "\n".join(parts)
        return str(content)

    @staticmethod
    def _normalize_list(items: list[str]) -> list[str]:
        normalized: list[str] = []
        seen: set[str] = set()
        for item in items:
            cleaned = item.strip()
            if not cleaned:
                continue
            if cleaned in seen:
                continue
            seen.add(cleaned)
            normalized.append(cleaned)
        return normalized

    @staticmethod
    def _extract_json_block(raw_text: str) -> str | None:
        start = raw_text.find("{")
        end = raw_text.rfind("}")
        if start == -1 or end == -1 or end <= start:
            return None
        return raw_text[start : end + 1]

    def _manual_parse_output(self, raw_text: str) -> MeetingSummaryOutput:
        json_block = self._extract_json_block(raw_text)
        if not json_block:
            raise ValueError("No JSON object found in LLM output")

        candidates = [json_block]
        if '\\"' in json_block:
            candidates.append(json_block.replace('\\"', '"'))

        parsed_obj: Optional[dict[str, Any]] = None
        for candidate in candidates:
            try:
                parsed = json.loads(candidate)
                if isinstance(parsed, str):
                    parsed = json.loads(parsed)
                if isinstance(parsed, dict):
                    parsed_obj = parsed
                    break
            except Exception:
                continue

        if parsed_obj is None:
            raise ValueError("Failed to decode JSON object from LLM output")

        if "open_questions" not in parsed_obj:
            if "open_question" in parsed_obj:
                parsed_obj["open_questions"] = parsed_obj.pop("open_question")
            elif "openQuestions" in parsed_obj:
                parsed_obj["open_questions"] = parsed_obj.pop("openQuestions")

        return MeetingSummaryOutput.model_validate(parsed_obj)

    def _split_transcript(self, transcript: str) -> list[str]:
        chunks = self.splitter.split_text(transcript)
        if not chunks:
            return [transcript]

        if len(chunks) <= self.max_chunks:
            return chunks

        head = chunks[: self.max_chunks - 1]
        tail = "\n".join(chunks[self.max_chunks - 1 :])
        return [*head, tail]

    async def _summarize_chunk(
        self,
        chunk: str,
        chunk_index: int,
        chunk_total: int,
        language: str,
    ) -> str:
        messages = self.map_prompt.format_messages(
            language=language,
            chunk=chunk,
            chunk_index=chunk_index,
            chunk_total=chunk_total,
        )
        response = await self.llm.ainvoke(messages)
        return self._message_to_text(response)

    def _normalize_output(self, output: MeetingSummaryOutput) -> MeetingSummaryOutput:
        output.summary = output.summary.strip()
        output.decisions = self._normalize_list(output.decisions)
        output.risks = self._normalize_list(output.risks)
        output.open_questions = self._normalize_list(output.open_questions)

        normalized_actions = []
        for action in output.action_items:
            task = action.task.strip() or "미정"
            owner = action.owner.strip() or "미정"
            due_date = action.due_date.strip() or "미정"
            normalized_actions.append(
                ActionItem(
                    task=task,
                    owner=owner,
                    due_date=due_date,
                )
            )
        output.action_items = normalized_actions
        return output

    async def _reduce_summaries(
        self,
        partial_summaries: str,
        language: str,
    ) -> MeetingSummaryOutput:
        format_instructions = self.output_parser.get_format_instructions()
        messages = self.reduce_prompt.format_messages(
            language=language,
            partial_summaries=partial_summaries,
            format_instructions=format_instructions,
        )

        response = await self.llm.ainvoke(messages)
        raw_text = self._message_to_text(response)

        for attempt in range(self.parse_retries + 1):
            try:
                parsed = self.output_parser.parse(raw_text)
                return self._normalize_output(parsed)
            except Exception as exc:
                try:
                    parsed = self._manual_parse_output(raw_text)
                    return self._normalize_output(parsed)
                except Exception:
                    pass

                if attempt >= self.parse_retries:
                    raise ValueError(f"Failed to parse summary output: {exc}") from exc

                retry_messages = self.retry_prompt.format_messages(
                    invalid_output=raw_text,
                    parse_error=str(exc),
                    format_instructions=format_instructions,
                )
                retry_response = await self.llm.ainvoke(retry_messages)
                raw_text = self._message_to_text(retry_response)

        raise ValueError("Unexpected parser retry flow error")

    async def ainvoke(self, payload: dict[str, Any]) -> dict[str, Any]:
        transcript = str(payload.get("transcript", "")).strip()
        language = str(payload.get("language", "Korean")).strip() or "Korean"
        if not transcript:
            raise ValueError("transcript is required")

        chunks = self._split_transcript(transcript)
        partial_summaries: list[str] = []
        chunk_total = len(chunks)
        for index, chunk in enumerate(chunks, start=1):
            partial = await self._summarize_chunk(
                chunk=chunk,
                chunk_index=index,
                chunk_total=chunk_total,
                language=language,
            )
            partial_summaries.append(f"[chunk {index}]\n{partial}")

        final_output = await self._reduce_summaries(
            partial_summaries="\n\n".join(partial_summaries),
            language=language,
        )
        return final_output.model_dump()
