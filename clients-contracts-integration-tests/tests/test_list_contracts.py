import json
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional
from urllib import request, parse

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
            'contracts.list@example.com',
            '+41795553333',
            'List Contracts Client',
            DATE '1993-09-09',
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


def _insert_contract(
    conn_uri: str,
    client_id: int,
    start_date: date,
    end_date: Any,
    cost_amount: float,
    created_at: datetime,
    updated_at: datetime,
) -> Dict[str, Any]:
    with psycopg.connect(conn_uri) as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO contracts (client_id, start_date, end_date, cost_amount, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s)
                RETURNING id
                """,
                (client_id, start_date, end_date, cost_amount, created_at, updated_at),
            )
            row = cur.fetchone()
        conn.commit()

    if not row:
        raise RuntimeError("Contract insert did not return id.")

    return {
        "id": row[0],
        "start_date": start_date,
        "end_date": end_date,
        "cost_amount": cost_amount,
        "updated_at": updated_at,
    }


def _fetch_contracts(base_url: str, client_id: int, updated_since: Optional[datetime] = None) -> tuple[int, List[Dict[str, Any]]]:
    url = f"{base_url.rstrip('/')}/contracts/clients/{client_id}/contracts"
    if updated_since is not None:
        query = parse.urlencode({"updatedSince": updated_since.isoformat()})
        url = f"{url}?{query}"
    with request.urlopen(url, timeout=5) as response:
        status_code = response.status
        body = response.read().decode("utf-8")

    parsed = json.loads(body) if body else []
    return status_code, parsed


def test_list_contracts_returns_active_sorted_by_start_date():
    env = _load_env()
    conn_uri = _build_connection_uri(env)
    _reset_tables(conn_uri)
    client_id = _seed_client(conn_uri)

    now = datetime.now(timezone.utc)
    today = date.today()
    first = _insert_contract(conn_uri, client_id, today - timedelta(days=180), today + timedelta(days=10), 100.0, now - timedelta(days=10), now - timedelta(days=10))
    second = _insert_contract(conn_uri, client_id, today - timedelta(days=90), None, 200.0, now - timedelta(days=5), now - timedelta(days=5))
    # inactive contract should be excluded
    _insert_contract(conn_uri, client_id, today - timedelta(days=60), today - timedelta(days=1), 300.0, now - timedelta(days=3), now - timedelta(days=3))

    base_url = env.get("API_BASE_URL", "http://localhost:8884")
    status_code, payload = _fetch_contracts(base_url, client_id)

    print(f"GET /contracts/clients/{client_id}/contracts -> {status_code}")
    print(payload)

    assert status_code == 200
    assert len(payload) == 2
    assert [contract["id"] for contract in payload] == [first["id"], second["id"]]
    assert payload[0]["costAmount"] == 100.0
    assert payload[1]["costAmount"] == 200.0


def test_list_contracts_respects_updated_since_filter():
    env = _load_env()
    conn_uri = _build_connection_uri(env)
    _reset_tables(conn_uri)
    client_id = _seed_client(conn_uri)

    now = datetime.now(timezone.utc)
    today = date.today()
    early = _insert_contract(conn_uri, client_id, today - timedelta(days=120), None, 150.0, now - timedelta(days=15), now - timedelta(days=15))
    recent = _insert_contract(conn_uri, client_id, today - timedelta(days=30), None, 225.0, now - timedelta(days=4), now - timedelta(days=2))

    updated_since = now - timedelta(days=5)

    base_url = env.get("API_BASE_URL", "http://localhost:8884")
    status_code, payload = _fetch_contracts(base_url, client_id, updated_since)

    print(f"GET /contracts/clients/{client_id}/contracts?updatedSince={updated_since.isoformat()} -> {status_code}")
    print(payload)

    assert status_code == 200
    assert len(payload) == 1
    assert payload[0]["id"] == recent["id"]
    assert payload[0]["costAmount"] == 225.0


if __name__ == "__main__":
    test_list_contracts_returns_active_sorted_by_start_date()
    test_list_contracts_respects_updated_since_filter()
