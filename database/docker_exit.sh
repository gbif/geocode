#!/usr/bin/env bash
set -Eeo pipefail

PGUSER="${PGUSER:-postgres}" pg_ctl -D "$PGDATA" -m fast -w stop

exit 1
