# Transactional Spring Boot PoC

This project demonstrates Spring transactions in three ways:
- Declarative using `@Transactional` with Spring Data JPA
- Programmatic using `PlatformTransactionManager` with `JdbcTemplate`
- Manual JDBC (connection commit/rollback)

It uses an in-memory H2 database and exposes simple REST endpoints to trigger transfers.

## Requirements
- Java 17+
- Maven 3.9+

## Build and test

```bash
mvn clean verify
```

## Run the app

```bash
mvn spring-boot:run
```

The app starts on http://localhost:8080

H2 console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:txdemo`)

## Seed data
On startup, two accounts are created:
- alice: 100.00
- bob: 50.00

## Endpoints

- GET /api/accounts/{owner}/balance
- POST /api/transfer/jpa?from=alice&to=bob&amount=10.00&failMidway=false
- POST /api/transfer/jdbc-txmgr?from=alice&to=bob&amount=10.00&failMidway=false
- POST /api/transfer/jdbc-manual?from=alice&to=bob&amount=10.00&failMidway=false
- POST /api/transfer/jdbc-no-tx?from=alice&to=bob&amount=10.00&failMidway=false

`failMidway=true` simulates an exception between debit and credit to show rollback behavior.

## Quick try (curl)

```bash
# Check initial balances
curl -s localhost:8080/api/accounts/alice/balance | jq
curl -s localhost:8080/api/accounts/bob/balance | jq

# JPA @Transactional transfer and rollback
curl -s -X POST "localhost:8080/api/transfer/jpa?from=alice&to=bob&amount=10.00&failMidway=false" | jq
curl -s -X POST "localhost:8080/api/transfer/jpa?from=alice&to=bob&amount=10.00&failMidway=true" | jq

# JDBC programmatic TX manager
curl -s -X POST "localhost:8080/api/transfer/jdbc-txmgr?from=alice&to=bob&amount=10.00&failMidway=false" | jq
curl -s -X POST "localhost:8080/api/transfer/jdbc-txmgr?from=alice&to=bob&amount=10.00&failMidway=true" | jq

# JDBC manual connection
curl -s -X POST "localhost:8080/api/transfer/jdbc-manual?from=alice&to=bob&amount=10.00&failMidway=false" | jq
curl -s -X POST "localhost:8080/api/transfer/jdbc-manual?from=alice&to=bob&amount=10.00&failMidway=true" | jq

# No transaction (shows partial update on failure)
curl -s -X POST "localhost:8080/api/transfer/jdbc-no-tx?from=alice&to=bob&amount=10.00&failMidway=true" | jq
```

## Notes
- The `TransactionConfig` defines a dedicated `DataSourceTransactionManager` bean for JDBC, qualified as `jdbcTxManager`, to avoid ambiguity with JPA's transaction manager.
- Integration tests cover commit and rollback scenarios for each approach.

