# Devices API

A Java 21 REST API for creating, retrieving, filtering, fully updating, and
deleting devices. Spring Boot 4, JPA/Hibernate, and file-backed SQLite provide
the HTTP and persistence layers. The full API contract is in
[openapi.yaml](openapi.yaml).

## Build and test

Install a Java 21 JDK. The checked-in Gradle wrapper downloads Gradle 9.7.1;
the first build needs internet access to download Gradle and dependencies.

```sh
./gradlew test bootJar
```

The executable application is written to
`build/libs/devices-api-0.0.1-SNAPSHOT.jar`. Tests cover domain rules, service
behavior, SQLite persistence, HTTP validation, and HTTP-to-database integration.
HTTP tests use port `18080`, which must be free, and isolated temporary SQLite
files rather than the local application database.

## Run locally

```sh
./gradlew bootRun
```

Alternatively, after building:

```sh
java -jar build/libs/devices-api-0.0.1-SNAPSHOT.jar
```

The API listens on `http://localhost:8080`. By default, SQLite stores data in
`devices.db` in the working directory. To choose another file, set
`DEVICES_DATABASE_PATH`; its parent directory must already exist and be writable:

```sh
DEVICES_DATABASE_PATH=/absolute/path/devices.db ./gradlew bootRun
```

## Run in a container

With Docker Engine and Docker Compose installed:

```sh
docker compose up -d --build
docker compose logs -f devices-api
```

The multi-stage image builds the application with Java 21 and runs it using a
Java 21 JRE as a non-root user. Compose publishes port `8080` and mounts the
`devices-data` named volume at `/data`. SQLite writes to `/data/devices.db`.
Only the application sources and Gradle build files enter the build context.

Stop the application with:

```sh
docker compose down
```

This retains the database volume. A subsequent `docker compose up -d` starts
the API with the same data, even though the container is recreated. Adding
`--volumes` to `docker compose down` deletes the stored database.


## Contract and design choices

- States are `available`, `in-use`, and `inactive`. Unknown and numeric enum
  values are rejected.
- POST and PUT require a nonblank `name`, a nonblank `brand`, and a valid
  `state`. Unknown properties and non-null client-supplied `id` or
  `creationTime` are rejected. Names and brands retain their whitespace.
- GET filters are optional, exact, and case-sensitive. Supplied filters combine
  with AND. An empty brand is an exact filter. An invalid or empty state returns
  `400`. Lists return arrays, including `[]` when nothing matches, with no
  ordering guarantee.
- A device that is already in use cannot change name or brand while remaining
  in use. The same PUT may release it and change those fields together. An
  in-use device cannot be deleted.
- Invalid requests return `400`, missing devices return `404`, and domain or
  optimistic concurrency conflicts return `409`. Errors use Problem Details.
- Packages follow controller, service, domain, and persistence layers. Services
  accept domain values rather than HTTP DTOs. The domain entity enforces the
  update rule; the service transaction enforces deletion eligibility.
- SQLite generates IDs and creation timestamps. Transactional updates and
  deletions use an internal optimistic version to reject stale writes; that
  version is excluded from the HTTP contract.
- Hibernate's SQLite community dialect and `ddl-auto=update` initialize and
  update the schema. There is no separate schema script or migration tool.

## Limitations

SQLite is intended for a single application instance with a local persistent
volume. Concurrent write throughput is limited; this setup is not designed for
multiple replicas sharing a database file. Automatic schema updates do not
provide versioned migrations or a production database upgrade strategy.

Filters are strict: brand matching is exact, case-sensitive, and preserves
whitespace, with no partial matching or normalization. State filters accept only
`available`, `in-use`, or `inactive`; other casing, surrounding whitespace, and
empty values return `400`. Combining brand and state requires both to match.
For production use, more flexible brand search, such as case-insensitive,
partial, or fuzzy matching, would be more appropriate.

No audit history is recorded. Updates retain only the current device values,
and deletion permanently removes the device. Previous values and deleted
devices cannot be retrieved or restored through the API.

PATCH, pagination, authentication, CI, and deployment hosting are outside the
current delivery. PUT requires all mutable fields. Jackson's existing string
conversion behavior is retained; stricter numeric or boolean input handling for
string fields is outside the current scope. The OpenAPI specification is a
checked-in file; no interactive documentation UI is bundled.
