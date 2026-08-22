# Recruitment Platform

Microservices internship project: auth, candidates, and job applications.

## Services

| Service | Port | Swagger |
|---------|------|---------|
| auth-service | 8081 | http://localhost:8081/swagger-ui.html |
| candidate-service | 8082 | http://localhost:8082/swagger-ui.html |
| application-service | 8083 | http://localhost:8083/swagger-ui.html |

## Prerequisites

- Java 21
- Docker Desktop
- IntelliJ (or Maven)

## Run

1. Start databases (and OpenLDAP for auth):

```bash
cd auth-service && docker compose up -d
cd candidate-service && docker compose up -d
cd application-service && docker compose up -d
```

2. Run each Spring Boot main class from IntelliJ:
   - `AuthServiceApplication`
   - `CandidateServiceApplication`
   - `ApplicationServiceApplication`

3. Demo login (seeded on auth startup): `hr@company.com` / `password123`

Also available: `admin@company.com`, `interviewer@company.com`, `ldap.hr@company.com` (same password).

## Postman

Import `postman/Recruitment-Platform.postman_collection.json` and `postman/Local.postman_environment.json`.

Sample CV files for upload tests: `postman/samples/`.

## Tests

From each service directory:

```bash
./mvnw test
```
