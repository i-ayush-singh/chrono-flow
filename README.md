# ChronoFlow

Distributed job scheduler as a service (Spring Boot, Kafka, Redis, PostgreSQL, Kubernetes).

## GitHub remote

This repository is configured to use GitHub as `origin`. Cursor uses your linked GitHub account for authentication when you push from the Source Control UI or terminal.

### One-time setup

1. On GitHub, create a **new empty repository** (no README, no .gitignore) named `chrono-flow` under the account you use with Cursor.
2. If the repo URL is different from the default below, update the remote:

   ```bash
   git remote set-url origin https://github.com/<YOUR_USER_OR_ORG>/chrono-flow.git
   ```

3. Push this branch:

   ```bash
   git push -u origin main
   ```

Default remote (matches local `git config user.name`):

- `https://github.com/ayushsingh/chrono-flow.git`

SSH alternative:

```bash
git remote set-url origin git@github.com:ayushsingh/chrono-flow.git
git push -u origin main
```

## Status

Project scaffolding and services will land in follow-up commits.
