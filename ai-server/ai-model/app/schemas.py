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


class MeetingSummaryInput(BaseModel):
    transcript: str = Field(
        ...,
        min_length=1,
        max_length=200000,
        description="Raw meeting transcript text.",
    )
    language: str = Field(
        default="Korean",
        min_length=2,
        max_length=30,
        description="Output language for the summary.",
    )


class ActionItem(BaseModel):
    task: str = Field(..., min_length=1, description="Action item description.")
    owner: str = Field(..., min_length=1, description="Owner of the task.")
    due_date: str = Field(..., min_length=1, description="Due date for the task.")


class MeetingSummaryOutput(BaseModel):
    summary: str = Field(..., min_length=1)
    decisions: list[str] = Field(default_factory=list)
    action_items: list[ActionItem] = Field(default_factory=list)
    risks: list[str] = Field(default_factory=list)
    open_questions: list[str] = Field(default_factory=list)


class ErrorResponse(BaseModel):
    code: str
    message: str
    detail: str | None = None
