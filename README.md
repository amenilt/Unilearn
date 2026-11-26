# UniLearn

UniLearn is a university learning platform implemented as a microservices monorepo.
The Spring Cloud foundation and MS1 – Courses are implemented. MS2–MS6 remain
planned and have not been started.

## Platform versions

- Java compiler target: 17
- Spring Boot: 3.5.15
- Spring Cloud release train: 2025.0.3
- Maven: 3.9+

The selected Spring Cloud train officially supports Spring Boot 3.5.x. The Maven
parent imports the Spring Cloud BOM so individual services do not pin Spring
Cloud component versions.

Select a Java 17 JDK in each terminal before launching Maven. For the current
development machine, the installed JDK is under `$env:USERPROFILE\.jdks\jbr-17.0.14`;
replace that path if your JDK 17 is installed elsewhere:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jbr-17.0.14"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

## Platform components

| Component | Port | Purpose |
|---|---:|---|
| Config Server | 8888 | Serves externalized YAML from `infra/config/` |
| Eureka Server | 8761 | Service registry and discovery |
| API Gateway | 8080 | Dynamic routes for instances registered in Eureka |
| MS1 – Courses | 8081 | Course catalog, content, and student enrollments |

Eureka and Gateway use Spring Cloud Config when it is available and retain local
defaults for startup resilience. Config Server uses the native filesystem
backend. `infra/config/application.yml` contains shared discovery and management
defaults; service-specific files override them.

## Run locally on Windows

Use three VS Code terminals from the repository root. Start the Config Server
first, then Eureka, then the Gateway. The first launch downloads Maven
dependencies. Run only the selected module for `spring-boot:run`; `-am` is for
reactor build/test commands and would also invoke the run goal on the parent.
Keep Maven's local dependency repository inside this workspace by defining the
following variable in each terminal before running a Maven command:

```powershell
$mavenRepo = Join-Path (Get-Location) '.m2\repository'
```

### Terminal 1 — Config Server

```powershell
$configPath = (Resolve-Path .\infra\config).Path.Replace('\', '/')
$env:CONFIG_REPOSITORY = "file:///$configPath/"
mvn "-Dmaven.repo.local=$mavenRepo" -f backend/pom.xml -pl platform/config-server spring-boot:run
```

### Terminal 2 — Eureka Server

```powershell
mvn "-Dmaven.repo.local=$mavenRepo" -f backend/pom.xml -pl platform/eureka-server spring-boot:run
```

### Terminal 3 — API Gateway

```powershell
mvn "-Dmaven.repo.local=$mavenRepo" -f backend/pom.xml -pl platform/gateway spring-boot:run
```

Port `8080` is the documented Gateway default. If it is occupied, start Gateway
with a temporary override, for example `$env:SERVER_PORT = '18080'` before the
Maven command, and use that port for its health/routes URLs.

Stop each process with `Ctrl+C`. The gateway's discovery locator creates routes
for services as they register; the explicit MS1 API route is available after
MS1 registers in Eureka.

### Start MS1 – Courses

MS1 requires MySQL 8. Copy `.env.example` to `.env`, change both development
passwords, then start the dedicated database after Docker Desktop is running:

```powershell
Copy-Item .env.example .env
# Edit .env and set MS1_DB_PASSWORD and MYSQL_ROOT_PASSWORD.
docker compose up -d mysql-ms1
```

In another terminal, with Config Server and Eureka already running:

```powershell
$mavenRepo = Join-Path (Get-Location) '.m2\repository'
mvn "-Dmaven.repo.local=$mavenRepo" -f backend/pom.xml -pl services/ms1-courses spring-boot:run
```

MS1 obtains its database, Eureka, and Keycloak settings from
`infra/config/ms1-courses.yml`; local environment variables can override them.
Flyway applies the MySQL schema migrations at startup. The database user is
restricted to the MS1 database. Do not expose development credentials outside
the local environment.

## Verify the platform

With the three processes running, open:

- Eureka dashboard: <http://localhost:8761>
- Gateway health: <http://localhost:8080/actuator/health>
- Gateway routes: <http://localhost:8080/actuator/gateway/routes>
- Configuration served for Gateway: <http://localhost:8888/gateway/default>
- MS1 health: <http://localhost:8081/actuator/health>
- MS1 Swagger UI: <http://localhost:8081/swagger-ui.html>
- MS1 OpenAPI: <http://localhost:8081/v3/api-docs>

