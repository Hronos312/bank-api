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

The application follows a layered architecture:

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

Main domain entities:

```text
User
 |
 +---- BankAccount
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

Only Docker and Docker Compose are required to run the complete application.

### 1. Clone the repository

```bash
git clone https://github.com/Hronos312/bank-api.git
cd bank-api
```

### 2. Create environment configuration

Create `.env` based on `.env.example`.

Example:

```dotenv
JWT_SECRET=your-base64-jwt-secret
JWT_EXPIRATION=3600000

ADMIN_EMAIL=admin@bank.local
ADMIN_PASSWORD=change-me
ADMIN_PHONE=+70000000001
ADMIN_FIRST_NAME=System
ADMIN_LAST_NAME=Administrator
ADMIN_BIRTH_DATE=2000-01-01
```

The `.env` file contains secrets and must not be committed to Git.

### 3. Build and start the application

```bash
docker compose up -d --build
```

Docker Compose starts:

```text
bank-api-app
    |
    +---- Spring Boot application
    |
    +---- port 8080

bank-api-db
    |
    +---- PostgreSQL 17
    |
    +---- persistent Docker volume
```

Check container status:

```bash
docker compose ps
```

### 4. Stop the application

```bash
docker compose down
```

Database data is preserved in the Docker volume.

To completely remove the local database:

```bash
docker compose down -v
```

> Warning: this command permanently deletes the local PostgreSQL volume.

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