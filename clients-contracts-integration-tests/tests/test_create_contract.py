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


def _seed_client(conn_uri: str) -> int:
    insert_sql = """
        INSERT INTO clients (client_type, email, phone, name, birthdate, company_identifier)
        VALUES (
            'PERSON',
            'contract.owner@example.com',
            '+41791234567',
            'Contract Owner',
            DATE '1990-01-01',
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


def _create_contract(base_url: str, payload: Dict[str, Any]) -> tuple[int, Dict[str, Any]]:
    url = f"{base_url.rstrip('/')}/contracts/create-contract"
    body = json.dumps(payload).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    req = request.Request(url, data=body, headers=headers, method="POST")
    with request.urlopen(req, timeout=5) as response:
        status_code = response.status
        response_body = response.read().decode("utf-8")

    parsed = json.loads(response_body) if response_body else {}
    return status_code, parsed


def _fetch_contract_row(conn_uri: str, contract_id: int) -> Dict[str, Any]:
    with psycopg.connect(conn_uri) as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, client_id, start_date, end_date, cost_amount
                FROM contracts
                WHERE id = %s
                """,
                (contract_id,),
            )
            row = cur.fetchone()

    if not row:
        raise RuntimeError(f"Contract {contract_id} not found.")

    return {
        "id": row[0],
        "client_id": row[1],
        "start_date": row[2],
        "end_date": row[3],
        "cost_amount": float(row[4]),
    }


def test_create_contract_persists_row_and_returns_dto():
    env = _load_env()
    conn_uri = _build_connection_uri(env)
    _reset_tables(conn_uri)
    client_id = _seed_client(conn_uri)

    base_url = env.get("API_BASE_URL", "http://localhost:8884")
    start = date(2024, 1, 10)
    end = date(2024, 12, 31)
    payload = {
        "clientId": client_id,
        "startDate": start.isoformat(),
        "endDate": end.isoformat(),
        "costAmount": 249.75,
    }

    status_code, response_payload = _create_contract(base_url, payload)

    print(f"POST /contracts/create-contract -> {status_code}")
    print(response_payload)

    assert status_code == 201
    assert response_payload["clientId"] == client_id
    assert response_payload["startDate"] == start.isoformat()
    assert response_payload["endDate"] == end.isoformat()
    assert response_payload["costAmount"] == 249.75

    contract_id = response_payload["id"]
    db_row = _fetch_contract_row(conn_uri, contract_id)
    assert db_row["client_id"] == client_id
    assert db_row["start_date"] == start
    assert db_row["end_date"] == end
    assert db_row["cost_amount"] == 249.75


if __name__ == "__main__":
    test_create_contract_persists_row_and_returns_dto()
