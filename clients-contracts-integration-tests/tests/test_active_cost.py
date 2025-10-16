import json
from datetime import date, timedelta
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


def _seed_client(conn_uri: str) -> int:
    insert_sql = """
        INSERT INTO clients (client_type, email, phone, name, birthdate, company_identifier)
        VALUES (
            'PERSON',
            'active.cost@example.com',
            '+41795551212',
            'Active Cost Client',
            DATE '1991-07-07',
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


def _insert_contract(conn_uri: str, client_id: int, start: date, end: Any, cost: float) -> None:
    with psycopg.connect(conn_uri) as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO contracts (client_id, start_date, end_date, cost_amount)
                VALUES (%s, %s, %s, %s)
                """,
                (client_id, start, end, cost),
            )
        conn.commit()


def _get_active_cost(base_url: str, client_id: int) -> tuple[int, Dict[str, Any]]:
    url = f"{base_url.rstrip('/')}/contracts/clients/{client_id}/active-cost"
    with request.urlopen(url, timeout=5) as response:
        status_code = response.status
        body = response.read().decode("utf-8")

    parsed = json.loads(body) if body else {}
    return status_code, parsed


def test_active_cost_sums_only_active_contracts():
    env = _load_env()
    conn_uri = _build_connection_uri(env)
    _reset_tables(conn_uri)
    client_id = _seed_client(conn_uri)

    today = date.today()
    _insert_contract(conn_uri, client_id, today - timedelta(days=30), None, 120.75)
    _insert_contract(conn_uri, client_id, today - timedelta(days=60), today + timedelta(days=15), 89.25)
    _insert_contract(conn_uri, client_id, today - timedelta(days=120), today - timedelta(days=1), 300.00)

    base_url = env.get("API_BASE_URL", "http://localhost:8884")
    status_code, payload = _get_active_cost(base_url, client_id)

    print(f"GET /contracts/clients/{client_id}/active-cost -> {status_code}")
    print(payload)

    expected_total = 120.75 + 89.25  # inactive contract should be ignored

    assert status_code == 200
    assert payload["clientId"] == client_id
    assert abs(float(payload["activeCostAmount"]) - expected_total) < 1e-6


if __name__ == "__main__":
    test_active_cost_sums_only_active_contracts()
