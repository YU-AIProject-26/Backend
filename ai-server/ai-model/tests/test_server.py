from fastapi.testclient import TestClient

from app import server


class FakeTopicChain:
    async def ainvoke(self, payload):
        return f"mocked:{payload['topic']}"


class FakeMeetingSummaryChain:
    async def ainvoke(self, payload):
        return {
            "summary": "요약",
            "decisions": ["결정 1"],
            "action_items": [
                {"task": "보고서 작성", "owner": "홍길동", "due_date": "2026-05-20"}
            ],
            "risks": [],
            "open_questions": [],
        }


def test_health_endpoint():
    client = TestClient(server.app)
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_ready_endpoint_success(monkeypatch):
    monkeypatch.setattr(server, "is_ollama_ready", lambda: (True, "ok"))
    client = TestClient(server.app)

    response = client.get("/ready")

    assert response.status_code == 200
    assert response.json()["status"] == "ready"


def test_ready_endpoint_failure(monkeypatch):
    monkeypatch.setattr(server, "is_ollama_ready", lambda: (False, "connection refused"))
    client = TestClient(server.app)

    response = client.get("/ready")

    assert response.status_code == 503
    assert response.json()["status"] == "not_ready"


def test_ask_endpoint_success(monkeypatch):
    monkeypatch.setattr(server, "topic_chain", FakeTopicChain())
    client = TestClient(server.app)

    response = client.post("/ask", json={"topic": " hello "})

    assert response.status_code == 200
    assert response.json()["answer"] == "mocked:hello"


def test_ask_endpoint_validation_error():
    client = TestClient(server.app)
    response = client.post("/ask", json={"topic": ""})

    assert response.status_code == 422
    assert response.json()["code"] == "INVALID_REQUEST"


def test_meeting_summary_endpoint_success(monkeypatch):
    monkeypatch.setattr(server, "meeting_summary_chain", FakeMeetingSummaryChain())
    client = TestClient(server.app)

    response = client.post(
        "/meeting/summary",
        json={"transcript": "회의 내용입니다.", "language": "Korean"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["summary"] == "요약"
    assert body["decisions"] == ["결정 1"]


def test_meeting_summary_endpoint_validation_error():
    client = TestClient(server.app)
    response = client.post("/meeting/summary", json={"transcript": ""})

    assert response.status_code == 422
    assert response.json()["code"] == "INVALID_REQUEST"
