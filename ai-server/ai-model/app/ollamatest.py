import sys

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama


if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")


llm = ChatOllama(model="eeve_q40:latest")

prompt = ChatPromptTemplate.from_template("{topic}에 대하여 간략히 설명해 줘.")

chain = prompt | llm | StrOutputParser()


if __name__ == "__main__":
    print(chain.invoke({"topic": "deep learning"}))
