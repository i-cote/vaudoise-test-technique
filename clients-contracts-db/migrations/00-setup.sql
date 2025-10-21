-- clients-contracts-db/00-setup.sql
-- Master bootstrap script intended for docker-entrypoint-initdb.d usage.
-- Executes base schema migrations located under /sql inside the container.

\echo 'Applying base schema...'
\i ./base/01-schema.sql

\echo 'Applying constraints...'
\i ./base/02-constraints.sql

\echo 'Applying triggers...'
\i ./base/03-triggers.sql

\echo 'Applying views...'
\i ./base/04-views.sql

\echo 'Applying performance indexes...'
\i ./base/05-indexes.sql

\echo 'Database bootstrap completed.'

