import os
import sys

from langserve import RemoteRunnable


if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")


base_url = os.getenv("AI_SERVER_BASE_URL", "http://127.0.0.1:8000")
sample_topic = os.getenv("AI_SERVER_SAMPLE_TOPIC", "1+1을 수학적으로 설명해줘.")
chain = RemoteRunnable(f"{base_url.rstrip('/')}/topic/")

print(chain.invoke({"topic": sample_topic}))
