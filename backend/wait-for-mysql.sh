#!/bin/sh
set -e

echo "Waiting for MySQL at ${DB_HOST}:${DB_PORT} ..."

until python - << 'PY'
import os, sys
import pymysql
try:
    conn = pymysql.connect(
        host=os.getenv("DB_HOST","mysql"),
        port=int(os.getenv("DB_PORT","3306")),
        user=os.getenv("DB_USER","root"),
        password=os.getenv("DB_PASSWORD","123456"),
        database=os.getenv("DB_NAME","caimingxuan"),
        connect_timeout=2
    )
    conn.close()
    sys.exit(0)
except Exception:
    sys.exit(1)
PY
do
  sleep 2
done

echo "MySQL is ready. Starting backend..."
exec python app.py
