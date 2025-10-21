# Clients & Contracts API
Backend service for managing insurance clients and their contracts. The API lets counselors create, read, update, and deactivate clients, manage contracts, and compute active exposure in line with the business requirements below.

## Specifications At A Glance
- Create person and company clients with name, phone, email, optional birthdate (persons only) or company identifier.
- Read, update (all mutable fields), and delete clients; deleting a client auto-closes their contracts by stamping the current date as the end date.
- Create contracts with optional start/end dates (defaults: start = today, end = `null`), track cost amounts, and keep internal update timestamps.
- Update only the contract cost amount; this refreshes the internal `updated_at`.
- List active contracts for a client, optionally filtering by last update (`updatedSince` ISO-8601 date-time).
- Retrieve the total cost of a client's active contracts via a dedicated performant endpoint.
- All payloads use JSON; dates and times follow ISO 8601; inputs are validated for phone, email, dates, and numeric values.

## Getting Started
### Prerequisites
- Docker & Docker Compose
- `uv` (https://github.com/astral-sh/uv) for managing the Python integration test environment

### Boot The Stack
1. Copy or adjust the provided `.env` (default credentials target a local PostgreSQL instance).
2. From the repository root, start the services:
   ```bash
   docker compose up --build
   ```
3. The API is available once logs show `Started ClientsContractsApiApplication`; base URL is `http://localhost:8080`.
4. Stop everything with `Ctrl+C` and clean up containers if needed:
   ```bash
   docker compose down
   ```

### What Runs Under The Hood
- `db`: PostgreSQL 16 with a named Docker volume (`db-data`) so data survives container restarts.
- `migrator`: Applies SQL migrations on startup. Scripts are idempotent, so reruns after teardown are safe.
- `api`: Spring Boot service exposing the REST endpoints.

## API Reference
Unless stated otherwise, responses use HTTP 200 on success and meaningful 4xx/5xx codes on errors.

### Clients (Write)
- `POST /clients/create-client`  
  Creates a person or company. Birthdate is required for persons and forbidden for companies. Example:
  ```json
  {
    "name": "Alex Doe",
    "email": "alex@example.com",
    "phone": "+41 79 123 45 67",
    "birthdate": "1990-02-10"
  }
  ```
- `PUT /clients/update-client`  
  Updates mutable fields (name, phone, email). Body includes the client `id`.
- `DELETE /clients/delete-client/{id}`  
  Deletes a client and stamps the current date on all their contracts’ `endDate`.

### Clients (Read)
- `GET /clients/{id}`  
  Returns the full client record or `404` if not found.

### Contracts (Write)
- `POST /contracts/create-contract`  
  Creates a contract linked to a client (`clientId`). `startDate` defaults to today, `endDate` can be omitted, and `costAmount` is mandatory.
- `PATCH /contracts/update-contract`  
  Updates the monetary value (`costAmount`) for an existing contract; `updated_at` refreshes automatically.

### Contracts (Read)
- `GET /contracts/clients/{clientId}/contracts?updatedSince=2024-06-01T00:00:00Z`  
  Lists the client’s active contracts (no `endDate` or `endDate` in the future). Optional `updatedSince` filters on the internal update timestamp.
- `GET /contracts/clients/{clientId}/active-cost`  
  Returns `{ "clientId": 7, "totalActiveCost": 1234.50 }` with the sum of active contract cost amounts.

### Postman / Curl Quick Checks
```bash
# Get a seeded client (see fixtures)
curl http://localhost:8080/clients/1

# Create a company client
curl -X POST http://localhost:8080/clients/create-client \
  -H "Content-Type: application/json" \
  -d '{"name":"ACME SA","email":"contact@acme.test","phone":"+41 78 000 00 00","companyIdentifier":"aaa-123"}'
```

## Data Integrity & Persistence Notes
- The PostgreSQL layer enforces most constraints (email/phone formats, immutable birthdate & company identifier, timestamp management) so controllers stay lean.
- Triggers prevent accidental changes to immutable fields and automatically set `updated_at`.
- Named Docker volumes keep the data even if containers are destroyed; running migrations again remains safe because scripts are idempotent.

## Testing & Verification
### Unit Tests (API Module)
```bash
docker compose --profile unit-tests up --build unit-tests
```

Every feature landed with accompanying unit tests, and the suite has been green on every commit since it was introduced.


### Integration Tests (End-to-End)
1. In `clients-contracts-integration-tests`, start the dedicated stack:
   ```bash
   cd clients-contracts-integration-tests
   docker compose up --build
   ```
2. Bootstrap the virtual environment with `uv` (one-time):
   ```bash
   uv install  # creates .venv and installs pyproject dependencies
   ```
3. Activate the environment and add pytest:
   ```bash
   source .venv/bin/activate
   uv pip install pytest
   ```
4. With the Docker stack still running, execute:
   ```bash
   pytest
   ```
5. To target a single test: `pytest tests/test_get_client.py::test_get_client_by_id`.
6. Tear down the stack when done: `docker compose down`.

These tests exercise real HTTP calls against the containerized API and database, providing proof that the system behaves as required end-to-end.

## Architecture (≤1000 chars)
The backend is organized around a layered Spring Boot application. Persistence is handled by Spring Data JPA repositories that map to PostgreSQL tables maintained by SQL migrations. Each domain (clients, contracts) exposes two controllers: read endpoints live in dedicated read controllers to isolate query-specific concerns, while write controllers focus on commands and validation. Entities map cleanly to schema objects and DTOs enforce external contracts. Database triggers enforce immutability and automatically refresh timestamps, keeping the API layer lean. Migrations and constraints encode most of the validation rules. Docker Compose orchestrates API, database, and migrator services so the same topology powers both local development and integration tests.

## 🧱 Additional Work (Post-Delivery Enhancements)

A few days after submitting the initial deliverable on **Thursday, October 16**, I took the initiative to implement some *additional features*.

#### 🔹 API Documentation (Swagger)

The project now includes an integrated *Swagger UI*. It provides the following capabilities:

- 📘 Document all endpoints with example requests and responses
- ⚡ Quickly test the API directly from the browser without additional setup

🔍 To access the swagger UI using the docker compose setup, go to localhost:8080 (root of the api).
  The swagger UI is also available in *the live Kubernetes deployment* (see below).

#### 🔹 API Error Handling Improvements

The application now implements standardized error responses using **ProblemDetail** objects instead of returning only HTTP status codes without a body. Each error response now follows the [RFC7807](https://datatracker.ietf.org/doc/html/rfc7807) specification, providing structured fields such as `type`, `title`, `status`, `detail`, and `instance`. This ensures clearer communication of error context to clients and aligns with modern REST API best practices.

The following extension was *not required* by the assignment but reflects my interest in seeing things run end-to-end and my background in DevOps engineering.

### 🔹 Kubernetes Deployment

The application now ships with a complete set of Kubernetes manifests located under the [`/kubernetes`](./kubernetes) directory:

- `Deployment` and `Service` for the API
- `StatefulSet` and `PersistentVolumeClaim` for PostgreSQL
- `ConfigMap` for schema initialization scripts
- `Job` to run database migrations automatically before API startup
- `Ingress` with HTTPS routing

All manifests are parameterized to run in a dedicated namespace named `vaudoise`.

#### 🔹Live Demo Environment

To make the system tangible, I deployed these manifests to my **personal Kubernetes cluster** and exposed two public entry points under a domain I own (icote.dev).

##### 🌐 API Endpoint in Kubernetes

👉 [https://clients-contracts-api.icote.dev](https://clients-contracts-api.icote.dev) *(click to access swagger)*

> 🚀 **This is the live API instance hosted in my** ***Kubernetes cluster***.
>
> You can target it directly using `curl` commands or explore it interactively via the Swagger UI.



##### 🖥️ Kubernetes Dashboard

In addition to the API, a read-only **Kubernetes Dashboard** is available to visualize all deployed resources related to the demo

[👉 ](https://cluster.icote.dev/#/workloads?namespace=vaudoise)[https://cluster.icote.dev/#/workloads?namespace=vaudoise](https://cluster.icote.dev/#/workloads?namespace=vaudoise)

This dashboard is scoped to the `vaudoise` namespace and allows viewing pods, services, jobs, and other workloads associated with the demo.

> 🔍 *You can freely explore all resources deployed for this demo (pods, services, jobs, etc.) within the ********************************************************************************************************************************************************`vaudoise`******************************************************************************************************************************************************** namespace through the read-only Dashboard view created specifically for this evaluation.*
>
> ⚠️ *If you encounter notification errors while browsing the Dashboard, they are due to permission restrictions: access is intentionally scoped to the ******************************************************************************************************************************************************`vaudoise`****************************************************************************************************************************************************** namespace only and not to the rest of the cluster.*
