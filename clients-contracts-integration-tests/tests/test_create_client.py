import json
from datetime import date
from pathlib import Path
from typing import Any, Dict
from urllib import request

import psycopg
from dotenv import dotenv_values


def _load_env() -> dict:
    env_path = Path(__file__).resolve().parents[1] / ".env"
    env = dotenv_values(env_path)
    if not env:
        raise RuntimeError(f"Unable to load environment variables from {env_path}")
    return env


def _build_connection_uri(env: dict) -> str:
    host = env.get("POSTGRES_HOST", "localhost")
    port = env.get("POSTGRES_PORT")
    db_name = env.get("POSTGRES_DB")
    user = env.get("POSTGRES_USER")
    password = env.get("POSTGRES_PASSWORD")

    missing = [key for key in ("POSTGRES_PORT", "POSTGRES_DB", "POSTGRES_USER", "POSTGRES_PASSWORD") if not env.get(key)]
    if missing:
        raise RuntimeError(f"Missing required database settings: {', '.join(missing)}")

    return f"postgresql://{user}:{password}@{host}:{port}/{db_name}"


def _reset_tables(conn_uri: str) -> None:
    with psycopg.connect(conn_uri) as conn:
        with conn.cursor() as cur:
            cur.execute("TRUNCATE contracts, clients RESTART IDENTITY CASCADE;")
        conn.commit()


def _post_client(base_url: str, payload: Dict[str, Any]) -> tuple[int, Dict[str, Any]]:
    url = f"{base_url.rstrip('/')}/clients/create-client"
    body = json.dumps(payload).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    req = request.Request(url, data=body, headers=headers, method="POST")
    with request.urlopen(req, timeout=5) as response:
        status_code = response.status
        response_body = response.read().decode("utf-8")

    parsed = json.loads(response_body) if response_body else {}
    return status_code, parsed


def _fetch_client(conn_uri: str, client_id: int) -> Dict[str, Any]:
    with psycopg.connect(conn_uri) as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, client_type, email, phone, name, birthdate, company_identifier
                FROM clients
                WHERE id = %s
                """,
                (client_id,),
            )
            row = cur.fetchone()

    if not row:
        raise RuntimeError(f"Client {client_id} not found.")

    return {
        "id": row[0],
        "client_type": row[1],
        "email": row[2],
        "phone": row[3],
        "name": row[4],
        "birthdate": row[5],
        "company_identifier": row[6],
    }


def test_create_client_persists_person_details():
    env = _load_env()
    conn_uri = _build_connection_uri(env)
    _reset_tables(conn_uri)

    base_url = env.get("API_BASE_URL", "http://localhost:8884")
    birthdate = date(1994, 2, 14)
    payload = {
        "phone": "+41795550000",
        "email": "new.person@example.com",
        "name": "New Person",
        "birthdate": birthdate.isoformat(),
    }

    status_code, response_payload = _post_client(base_url, payload)

    print(f"POST /clients/create-client -> {status_code}")
    print(response_payload)

    assert status_code == 201
    assert response_payload["phone"] == payload["phone"]
    assert response_payload["email"] == payload["email"]
    assert response_payload["name"] == payload["name"]
    assert response_payload["clientType"] == "PERSON"
    assert response_payload["birthdate"] == birthdate.isoformat()
    assert response_payload.get("companyIdentifier") is None

    client_id = response_payload["id"]
    db_row = _fetch_client(conn_uri, client_id)
    assert db_row["client_type"] == "PERSON"
    assert db_row["email"] == payload["email"]
    assert db_row["phone"] == payload["phone"]
    assert db_row["name"] == payload["name"]
    assert db_row["birthdate"] == birthdate
    assert db_row["company_identifier"] is None


if __name__ == "__main__":
    test_create_client_persists_person_details()
