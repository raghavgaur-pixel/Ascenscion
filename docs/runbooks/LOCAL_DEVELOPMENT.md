# Local Development

## Requirements

- Java 21
- Maven 3.9+

## Build

```bash
mvn clean package
```

## Output

The shaded plugin jar will be produced under `target/`.

## Notes

- Optional integrations are compile-time optional and runtime optional.
- SQLite is intended for local development only.
- PostgreSQL support will be implemented behind the database subsystem.

