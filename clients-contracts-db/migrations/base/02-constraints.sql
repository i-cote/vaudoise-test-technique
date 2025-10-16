-- clients-contracts-db/03-constraints.sql
-- Constraint definitions for Clients & Contracts domain.
-- Target database: PostgreSQL 13+

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'clients_type_check') THEN
        ALTER TABLE clients
            ADD CONSTRAINT clients_type_check CHECK (client_type IN ('PERSON', 'COMPANY'));
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'clients_person_requires_birthdate') THEN
        ALTER TABLE clients
            ADD CONSTRAINT clients_person_requires_birthdate CHECK (
                (client_type = 'PERSON' AND birthdate IS NOT NULL AND company_identifier IS NULL)
                OR (client_type = 'COMPANY' AND birthdate IS NULL)
            );
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'clients_company_requires_identifier') THEN
        ALTER TABLE clients
            ADD CONSTRAINT clients_company_requires_identifier CHECK (
                (client_type = 'COMPANY' AND company_identifier IS NOT NULL)
                OR (client_type = 'PERSON' AND company_identifier IS NULL)
            );
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'clients_email_format') THEN
        ALTER TABLE clients
            ADD CONSTRAINT clients_email_format CHECK (
                email ~ '^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$'
            );
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'clients_phone_format') THEN
        ALTER TABLE clients
            ADD CONSTRAINT clients_phone_format CHECK (
                phone ~ '^[+0-9(). \\-]{7,20}$'
            );
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'contracts_cost_amount_positive') THEN
        ALTER TABLE contracts
            ADD CONSTRAINT contracts_cost_amount_positive CHECK (cost_amount >= 0);
    END IF;
END
$$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'contracts_end_date_after_start') THEN
        ALTER TABLE contracts
            DROP CONSTRAINT contracts_end_date_after_start;
    END IF;

    ALTER TABLE contracts
        ADD CONSTRAINT contracts_end_date_after_start CHECK (
            end_date IS NULL OR end_date > start_date
        );
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_clients_email ON clients ((LOWER(email)));

CREATE UNIQUE INDEX IF NOT EXISTS ux_clients_company_identifier
    ON clients (company_identifier)
    WHERE client_type = 'COMPANY';

COMMIT;
