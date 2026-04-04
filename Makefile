# Procurement AI — Developer Shortcuts
# Usage: make <target>

.PHONY: help up down db-only build test clean logs shell-db

help:
	@echo ""
	@echo "  make up          Start everything (app + postgres + redis)"
	@echo "  make up-local    Start with LocalStack (no real AWS needed)"
	@echo "  make down        Stop all containers"
	@echo "  make db-only     Start just postgres (for running app locally)"
	@echo "  make build       Build the Spring Boot jar"
	@echo "  make test        Run tests (Testcontainers spins up its own postgres)"
	@echo "  make logs        Tail app logs"
	@echo "  make shell-db    Open psql shell in the postgres container"
	@echo "  make clean       Stop containers and remove volumes"
	@echo ""

# ── Docker Compose ─────────────────────────────────────────────

up:
	docker compose up -d
	@echo "App running at http://localhost:8080"
	@echo "Postgres on localhost:5432"

up-local:
	docker compose --profile local up -d
	@echo "App running at http://localhost:8080"
	@echo "LocalStack at http://localhost:4566"

down:
	docker compose down

clean:
	docker compose down -v
	@echo "Containers and volumes removed"

db-only:
	docker compose up -d postgres redis
	@echo "Postgres ready on localhost:5432"
	@echo "Connect: psql -h localhost -U procuser -d procurement_ai"

# ── Build & Test ───────────────────────────────────────────────

build:
	./mvnw clean package -DskipTests

test:
	./mvnw test

test-verbose:
	./mvnw test -pl . -Dspring.profiles.active=test

# ── Useful shortcuts ───────────────────────────────────────────

logs:
	docker compose logs -f app

shell-db:
	docker compose exec postgres psql -U procuser -d procurement_ai

# Show all tables
db-tables:
	docker compose exec postgres psql -U procuser -d procurement_ai \
		-c "\dt public.*"

# Show Flyway migration history
db-migrations:
	docker compose exec postgres psql -U procuser -d procurement_ai \
		-c "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"

# Show demo data
db-demo:
	docker compose exec postgres psql -U procuser -d procurement_ai \
		-c "SELECT name, subscription, monthly_doc_limit FROM companies;" \
		-c "SELECT email, role FROM users;" \
		-c "SELECT name, city, is_preferred FROM suppliers;"
