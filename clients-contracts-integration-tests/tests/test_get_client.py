import json
import time
from pathlib import Path
from typing import Any, Dict, Optional, Tuple
from urllib import error, request

import psycopg
from dotenv import dotenv_values


def _project_root() -> Path:
    return Path(__file__).resolve().parents[1]


def _load_env() -> dict:
    env_path = _project_root() / ".env"
    env = dotenv_values(env_path)
    if not env:
        raise RuntimeError(f"Unable to load environment variables from {env_path}")
    return env


def _seed_client(conn_uri: str) -> int:
    truncate_sql = "TRUNCATE contracts, clients RESTART IDENTITY CASCADE;"
    insert_sql = """
        INSERT INTO clients (client_type, email, phone, name, birthdate, company_identifier)
        VALUES (
            'PERSON',
            'integration.tester@example.com',
            '+41441234567',
            'Integration Tester',
            DATE '1992-07-09',
            NULL
        )
        RETURNING id;
    """

    with psycopg.connect(conn_uri) as conn:
        with conn.cursor() as cur:
            cur.execute(truncate_sql)
            cur.execute(insert_sql)
            row = cur.fetchone()
        conn.commit()

    if not row:
        raise RuntimeError("Insert statement did not return a client id.")

    return int(row[0])


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


def _get_api_base_url(env: dict) -> str:
    base_url = env.get("API_BASE_URL", "http://localhost:8884")
    return base_url.rstrip("/")


def _fetch_client(base_url: str, client_id: int) -> Tuple[int, Dict[str, Any]]:
    url = f"{base_url}/clients/{client_id}"
    last_error: Optional[Exception] = None

    for _ in range(10):
        try:
            with request.urlopen(url, timeout=5) as response:
                body = response.read().decode("utf-8")
                status_code = response.status
            return status_code, json.loads(body)
        except (error.HTTPError, error.URLError) as exc:
            last_error = exc
            time.sleep(1)

    raise RuntimeError(f"Failed to reach API at {url}") from last_error


def test_get_client_by_id():
    env = _load_env()
    conn_uri = _build_connection_uri(env)
    client_id = _seed_client(conn_uri)

    base_url = _get_api_base_url(env)
    status_code, payload = _fetch_client(base_url, client_id)

    assert status_code == 200
    assert payload["id"] == client_id
    assert payload["name"] == "Integration Tester"
    assert payload["clientType"] == "PERSON"
    assert payload["email"] == "integration.tester@example.com"
    assert payload["phone"] == "+41441234567"


if __name__ == "__main__":
    test_get_client_by_id()
