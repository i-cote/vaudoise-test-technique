-- clients-contracts-db/01-schema.sql
-- Schema definition for the Clients & Contracts domain.
-- Target database: PostgreSQL 13+

BEGIN;

CREATE TABLE IF NOT EXISTS clients (
    id BIGSERIAL PRIMARY KEY,
    client_type TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT NOT NULL,
    name TEXT NOT NULL,
    birthdate DATE,
    company_identifier TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS contracts (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    end_date DATE,
    cost_amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMIT;
