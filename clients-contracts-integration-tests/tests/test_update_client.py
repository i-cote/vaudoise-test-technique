import json
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


def _seed_client_for_update(conn_uri: str) -> int:
    insert_sql = """
        INSERT INTO clients (client_type, email, phone, name, birthdate, company_identifier)
        VALUES (
            'PERSON',
            'original.email@example.com',
            '+41791234567',
            'Original Name',
            DATE '1995-05-05',
            NULL
        )
        RETURNING id;
    """

    with psycopg.connect(conn_uri) as conn:
        with conn.cursor() as cur:
            cur.execute(insert_sql)
            row = cur.fetchone()
        conn.commit()

    if not row:
        raise RuntimeError("Client insert did not return an id.")

    return int(row[0])


def _update_client(base_url: str, payload: Dict[str, Any]) -> tuple[int, Dict[str, Any]]:
    url = f"{base_url.rstrip('/')}/clients/update-client"
    body = json.dumps(payload).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    req = request.Request(url, data=body, headers=headers, method="PUT")
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
                SELECT id, email, phone, name
                FROM clients
                WHERE id = %s
                """,
                (client_id,),
            )
            row = cur.fetchone()

    if not row:
        raise RuntimeError(f"Client {client_id} not found in database.")

    return {"id": row[0], "email": row[1], "phone": row[2], "name": row[3]}


def test_update_client_overwrites_mutable_fields():
    env = _load_env()
    conn_uri = _build_connection_uri(env)
    _reset_tables(conn_uri)
    client_id = _seed_client_for_update(conn_uri)

    base_url = env.get("API_BASE_URL", "http://localhost:8884")
    update_payload = {
        "id": client_id,
        "email": "updated.email@example.com",
        "phone": "+41795556677",
        "name": "Updated Client Name",
    }

    status_code, response_payload = _update_client(base_url, update_payload)

    print(f"PUT /clients/update-client -> {status_code}")
    print(response_payload)

    assert status_code == 200
    assert response_payload["id"] == client_id
    assert response_payload["email"] == "updated.email@example.com"
    assert response_payload["phone"] == "+41795556677"
    assert response_payload["name"] == "Updated Client Name"

    client_row = _fetch_client(conn_uri, client_id)
    assert client_row["email"] == "updated.email@example.com"
    assert client_row["phone"] == "+41795556677"
    assert client_row["name"] == "Updated Client Name"


if __name__ == "__main__":
    test_update_client_overwrites_mutable_fields()
