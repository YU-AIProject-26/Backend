from fastapi.testclient import TestClient

from app import server


class FakeTopicChain:
    async def ainvoke(self, payload):
        return f"mocked:{payload['topic']}"


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
