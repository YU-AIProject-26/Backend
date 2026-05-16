import os
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path


def _to_int(value: str, default: int) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _to_float(value: str, default: float) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def _to_list(value: str, default: list[str]) -> list[str]:
    if not value:
        return default
    return [item.strip() for item in value.split(",") if item.strip()]


@dataclass(frozen=True)
class Settings:
    app_name: str
    host: str
    port: int
    log_level: str
    cors_origins: list[str]
    model_name: str
    model_temperature: float
    model_num_predict: int
    model_repeat_penalty: float
    model_stop: list[str]
    ollama_base_url: str
    rag_file_path: str
    rag_embedding_model: str
    rag_chunk_size: int
    rag_chunk_overlap: int
    rag_top_k: int
    rag_index_dir: Path


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings(
        app_name=os.getenv("APP_NAME", "AI Model Server"),
        host=os.getenv("HOST", "0.0.0.0"),
        port=_to_int(os.getenv("PORT", "8000"), 8000),
        log_level=os.getenv("LOG_LEVEL", "INFO").upper(),
        cors_origins=_to_list(os.getenv("CORS_ORIGINS", "*"), ["*"]),
        model_name=os.getenv("MODEL_NAME", "eeve_q40:latest"),
        model_temperature=_to_float(os.getenv("MODEL_TEMPERATURE", "0"), 0.0),
        model_num_predict=_to_int(os.getenv("MODEL_NUM_PREDICT", "512"), 512),
        model_repeat_penalty=_to_float(os.getenv("MODEL_REPEAT_PENALTY", "1.1"), 1.1),
        model_stop=_to_list(
            os.getenv("MODEL_STOP", "<s>,</s>,<|im_end|>,Human:"),
            ["<s>", "</s>", "<|im_end|>", "Human:"],
        ),
        ollama_base_url=os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434"),
        rag_file_path=os.getenv("RAG_FILE_PATH", ""),
        rag_embedding_model=os.getenv("RAG_EMBEDDING_MODEL", "bge-m3"),
        rag_chunk_size=_to_int(os.getenv("RAG_CHUNK_SIZE", "500"), 500),
        rag_chunk_overlap=_to_int(os.getenv("RAG_CHUNK_OVERLAP", "50"), 50),
        rag_top_k=_to_int(os.getenv("RAG_TOP_K", "4"), 4),
        rag_index_dir=Path(os.getenv("RAG_INDEX_DIR", ".cache/faiss-index")),
    )
