import sys

from langserve import RemoteRunnable

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")


chain = RemoteRunnable(
    "https://dividable-surcharge-stump.ngrok-free.dev/topic/",
    headers={"ngrok-skip-browser-warning": "true"},
)

print(chain.invoke({"topic": "1+1을 수학적으로 계산해줘."}))
