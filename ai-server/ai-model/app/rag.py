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
    from .config import get_settings
except ImportError:
    from base import BaseChain
    from config import get_settings


SETTINGS = get_settings()

DEFAULT_MODEL = SETTINGS.model_name
DEFAULT_EMBEDDING_MODEL = SETTINGS.rag_embedding_model
DEFAULT_STOP = SETTINGS.model_stop


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
        temperature: float = SETTINGS.model_temperature,
        system_prompt: Optional[str] = None,
        file_path: Optional[str] = None,
        embedding_model: str = DEFAULT_EMBEDDING_MODEL,
        chunk_size: int = SETTINGS.rag_chunk_size,
        chunk_overlap: int = SETTINGS.rag_chunk_overlap,
        top_k: int = SETTINGS.rag_top_k,
        index_dir: Optional[str] = None,
        **kwargs,
    ):
        super().__init__(model, temperature, **kwargs)
        self.system_prompt = (
            system_prompt
            or "You are a helpful AI assistant. Answer in Korean based on the provided context."
        )
        self.file_path = file_path
        self.embedding_model = embedding_model
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.top_k = top_k
        self.index_dir = Path(index_dir) if index_dir else SETTINGS.rag_index_dir

    def _load_vectorstore(self, embeddings: OllamaEmbeddings, index_dir: Path) -> Optional[FAISS]:
        if not index_dir.is_dir():
            return None

        try:
            return FAISS.load_local(
                str(index_dir),
                embeddings=embeddings,
                allow_dangerous_deserialization=True,
            )
        except TypeError:
            return FAISS.load_local(str(index_dir), embeddings=embeddings)
        except Exception:
            return None

    def _build_vectorstore(self, pdf_path: Path, embeddings: OllamaEmbeddings) -> FAISS:
        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=self.chunk_size,
            chunk_overlap=self.chunk_overlap,
        )
        docs = PDFPlumberLoader(str(pdf_path)).load_and_split(text_splitter=text_splitter)
        return FAISS.from_documents(docs, embedding=embeddings)

    def setup(self):
        if not self.file_path:
            raise ValueError("file_path is required")

        pdf_path = Path(self.file_path).expanduser()
        if not pdf_path.is_file():
            raise ValueError(f"File path {pdf_path} is not a valid file")

        embeddings = OllamaEmbeddings(model=self.embedding_model)
        vectorstore = self._load_vectorstore(embeddings, self.index_dir)
        if vectorstore is None:
            vectorstore = self._build_vectorstore(pdf_path, embeddings)
            self.index_dir.mkdir(parents=True, exist_ok=True)
            vectorstore.save_local(str(self.index_dir))

        retriever = vectorstore.as_retriever(search_kwargs={"k": self.top_k})

        prompt_path = Path(__file__).resolve().parent / "prompts" / "rag-exaone.yaml"
        prompt = load_prompt(str(prompt_path), encoding="utf-8")
        llm = ChatOllama(
            model=self.model,
            temperature=self.temperature,
            num_predict=SETTINGS.model_num_predict,
            repeat_penalty=SETTINGS.model_repeat_penalty,
            stop=DEFAULT_STOP,
        )

        return (
            {"context": retriever | format_docs, "question": RunnablePassthrough()}
            | prompt
            | llm
            | StrOutputParser()
        )
