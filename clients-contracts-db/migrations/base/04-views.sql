-- clients-contracts-db/05-views.sql
-- View definitions for Clients & Contracts domain.
-- Target database: PostgreSQL 13+

BEGIN;

CREATE OR REPLACE VIEW active_contracts AS
SELECT *
FROM contracts
WHERE end_date IS NULL OR end_date > CURRENT_DATE;

CREATE OR REPLACE VIEW client_active_contract_totals AS
SELECT
    client_id,
    SUM(cost_amount) AS active_contract_total
FROM active_contracts
GROUP BY client_id;

COMMIT;