MS1 exposes authenticated REST endpoints for `/api/courses`, `/api/categories`,
`/api/subjects`, `/api/enrollments/me`, course enrollments, chapters, and lessons.
JWT realm roles are mapped to `STUDENT`, `TEACHER`, and `ADMIN`; teachers may
manage only their own courses, while admins may manage any course.

Run all current module tests from the repository root:

```powershell
$mavenRepo = Join-Path (Get-Location) '.m2\repository'
mvn "-Dmaven.repo.local=$mavenRepo" -B -f backend/pom.xml clean verify
```

Run one platform component and its parent reactor tests:

```powershell
$mavenRepo = Join-Path (Get-Location) '.m2\repository'
mvn "-Dmaven.repo.local=$mavenRepo" -B -f backend/pom.xml -pl platform/eureka-server -am test
mvn "-Dmaven.repo.local=$mavenRepo" -B -f backend/pom.xml -pl platform/config-server -am test
mvn "-Dmaven.repo.local=$mavenRepo" -B -f backend/pom.xml -pl platform/gateway -am test
```

Run MS1 tests, including its H2-backed API integration tests:

```powershell
$mavenRepo = Join-Path (Get-Location) '.m2\repository'
mvn "-Dmaven.repo.local=$mavenRepo" -B -f backend/pom.xml -pl services/ms1-courses -am verify
```

The MS1 integration tests run Flyway migrations and Hibernate schema validation
against H2 in MySQL compatibility mode. A separate Testcontainers test exercises
MySQL 8 and is skipped when Docker is unavailable.

## Latest infrastructure verification

Verified on 2026-11-26 with OpenJDK 17.0.14 and Maven 3.9.9:

- `mvn -Dmaven.repo.local=<workspace>\.m2\repository -B -f backend/pom.xml clean verify` — **BUILD SUCCESS** for the parent and all three platform modules.
- Eureka Server: **1 test passed**; the live health endpoint returned `UP`.
- Config Server: **1 test passed**; the live health endpoint returned `UP`, and `GET /gateway/default` returned the centralized Gateway discovery configuration.
- API Gateway: **1 test passed**; the discovery route locator and service allow-list were loaded, and the live health endpoint returned `UP`.
- Runtime registry check: Config Server and Gateway both appeared as `UP` instances in Eureka.
- At that verification time, MS1 had not yet been implemented, so no business-service routes were exposed. Internal platform services were excluded by the Gateway allow-list.

Port `8080` was already occupied by another local process during verification. That process was left untouched; the Gateway smoke test therefore used port `18080`. Eureka and Config Server were verified on their configured ports (`8761` and `8888`).

Generated Maven output under `target/` and dependencies under `.m2/` are ignored by Git. The local Maven repository is retained inside this workspace; it is a dependency cache and must not be committed.

## Configuration

Set `CONFIG_REPOSITORY` to a filesystem URI pointing to `infra/config/` when
launching Config Server from another working directory. Set
`CONFIG_SERVER_URL` and `EUREKA_SERVER_URL` to override the local service
addresses. Do not commit credentials or environment-specific secrets.

## Latest MS1 verification

Verified on 2026-11-26 with OpenJDK 17.0.14 and Maven 3.9.9:

- `mvn -Dmaven.repo.local=<workspace>\.m2\repository -B -f backend/pom.xml -pl services/ms1-courses -am verify` — **BUILD SUCCESS**, including the repackaged MS1 executable JAR.
- MS1 tests: **9 run, 0 failures, 0 errors, 1 skipped**. The skipped test is the MySQL 8 Testcontainers test because no Docker daemon was available.
- The H2 integration context applied both Flyway migrations and passed Hibernate schema validation.
- Five API integration tests passed, including authorization/content flows and a real HTTP `GET /actuator/health` check returning `200` with `UP`.
- Final selected reactor verification (`platform/config-server`, `platform/gateway`, and `services/ms1-courses` with `-am verify`) completed with **BUILD SUCCESS**; Config Server and Gateway each passed their test, and the MS1 totals above are unchanged.
- A live `GET http://localhost:8888/gateway/default` returned HTTP 200 and contained the `/api/chapters/**` route. The temporary Config Server was stopped after verification.
- A live MySQL 8 startup, Eureka registration, and Gateway-to-MS1 routing have not been verified in this phase because Docker is unavailable and no local MySQL server is listening on port `3306`. The platform's earlier runtime verification remains recorded above.

The chapter/lesson payload is a minimal implementation choice: ordered chapters,
ordered text lessons, and lesson content limited to 10,000 characters. The
source PowerPoint is not present in the workspace, so this data shape should be
checked against its detailed content model before treating it as final.
