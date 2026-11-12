# Bank API

REST API for a banking application built with Java and Spring Boot.

The project implements user authentication, bank accounts, cards, money operations, transaction history, spending reports, administrative operations, concurrency protection, and Docker deployment.

## Features

### Authentication and users

- User registration
- Login with JWT authentication
- BCrypt password hashing
- CLIENT and ADMIN roles
- User account blocking and unblocking
- Administrator bootstrap through environment variables

### Bank accounts

- Create multiple bank accounts
- RUB currency support
- View account information and balance
- ACTIVE, BLOCKED and CLOSED account statuses
- Account blocking and unblocking
- Account closing with business-rule validation

### Cards

- Issue a bank card for an account
- One card per account in V1
- Luhn-valid card number generation
- Card expiration date
- ACTIVE, BLOCKED, EXPIRED and CLOSED statuses
- Administrative card blocking and unblocking

### Money operations

- Deposit
- Withdrawal
- Transfer by bank account number
- No overdraft
- Transaction history
- Spending report

Transfers and withdrawals use pessimistic database locking to protect balances from race conditions.

Locks are acquired in a deterministic order during transfers to reduce the risk of database deadlocks.

### Administration

ADMIN users can:

- View users
- Create CLIENT users
- Block and unblock users
- View bank accounts
- Block, unblock and close accounts
- View cards
- Block and unblock cards
- View transaction history with pagination

Administrators cannot directly modify account balances or transaction records.

## Technology stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- JWT
- PostgreSQL 17
- Flyway
- Bean Validation
- Maven
- Lombok
- Docker
- Docker Compose
- Testcontainers
- JUnit 5
- Mockito
- Swagger / OpenAPI

## Architecture

Gateway routes HTTP requests to Identity and Banking. Identity owns users and signs JWTs. Banking owns accounts, cards, transactions and Outbox. Notification consumes banking events independently. Each stateful service uses its own PostgreSQL database.

The HTTP services retain the layered architecture:

```text
HTTP Request
     |
     v
Controller
     |
     v
Service
     |
     v
Repository
     |
     v
PostgreSQL
```

Security requests pass through the Spring Security filter chain and JWT authentication filter before reaching protected endpoints.

Banking stores the user ID assigned by Identity. Accounts link to cards and transactions inside the Banking database.

```text
BankAccount (userId)
 |
 +---- Card

BankTransaction
 |
 +---- sourceAccount
 |
 +---- destinationAccount
```

## Security

The API uses stateless JWT authentication.

Public endpoints:

```text
/api/auth/**
/swagger-ui/**
/v3/api-docs/**
```

Authenticated users can access client API endpoints.

Endpoints under:

```text
/api/admin/**
```

require the `ADMIN` role.

Passwords are stored using BCrypt hashes and are never stored in plain text.

## Running with Docker

### Requirements

Use Docker Compose and Java 21. Java generates the local development credentials and runs the Maven reactor.

### 1. Clone the repository

```bash
git clone https://github.com/Hronos312/bank-api.git
cd bank-api
git switch develop/microservices
```

### 2. Create environment configuration

Generate the RSA keys, database passwords and internal Identity API credential.

```bash
java tools/GenerateDevSecrets.java
```

Optional administrator bootstrap settings are in `.env.example`. Pass `--env-file .env` before `--env-file secrets/compose.env` when using these settings.

### 3. Build and start the application

```bash
docker compose --env-file secrets/compose.env up -d --build --wait
```

Gateway exposes the public HTTP API at `127.0.0.1:8080`. Identity, Banking and Notification each own a PostgreSQL 17 database. Kafka has a local development listener at `127.0.0.1:9092`.

See the [roadmap](docs/roadmap.md), [service contracts](docs/contracts.md), [migration guide](docs/migration.md) and [operations guide](docs/operations.md).

Check container status:

```bash
docker compose --env-file secrets/compose.env ps
```

### 4. Stop the application

```bash
docker compose --env-file secrets/compose.env down
```

Database data is preserved in the Docker volume.

For a clean test stack, use a separate Compose project with `-p bank-api-smoke`.

## Swagger / OpenAPI

After starting the application, Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
```

Protected endpoints can be called directly from Swagger UI using the JWT `Authorize` button.

## API overview

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new CLIENT |
| POST | `/api/auth/login` | Authenticate and receive JWT |

### User

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users/me` | Get current user profile |

### Accounts

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/accounts` | Create bank account |
| GET | `/api/accounts` | Get current user's accounts |
| GET | `/api/accounts/{accountId}` | Get account |
| POST | `/api/accounts/{accountId}/deposit` | Deposit money |
| POST | `/api/accounts/{accountId}/withdraw` | Withdraw money |
| POST | `/api/accounts/{accountId}/transfer` | Transfer money |
| GET | `/api/accounts/{accountId}/transactions` | Get transaction history |

### Cards

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/accounts/{accountId}/card` | Issue card |
| GET | `/api/cards` | Get user's cards |
| GET | `/api/cards/{cardId}` | Get card |

### Reports

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/reports/spending` | Get spending report |

### Administration

Administrative endpoints are available under:

```text
/api/admin/**
```

They provide management operations for users, accounts, cards and transactions.

Full endpoint documentation is available in Swagger UI.

## Spending report

The spending report treats the following operations as expenses:

- withdrawals;
- transfers to another user's account.

The following operations are not considered expenses:

- deposits;
- transfers between accounts belonging to the same user.

## Database migrations

The database schema is managed with Flyway.

On application startup Flyway automatically validates and applies migrations from:

```text
src/main/resources/db/migration
```

Hibernate is configured to validate the schema instead of creating it automatically.

## Testing

The project contains unit, MVC and integration tests.

Integration tests use PostgreSQL through Testcontainers.

Covered scenarios include:

- registration and authentication;
- JWT authorization;
- accounts and cards;
- deposits and withdrawals;
- transfers;
- transaction history;
- spending reports;
- Admin API;
- user/account/card blocking;
- administrator bootstrap;
- concurrent withdrawals;
- concurrent opposite-direction transfers;
- deadlock prevention;
- complete client flow;
- complete administrator flow.

Run all tests:

```bash
mvn test
```

Docker must be running because integration tests use Testcontainers.

## Concurrency

Money operations use pessimistic database locking.

For example, concurrent withdrawals cannot both read the same old balance and spend the same funds twice.

Transfers lock both participating accounts.

To reduce deadlock probability, accounts are locked in ascending order by their database IDs regardless of transfer direction.

Example:

```text
Transfer A -> B
locks A, then B

Transfer B -> A
also locks A, then B
```

This guarantees a consistent lock acquisition order.

## Project status

Current version: **V1**

V1 includes:

- authentication;
- JWT security;
- users;
- accounts;
- cards;
- money operations;
- transfers;
- concurrency protection;
- transaction history;
- spending reports;
- administrative API;
- OpenAPI documentation;
- Docker deployment;
- integration testing.

Possible future improvements:

- multi-currency accounts;
- transfers by phone number;
- additional card types;
- payment operations;
- transaction filtering and date ranges;
- refresh tokens;
- CI/CD;
- metrics and monitoring.
