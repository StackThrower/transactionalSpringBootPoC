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

## Isolation level demos

New endpoints to demonstrate isolation anomalies (non-repeatable and phantom reads) at different isolation levels using programmatic JDBC transactions:

- GET /api/isolation/non-repeatable?owner=alice&delta=5.00&level=READ_COMMITTED
  - Reads balance twice within one transaction while another transaction updates between reads.
  - Response fields: isolation, firstRead, secondRead, anomaly (true if values differ).
- GET /api/isolation/phantom?threshold=50.00&level=READ_COMMITTED
  - Counts rows with balance >= threshold twice; another transaction inserts a new qualifying row between queries.
  - Response fields: isolation, firstCount, secondCount, anomaly (true if counts differ).

Supported levels: READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE.

Notes:
- Expected behavior (typical on many databases):
  - Non-repeatable reads: allowed at READ_UNCOMMITTED and READ_COMMITTED; prevented at REPEATABLE_READ and SERIALIZABLE.
  - Phantom reads: allowed at READ_UNCOMMITTED and READ_COMMITTED; often prevented only at SERIALIZABLE (REPEATABLE_READ behavior is DB-specific).
- H2’s behavior may differ slightly compared to Postgres/MySQL due to its MVCC implementation; use this as an educational demo.

### Quick try (curl)

```bash
# Non-repeatable read demo
curl -s "localhost:8080/api/isolation/non-repeatable?owner=alice&delta=5.00&level=READ_COMMITTED" | jq
curl -s "localhost:8080/api/isolation/non-repeatable?owner=alice&delta=5.00&level=REPEATABLE_READ" | jq
curl -s "localhost:8080/api/isolation/non-repeatable?owner=alice&delta=5.00&level=SERIALIZABLE" | jq

# Phantom read demo
curl -s "localhost:8080/api/isolation/phantom?threshold=50.00&level=READ_COMMITTED" | jq
curl -s "localhost:8080/api/isolation/phantom?threshold=50.00&level=REPEATABLE_READ" | jq
curl -s "localhost:8080/api/isolation/phantom?threshold=50.00&level=SERIALIZABLE" | jq
```

## Notes
- The `TransactionConfig` defines a dedicated `DataSourceTransactionManager` bean for JDBC, qualified as `jdbcTxManager`, to avoid ambiguity with JPA's transaction manager.
- Integration tests cover commit and rollback scenarios for each approach.
