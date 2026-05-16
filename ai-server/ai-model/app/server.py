import os
from pathlib import Path
from typing import List, Union

import uvicorn
from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from fastapi.responses import RedirectResponse
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langserve import add_routes
from pydantic import BaseModel, Field

try:
    from .chains import ChatChain, LLM, TopicChain, Translator
    from .rag import RagChain
except ImportError:
    from chains import ChatChain, LLM, TopicChain, Translator
    from rag import RagChain


load_dotenv()

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["*"],
)


class InputChat(BaseModel):
    messages: List[Union[HumanMessage, AIMessage, SystemMessage]] = Field(
        ...,
        description="The chat messages representing the current conversation.",
    )


class AskInput(BaseModel):
    topic: str = Field(..., description="Question or topic to send to the model.")


topic_chain = TopicChain().create()


@app.get("/")
async def home():
    return HTMLResponse(
        """
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>AI Model</title>
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
    <h1>AI Model</h1>
    <form id="ask-form">
      <textarea id="topic" placeholder="질문을 입력하세요">딥러닝에 대해서 알려줘</textarea>
      <button id="submit" type="submit">질문하기</button>
    </form>
    <pre id="answer">답변이 여기에 표시됩니다.</pre>
  </main>
  <script>
    const form = document.getElementById("ask-form");
    const topic = document.getElementById("topic");
    const submit = document.getElementById("submit");
    const answer = document.getElementById("answer");

    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      submit.disabled = true;
      answer.textContent = "생성 중...";

      try {
        const response = await fetch("/ask", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ topic: topic.value })
        });
        const data = await response.json();
        answer.textContent = data.answer || data.detail || "응답이 비어 있습니다.";
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


@app.post("/ask")
async def ask(payload: AskInput):
    return {"answer": await topic_chain.ainvoke({"topic": payload.topic})}


@app.get("/topic/playground", include_in_schema=False)
@app.get("/chat/playground", include_in_schema=False)
async def playground_redirect():
    return RedirectResponse("/")


add_routes(app, Translator().create(), path="/translate", disabled_endpoints=["playground"])
add_routes(app, LLM().create(), path="/llm", disabled_endpoints=["playground"])
add_routes(app, topic_chain, path="/topic", disabled_endpoints=["playground"])
add_routes(
    app,
    ChatChain().create().with_types(input_type=InputChat),
    path="/chat",
    enable_feedback_endpoint=True,
    enable_public_trace_link_endpoint=True,
    playground_type="chat",
    disabled_endpoints=["playground"],
)


def find_rag_file() -> Path | None:
    env_path = os.getenv("RAG_FILE_PATH")
    if env_path:
        path = Path(env_path).expanduser()
        if not path.is_absolute():
            path = Path.cwd() / path
        return path if path.is_file() else None

    data_dir = Path(__file__).resolve().parent / "data"
    return next(data_dir.glob("*.pdf"), None) if data_dir.is_dir() else None


rag_file = find_rag_file()
if rag_file:
    add_routes(app, RagChain(file_path=str(rag_file)).create(), path="/rag")
else:
    print("Skipping /rag route because no PDF was found. Set RAG_FILE_PATH or add a PDF to app/data.")


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
