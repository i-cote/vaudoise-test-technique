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
            'contract.updater@example.com',
            '+41880001122',
            'Contract Updater',
            DATE '1988-03-03',
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


def _seed_contract(conn_uri: str, client_id: int) -> Dict[str, Any]:
    insert_sql = """
        INSERT INTO contracts (client_id, start_date, end_date, cost_amount)
        VALUES (%s, %s, %s, %s)
        RETURNING id, start_date, end_date, cost_amount;
    """
    start = date(2024, 6, 1)
    end = date(2025, 6, 1)
    initial_cost = 315.50

    with psycopg.connect(conn_uri) as conn:
        with conn.cursor() as cur:
            cur.execute(insert_sql, (client_id, start, end, initial_cost))
            row = cur.fetchone()
        conn.commit()

    if not row:
        raise RuntimeError("Contract insert did not return data.")

    return {
        "id": row[0],
        "start_date": row[1],
        "end_date": row[2],
        "cost_amount": float(row[3]),
        "initial_cost": initial_cost,
    }


def _update_contract(base_url: str, payload: Dict[str, Any]) -> tuple[int, Dict[str, Any]]:
    url = f"{base_url.rstrip('/')}/contracts/update-contract"
    body = json.dumps(payload).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    req = request.Request(url, data=body, headers=headers, method="PATCH")
    with request.urlopen(req, timeout=5) as response:
        status_code = response.status
        response_body = response.read().decode("utf-8")

    parsed = json.loads(response_body) if response_body else {}
    return status_code, parsed


def _fetch_contract(conn_uri: str, contract_id: int) -> Dict[str, Any]:
    with psycopg.connect(conn_uri) as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT cost_amount
                FROM contracts
                WHERE id = %s
                """,
                (contract_id,),
            )
            row = cur.fetchone()

    if not row:
        raise RuntimeError(f"Contract {contract_id} not found in database.")

    return {"cost_amount": float(row[0])}


def test_update_contract_adjusts_cost_amount():
    env = _load_env()
    conn_uri = _build_connection_uri(env)
    _reset_tables(conn_uri)
    client_id = _seed_client(conn_uri)
    contract = _seed_contract(conn_uri, client_id)

    base_url = env.get("API_BASE_URL", "http://localhost:8884")
    new_cost = 444.25
    payload = {"contractId": contract["id"], "costAmount": new_cost}

    status_code, response_payload = _update_contract(base_url, payload)

    print(f"PATCH /contracts/update-contract -> {status_code}")
    print(response_payload)

    assert status_code == 200
    assert response_payload["id"] == contract["id"]
    assert response_payload["clientId"] == client_id
    assert response_payload["costAmount"] == new_cost

    db_row = _fetch_contract(conn_uri, contract["id"])
    assert db_row["cost_amount"] == new_cost


if __name__ == "__main__":
    test_update_contract_adjusts_cost_amount()
