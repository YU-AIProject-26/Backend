from pathlib import Path
from typing import Optional

from langchain_community.document_loaders import PDFPlumberLoader
from langchain_community.vectorstores.faiss import FAISS
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import load_prompt
from langchain_core.runnables import RunnablePassthrough
from langchain_ollama import ChatOllama, OllamaEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

try:
    from .base import BaseChain
except ImportError:
    from base import BaseChain


DEFAULT_MODEL = "eeve_q40:latest"
DEFAULT_EMBEDDING_MODEL = "bge-m3"
DEFAULT_STOP = ["<s>", "</s>", "<|im_end|>", "Human:"]


def format_docs(docs):
    return "\n\n".join(
        f"<document><content>{doc.page_content}</content>"
        f"<page>{doc.metadata.get('page')}</page>"
        f"<source>{doc.metadata.get('source')}</source></document>"
        for doc in docs
    )


class RagChain(BaseChain):
    def __init__(
        self,
        model: str = DEFAULT_MODEL,
        temperature: float = 0.3,
        system_prompt: Optional[str] = None,
        file_path: Optional[str] = None,
        embedding_model: str = DEFAULT_EMBEDDING_MODEL,
        **kwargs,
    ):
        super().__init__(model, temperature, **kwargs)
        self.system_prompt = (
            system_prompt
            or "You are a helpful AI assistant. Answer in Korean based on the provided context."
        )
        self.file_path = file_path
        self.embedding_model = embedding_model

    def setup(self):
        if not self.file_path:
            raise ValueError("file_path is required")

        pdf_path = Path(self.file_path).expanduser()
        if not pdf_path.is_file():
            raise ValueError(f"File path {pdf_path} is not a valid file")

        text_splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
        docs = PDFPlumberLoader(str(pdf_path)).load_and_split(text_splitter=text_splitter)

        embeddings = OllamaEmbeddings(model=self.embedding_model)
        vectorstore = FAISS.from_documents(docs, embedding=embeddings)
        retriever = vectorstore.as_retriever()

        prompt_path = Path(__file__).resolve().parent / "prompts" / "rag-exaone.yaml"
        prompt = load_prompt(str(prompt_path), encoding="utf-8")
        llm = ChatOllama(
            model=self.model,
            temperature=self.temperature,
            num_predict=512,
            repeat_penalty=1.1,
            stop=DEFAULT_STOP,
        )

        return (
            {"context": retriever | format_docs, "question": RunnablePassthrough()}
            | prompt
            | llm
            | StrOutputParser()
        )
