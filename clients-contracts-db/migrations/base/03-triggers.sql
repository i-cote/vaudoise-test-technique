-- clients-contracts-db/04-triggers.sql
-- Trigger function and binding definitions for Clients & Contracts domain.
-- Target database: PostgreSQL 13+

BEGIN;

CREATE OR REPLACE FUNCTION trg_clients_before_update()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.birthdate IS DISTINCT FROM OLD.birthdate THEN
        RAISE EXCEPTION 'birthdate is immutable';
    END IF;

    IF NEW.company_identifier IS DISTINCT FROM OLD.company_identifier THEN
        RAISE EXCEPTION 'company_identifier is immutable';
    END IF;

    IF NEW IS DISTINCT FROM OLD THEN
        NEW.updated_at = NOW();
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW IS DISTINCT FROM OLD THEN
        NEW.updated_at = NOW();
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS clients_before_update ON clients;
CREATE TRIGGER clients_before_update
BEFORE UPDATE ON clients
FOR EACH ROW
EXECUTE FUNCTION trg_clients_before_update();

DROP TRIGGER IF EXISTS contracts_set_updated_at ON contracts;
CREATE TRIGGER contracts_set_updated_at
BEFORE UPDATE ON contracts
FOR EACH ROW
EXECUTE FUNCTION trg_set_updated_at();

COMMIT;
