# Recruitment Platform

Internship microservices project for recruitment: authentication, candidates, and hiring pipeline (jobs / applications / evaluations).

## Stack

- Java 21, Spring Boot
- MySQL (one database per service)
- OpenLDAP (optional login path in auth-service)
- JWT shared across services
- Swagger / OpenAPI per service
- Postman collection for end-to-end flows

## Services

| Service | App port | DB port | Database | What it does |
|---------|----------|---------|----------|--------------|
| `auth-service` | 8081 | 3307 | `auth_db` | Login, register users, JWT, LDAP |
| `candidate-service` | 8082 | 3308 | `candidate_db` | Candidates, tags, CV upload/parse |
| `application-service` | 8083 | 3309 | `application_db` | Jobs, applications, stages, assignments, evaluations |

Swagger:

- http://localhost:8081/swagger-ui.html
- http://localhost:8082/swagger-ui.html
- http://localhost:8083/swagger-ui.html

API version header used by clients (Postman / Swagger examples):

```http
Accept: application/json;version=1.0
```

## Prerequisites

- Java 21
- Docker Desktop (running)
- IntelliJ IDEA (recommended) or Maven wrapper (`mvnw` / `mvnw.cmd`)
- Postman (optional, for the collection under `postman/`)

## Docker (databases + LDAP)

Each service has its own `compose.yaml`. Docker only runs infrastructure — not the Spring apps.

From the repo root:

```bash
cd auth-service && docker compose up -d
cd ../candidate-service && docker compose up -d
cd ../application-service && docker compose up -d
```

On Windows PowerShell you can run the same commands one folder at a time.

What starts:

| Container | Port | Notes |
|-----------|------|--------|
| `recruitment-auth-db` | 3307 | MySQL for auth |
| `recruitment-openldap` | 1389 | LDAP users (kept across DB resets) |
| `recruitment-candidate-db` | 3308 | MySQL for candidates |
| `recruitment-application-db` | 3309 | MySQL for jobs/applications |

DB credentials (local only): user `root` / password `root`.

Check containers:

```bash
docker ps
```

Stop infra (keeps data volumes):

```bash
cd auth-service && docker compose stop
cd ../candidate-service && docker compose stop
cd ../application-service && docker compose stop
```

## Run the applications

Start Docker first, then run all three apps (order: auth → candidate → application is safest).

### IntelliJ

Run these main classes:

1. `com.recruitment.authservice.AuthServiceApplication`
2. `com.recruitment.candidateservice.CandidateServiceApplication`
3. `com.recruitment.applicationservice.ApplicationServiceApplication`

### Maven

```bash
cd auth-service && ./mvnw spring-boot:run
cd candidate-service && ./mvnw spring-boot:run
cd application-service && ./mvnw spring-boot:run
```

Windows: use `mvnw.cmd` instead of `./mvnw`.

Spring Boot may also start the related compose file when an app launches (`spring.docker.compose.lifecycle-management=start-and-stop`). Starting compose manually first is still the most reliable approach.

## Initial data (on first startup)

If the tables are empty, each service inserts starter rows so you can demo without creating everything by hand.

| Area | Seeded data |
|------|-------------|
| Auth | `admin@company.com`, `hr@company.com`, `interviewer@company.com`, `ldap.hr@company.com` — password `password123` |
| Candidates | Alice (`alice@example.com`), Bob (`bob@example.com`) |
| Hiring | Published job **Java Engineer**, Alice’s application at **INTERVIEW**, assigned to the interviewer user |

Useful IDs (stable across restarts as long as you keep the same DB volume):

| Entity | ID |
|--------|----|
| HR user | `10000000-0000-4000-8000-000000000002` |
| Interviewer user | `10000000-0000-4000-8000-000000000003` |
| Alice candidate | `20000000-0000-4000-8000-000000000001` |
| Java Engineer job | `30000000-0000-4000-8000-000000000001` |
| Alice application | `30000000-0000-4000-8000-000000000002` |

LDAP demo user: `ldap.hr@company.com` / `password123` (also exists in OpenLDAP as `ldap.hr`).

## Roles (quick)

| Role | Typical use |
|------|-------------|
| `ADMIN` | Register users (including LDAP-linked accounts) |
| `HR` | Candidates, jobs, applications, assignments, stage changes |
| `INTERVIEWER` | See assigned applications, submit evaluations |

All three apps share the same JWT secret (`jwt.secret` in each `application.properties`). Tokens from auth-service work on the other services.

## Postman

Files:

- `postman/Recruitment-Platform.postman_collection.json` — full API flow
- `postman/Local.postman_environment.json` — local URLs + variables
- `postman/samples/` — sample CV text files for upload requests

Setup:

1. Import the collection and the **Local** environment into Postman.
2. Select environment **Local**.
3. Run **Auth → Login** with `hr@company.com` / `password123`.
4. The login request saves `access_token` automatically; other requests use `Authorization: Bearer {{access_token}}`.
5. Create/list flows also fill `candidate_id`, `job_id`, `application_id`, etc. when those requests succeed.

Suggested happy path:

1. Login as HR  
2. List candidates / create candidate / upload CV  
3. Create or list jobs → publish if needed  
4. Create application → change stage → assign interviewer  
5. Login as `interviewer@company.com` → **My Assignments** → create evaluation  

For LDAP: use **Auth → Login LDAP** with `ldap.hr@company.com` / `password123`.

## Tests

From each service folder:

```bash
./mvnw test
```

Windows: `mvnw.cmd test`.

## Project layout

```text
internship-project/
  auth-service/          # :8081 + compose (MySQL 3307, OpenLDAP 1389)
  candidate-service/     # :8082 + compose (MySQL 3308), CV files under uploads/
  application-service/   # :8083 + compose (MySQL 3309); calls auth + candidate over HTTP
  postman/               # collection, environment, sample CVs
  README.md
```

## Troubleshooting

| Problem | What to check |
|---------|----------------|
| App can’t connect to MySQL | `docker ps` — DB container healthy? Ports 3307/3308/3309 free? |
| LDAP login fails | OpenLDAP container up on 1389; use seeded `ldap.hr@company.com` |
| 401 on candidate/application APIs | Login again; paste Bearer token; confirm `jwt.secret` is identical in all three services |
| 403 | Wrong role for that endpoint (e.g. evaluations need interviewer + assignment) |
| application-service 502 | auth-service and/or candidate-service not running |
| Port already in use | Stop the old Java process or change `server.port` in that service’s `application.properties` |

CV uploads are stored under `candidate-service/uploads/` (gitignored).
