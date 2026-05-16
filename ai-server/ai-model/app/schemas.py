from typing import List, Union

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from pydantic import BaseModel, Field


class InputChat(BaseModel):
    messages: List[Union[HumanMessage, AIMessage, SystemMessage]] = Field(
        ...,
        description="The chat messages representing the current conversation.",
    )


class AskInput(BaseModel):
    topic: str = Field(
        ...,
        min_length=1,
        max_length=2000,
        description="Question or topic to send to the model.",
    )


class ErrorResponse(BaseModel):
    code: str
    message: str
    detail: str | None = None
