-- clients-contracts-db/02-indexes.sql
-- Supplemental index definitions for Clients & Contracts domain.
-- Target database: PostgreSQL 13+

BEGIN;

CREATE INDEX IF NOT EXISTS idx_clients_phone ON clients (phone);

CREATE INDEX IF NOT EXISTS idx_contracts_client ON contracts (client_id);

CREATE INDEX IF NOT EXISTS idx_contracts_client_updated_at
    ON contracts (client_id, updated_at);

CREATE INDEX IF NOT EXISTS idx_contracts_client_end_date
    ON contracts (client_id, end_date);

COMMIT;
