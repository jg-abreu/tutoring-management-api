#!/bin/sh
# Restores the live demo to the sample data: empties every table and reloads 02-seed.sql.
# Installed at /opt/tutoring-api/reset-demo.sh and run daily by tutoring-demo-reset.timer.
set -eu

SEED=/opt/tutoring-api/seed/02-seed.sql
CONTAINER=portfolio-production-postgres-1

{
    echo 'BEGIN;'
    echo 'TRUNCATE notification, enrollment, session, tutoring_bond, subject, users;'
    cat "$SEED"
    echo 'COMMIT;'
} | docker exec -i "$CONTAINER" psql -q -U tutoring -d tutoring -v ON_ERROR_STOP=1

echo "tutoring demo reset at $(date -u +%Y-%m-%dT%H:%M:%SZ)"
