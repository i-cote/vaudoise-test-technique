import json
from datetime import date
from pathlib import Path
from typing import Optional
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


def _seed_client_for_delete(conn_uri: str) -> int:
    insert_sql = """
        INSERT INTO clients (client_type, email, phone, name, birthdate, company_identifier)
        VALUES (
            'PERSON',
            'candidate.for.delete@example.com',
            '+41790001122',
            'Client Scheduled For Deletion',
            DATE '1990-12-12',
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


def _seed_open_contract(conn_uri: str, client_id: int) -> int:
    insert_sql = """
        INSERT INTO contracts (client_id, start_date, end_date, cost_amount)
        VALUES (%s, CURRENT_DATE - INTERVAL '45 days', NULL, 155.55)
        RETURNING id;
    """

    with psycopg.connect(conn_uri) as conn:
        with conn.cursor() as cur:
            cur.execute(insert_sql, (client_id,))
            row = cur.fetchone()
        conn.commit()

    if not row:
        raise RuntimeError("Contract insert did not return an id.")

    return int(row[0])


def _client_exists(conn_uri: str, client_id: int) -> bool:
    with psycopg.connect(conn_uri) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT 1 FROM clients WHERE id = %s", (client_id,))
            return cur.fetchone() is not None


def _get_contract_end_date(conn_uri: str, contract_id: int) -> Optional[date]:
    with psycopg.connect(conn_uri) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT end_date FROM contracts WHERE id = %s", (contract_id,))
            row = cur.fetchone()
            return row[0] if row else None


def _delete_client(base_url: str, client_id: int) -> int:
    url = f"{base_url.rstrip('/')}/clients/delete-client/{client_id}"
    req = request.Request(url, method="DELETE")
    with request.urlopen(req, timeout=5) as response:
        status_code = response.status
        payload = response.read().decode("utf-8")
    if payload:
        try:
            print(json.loads(payload))
        except json.JSONDecodeError:
            print(payload)
    return status_code


def test_delete_client_removes_record_and_closes_contracts():
    env = _load_env()
    conn_uri = _build_connection_uri(env)
    _reset_tables(conn_uri)
    client_id = _seed_client_for_delete(conn_uri)
    contract_id = _seed_open_contract(conn_uri, client_id)

    base_url = env.get("API_BASE_URL", "http://localhost:8884")
    status_code = _delete_client(base_url, client_id)

    print(f"DELETE /clients/delete-client/{client_id} -> {status_code}")

    assert status_code == 204
    assert not _client_exists(conn_uri, client_id)
    contract_end_date = _get_contract_end_date(conn_uri, contract_id)
    assert contract_end_date == date.today()


if __name__ == "__main__":
    test_delete_client_removes_record_and_closes_contracts()
