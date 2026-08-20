# Players API — test framework

REST API tests for the Players API, written with **Java 21 + Feign + RestAssured + TestNG**.

## Running

Requires **JDK 21 or newer** and **Maven 3.9+** on the path.

The environment lives in `src/test/resources/config.properties` and every key can be overridden without
touching the file — system property first, then environment variable, then the file:

```bash
mvn test -Dapi.base.url=https://testslotegrator.com -Dauth.email=<email> -Dauth.password=<password>
# or
export API_BASE_URL=https://testslotegrator.com AUTH_EMAIL=<email> AUTH_PASSWORD=<password>
mvn test
```

## Configuration

The environment variable name is the key upper-cased with dots replaced by underscores
(`api.base.url` → `API_BASE_URL`).

| Key | Default | Meaning |
| --- | --- | --- |
| `api.base.url` | *(required)* | Scheme + host (+ port), no trailing slash, no `/api` suffix |
| `auth.email` / `auth.password` | *(required)* | Tester credentials for `POST /api/tester/login` |
| `auth.basic.username` / `auth.basic.password` | *(empty)* | Optional `Authorization: Basic` header for login. The contract declares BasicAuth on this endpoint, but the service accepts the call without it |
| `api.transport` | `feign` | HTTP stack: `feign` or `restassured` |
| `api.log.level` | `BASIC` | `NONE`, `BASIC`, `HEADERS`, `FULL` |
| `api.connect.timeout.ms` / `api.read.timeout.ms` | `10000` / `30000` | Timeouts |
| `players.count` | `12` | Players created in step 2 |
| `player.currency.code` | `EUR` | `currency_code` of generated players |
| `player.password` | `Passw0rd!` | `password_change` / `password_repeat` |
| `cleanup.expect.empty` | `false` | `true` asserts `getAll` is globally empty, `false` on this environment because the collection is shared between testers (BUG-010) — it then asserts that this run left nothing behind |


## Defects found

The service contains defects described in table below.

| ID                                                               | Severity | Title |
|------------------------------------------------------------------| --- | --- |
| BUG-001                                                          | **Critical** | `create` and `deleteOne` return the player's password in plain text |
| BUG-002                                                          | Major | `POST /login` returns 201 and a body that is not `TokenDTO` |
| BUG-003                                                          | Major | Player `id` is a 24-character hex string, documented as `integer` |
| BUG-004                                                          | Major | `create` and `deleteOne` return the raw stored document (`_id`, `__v`) |
| BUG-005        | Major | Deleting a non-existent player returns `200 OK` with an empty body |
| BUG-006        | Major | `POST /login` declares BasicAuth but does not require it |
| BUG-010        | Major | `getAll` returns players registered by other tester accounts |
| BUG-011 | Major | `create` accepts a duplicate email and username |
| BUG-007  | Minor | `getAll` is documented as returning a single object, not an array |
| BUG-008| Trivial | `PlayerResponseDTO` requires `currency_code` but does not declare it |
| BUG-009   | Trivial | `getOne` answers `201 Created` for a read |

The main suite is written against the **actual** behaviour, so it stays a usable regression signal. The
defects are asserted separately by `ContractConformanceTest`, which checks the **documented** contract and
therefore fails by design — in the TestNG group `contract`, excluded from the default run:

```bash
mvn test -Pcontract      # 6 failures, one per finding, each printing the offending payload
```
