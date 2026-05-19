import logging
from pathlib import Path
from urllib.error import URLError
from urllib.request import urlopen

import uvicorn
from dotenv import load_dotenv
from fastapi import FastAPI, Response, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, RedirectResponse
from langserve import add_routes

from voice_analyzer.pipeline import lifespan, analyze_voice_meeting

try:
    from .chains import ChatChain, LLM, TopicChain, Translator
    from .config import get_settings
    from .exceptions import register_exception_handlers
    from .logging_config import configure_logging
    from .meeting_summary import MeetingSummaryChain
    from .rag import RagChain
    from .schemas import AskInput, InputChat, MeetingSummaryInput
except ImportError:
    from chains import ChatChain, LLM, TopicChain, Translator
    from config import get_settings
    from exceptions import register_exception_handlers
    from logging_config import configure_logging
    from meeting_summary import MeetingSummaryChain
    from rag import RagChain
    from schemas import AskInput, InputChat, MeetingSummaryInput


load_dotenv()
settings = get_settings()
configure_logging(settings.log_level)
logger = logging.getLogger(__name__)

app = FastAPI(title=settings.app_name, lifespan=lifespan)
register_exception_handlers(app)

allow_credentials = not (len(settings.cors_origins) == 1 and settings.cors_origins[0] == "*")
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=allow_credentials,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["*"],
)


def build_topic_chain():
    return TopicChain(
        model=settings.model_name,
        temperature=settings.model_temperature,
    ).create()


topic_chain = build_topic_chain()
meeting_summary_chain = MeetingSummaryChain(
    model=settings.model_name,
    temperature=settings.meeting_temperature,
    chunk_size=settings.meeting_chunk_size,
    chunk_overlap=settings.meeting_chunk_overlap,
    max_chunks=settings.meeting_max_chunks,
    parse_retries=settings.meeting_parse_retries,
)


def find_rag_file() -> Path | None:
    if settings.rag_file_path:
        path = Path(settings.rag_file_path).expanduser()
        if not path.is_absolute():
            path = Path.cwd() / path
        return path if path.is_file() else None

    data_dir = Path(__file__).resolve().parent / "data"
    return next(data_dir.glob("*.pdf"), None) if data_dir.is_dir() else None


def is_ollama_ready() -> tuple[bool, str]:
    ollama_url = f"{settings.ollama_base_url.rstrip('/')}/api/tags"
    try:
        with urlopen(ollama_url, timeout=3) as response:
            if response.status < 400:
                return True, "ok"
            return False, f"unexpected status: {response.status}"
    except URLError as exc:
        return False, str(exc.reason)
    except Exception as exc:
        return False, str(exc)


@app.get("/")
async def home():
    return HTMLResponse(
        """
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>AI Model Server</title>
  <style>
    body { margin: 0; font-family: Segoe UI, sans-serif; background: #f6f7f9; color: #15171a; }
    main { max-width: 820px; margin: 0 auto; padding: 40px 20px; }
    h1 { font-size: 28px; margin: 0 0 16px; }
    form { display: grid; gap: 12px; }
    textarea { min-height: 120px; padding: 14px; font: inherit; border: 1px solid #cfd6df; border-radius: 8px; resize: vertical; }
    button { width: fit-content; padding: 10px 16px; font: inherit; font-weight: 600; color: white; background: #1f6feb; border: 0; border-radius: 8px; cursor: pointer; }
    button:disabled { opacity: .6; cursor: wait; }
    pre { margin-top: 24px; padding: 18px; white-space: pre-wrap; line-height: 1.55; background: white; border: 1px solid #d8dee7; border-radius: 8px; }
  </style>
</head>
<body>
  <main>
    <h1>AI Model Server</h1>
    <form id="ask-form">
      <textarea id="topic" placeholder="Type your question here"></textarea>
      <button id="submit" type="submit">Ask</button>
    </form>
    <pre id="answer">Response will be shown here.</pre>
  </main>
  <script>
    const form = document.getElementById("ask-form");
    const topic = document.getElementById("topic");
    const submit = document.getElementById("submit");
    const answer = document.getElementById("answer");

    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      submit.disabled = true;
      answer.textContent = "Generating...";

      try {
        const response = await fetch("/ask", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ topic: topic.value })
        });
        const data = await response.json();
        answer.textContent = data.answer || data.detail || "No response returned.";
      } catch (error) {
        answer.textContent = error.message;
      } finally {
        submit.disabled = false;
      }
    });
  </script>
</body>
</html>
        """
    )


@app.get("/health")
async def health():
    return {"status": "ok", "service": settings.app_name}


@app.get("/ready")
async def ready(response: Response):
    ollama_ok, ollama_detail = is_ollama_ready()
    ready_state = ollama_ok
    if not ready_state:
        response.status_code = 503

    return {
        "status": "ready" if ready_state else "not_ready",
        "checks": {
            "ollama": {"ok": ollama_ok, "detail": ollama_detail},
        },
    }


@app.post("/ask")
async def ask(payload: AskInput):
    cleaned_topic = payload.topic.strip()
    return {"answer": await topic_chain.ainvoke({"topic": cleaned_topic})}


@app.post("/meeting/summary")
async def summarize_meeting(payload: MeetingSummaryInput):
    try:
        return await meeting_summary_chain.ainvoke(payload.model_dump())
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@app.get("/topic/playground", include_in_schema=False)
@app.get("/chat/playground", include_in_schema=False)
async def playground_redirect():
    return RedirectResponse("/")


add_routes(
    app,
    Translator(model=settings.model_name, temperature=settings.model_temperature).create(),
    path="/translate",
    disabled_endpoints=["playground"],
)
add_routes(
    app,
    LLM(model=settings.model_name, temperature=settings.model_temperature).create(),
    path="/llm",
    disabled_endpoints=["playground"],
)
add_routes(app, topic_chain, path="/topic", disabled_endpoints=["playground"])
add_routes(
    app,
    ChatChain(model=settings.model_name).create().with_types(input_type=InputChat),
    path="/chat",
    enable_feedback_endpoint=True,
    enable_public_trace_link_endpoint=True,
    playground_type="chat",
    disabled_endpoints=["playground"],
)

@app.post("/api/analyze-meeting", summary="음성 회의록 화자분리 및 요약 분석")
async def analyze_meeting(file: UploadFile = File(...)):
    """
    프론트엔드 또는 백엔드에서 오디오 파일을 POST로 전송하면,
    화자 분리 -> STT -> LangChain 요약 후 회의록 JSON 반환.
    """
    return await analyze_voice_meeting(file)

rag_file = find_rag_file()
if rag_file:
    try:
        add_routes(
            app,
            RagChain(
                file_path=str(rag_file),
                model=settings.model_name,
                embedding_model=settings.rag_embedding_model,
            ).create(),
            path="/rag",
        )
        logger.info("RAG route enabled with %s", rag_file)
    except Exception as exc:
        logger.exception("Failed to initialize RAG route: %s", exc)
else:
    logger.warning("Skipping /rag route because no PDF was found.")


if __name__ == "__main__":
    uvicorn.run(app, host=settings.host, port=settings.port)
