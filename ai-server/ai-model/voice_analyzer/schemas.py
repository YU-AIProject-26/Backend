from pydantic import BaseModel, Field
from typing import List

class ActionItem(BaseModel):
    assignee: str = Field(description="업무 담당자")
    task: str = Field(description="수행할 업무 내용")
    deadline: str = Field(description="업무 마감 기한 (명시되지 않은 경우 '미정'으로 표기)")

class MeetingAnalysis(BaseModel):
    summary: str = Field(
        description="어떤 기술 또는 사건, 일정에 대해 어떤 결정이 내려졌는지 구체적이고 전문적으로 요약한 문장"
    )
    action_items: List[ActionItem] = Field(
        description="대화 맥락과 화자 태그를 파악하여 도출된 팀원별 할 일 목록"
    )
    tags: List[str] = Field(
        description="대화에 등장한 용어 및 프로젝트 핵심 키워드 3개"
    )
