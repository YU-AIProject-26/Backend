from typing import Optional

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_ollama import ChatOllama

try:
    from .base import BaseChain
except ImportError:
    from base import BaseChain


DEFAULT_MODEL = "eeve_q40:latest"
DEFAULT_NUM_PREDICT = 512
DEFAULT_REPEAT_PENALTY = 1.1
DEFAULT_STOP = ["<s>", "</s>", "<|im_end|>", "Human:"]


def create_llm(model: str, temperature: float):
    return ChatOllama(
        model=model,
        temperature=temperature,
        num_predict=DEFAULT_NUM_PREDICT,
        repeat_penalty=DEFAULT_REPEAT_PENALTY,
        stop=DEFAULT_STOP,
    )


class TopicChain(BaseChain):
    def __init__(
        self,
        model: str = DEFAULT_MODEL,
        temperature: float = 0,
        system_prompt: Optional[str] = None,
        **kwargs,
    ):
        super().__init__(model, temperature, **kwargs)
        self.system_prompt = (
            system_prompt
            or "You are a helpful assistant. Explain the given topic briefly in Korean."
        )

    def setup(self):
        llm = create_llm(self.model, self.temperature)

        prompt = ChatPromptTemplate.from_messages(
            [
                ("system", self.system_prompt),
                ("user", "Topic: {topic}"),
            ]
        )

        return prompt | llm | StrOutputParser()


class ChatChain(BaseChain):
    def __init__(
        self,
        model: str = DEFAULT_MODEL,
        temperature: float = 0.3,
        system_prompt: Optional[str] = None,
        **kwargs,
    ):
        super().__init__(model, temperature, **kwargs)
        self.system_prompt = (
            system_prompt
            or "You are a helpful AI assistant. Always answer in Korean."
        )

    def setup(self):
        llm = create_llm(self.model, self.temperature)

        prompt = ChatPromptTemplate.from_messages(
            [
                ("system", self.system_prompt),
                MessagesPlaceholder(variable_name="messages"),
            ]
        )

        return prompt | llm | StrOutputParser()


class LLM(BaseChain):
    def __init__(self, model: str = DEFAULT_MODEL, temperature: float = 0, **kwargs):
        super().__init__(model, temperature, **kwargs)

    def setup(self):
        return create_llm(self.model, self.temperature)


class Translator(BaseChain):
    def __init__(
        self,
        model: str = DEFAULT_MODEL,
        temperature: float = 0,
        system_prompt: Optional[str] = None,
        **kwargs,
    ):
        super().__init__(model, temperature, **kwargs)
        self.system_prompt = (
            system_prompt
            or "You are a helpful assistant. Translate the given sentence into Korean."
        )

    def setup(self):
        llm = create_llm(self.model, self.temperature)

        prompt = ChatPromptTemplate.from_messages(
            [
                ("system", self.system_prompt),
                ("user", "Sentence: {input}"),
            ]
        )

        return prompt | llm | StrOutputParser()


prompt = ChatPromptTemplate.from_template("Explain this topic briefly in Korean: {topic}")
llm = create_llm(DEFAULT_MODEL, 0)
chain = prompt | llm | StrOutputParser()
