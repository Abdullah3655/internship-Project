# Recruitment Platform UI

React + Vite frontend for the three microservices.

## Run

1. Start auth (`8081`), candidate (`8082`), and application (`8083`) services.
2. From this folder:

```bash
npm install
npm run dev
```

Open http://localhost:5173

## Demo accounts

| Email | Password | Role |
|-------|----------|------|
| hr@company.com | password123 | HR |
| admin@company.com | password123 | Admin |
| interviewer@company.com | password123 | Interviewer |

## What it covers

- Login / refresh / logout
- Candidates, jobs, applications (role-aware)
- Interviewer picker by name (no UUID pasting)
- Pipeline stage buttons limited to allowed transitions
- Admin team registration
- Interviewer “My assignments” + evaluations

Optional env overrides in `.env`:

```
VITE_AUTH_URL=http://localhost:8081
VITE_CANDIDATE_URL=http://localhost:8082
VITE_APPLICATION_URL=http://localhost:8083
```
