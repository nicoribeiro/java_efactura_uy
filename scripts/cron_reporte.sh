#!/bin/bash

DATE_START=$1
RUT_DEFAULT_VALUE=210475270010 # especificar RUT por omisión aqui
RUT=${2:-$RUT_DEFAULT_VALUE}
AUTH_TOKEN=cd0428ac-e516-417d-adbb-aa9419427d63 # especificar auth token
PORT=9000
BASE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"/..
APP_NAME=java_efactura_uy
LOGS=/var/log/srv/$APP_NAME\_cron.log
REPORT_DATE=${DATE_START:=$(date -d "yesterday 13:00" '+%Y%m%d')}
REPORT_URL=http://localhost:$PORT/api/v1/empresa/$RUT/reporteDiario/$REPORT_DATE?cantReportes=1

#LOG functions
LOG() {
  echo "$(date):$@"
}

INFO() {
  LOG "INFO: $@"
}

WARNING() {
  LOG "WARNING: $@"
}

INFO $REPORT_URL
curl -v -X POST "$REPORT_URL" -H "AUTH-TOKEN: $AUTH_TOKEN" --compressed >>$LOGS 2>&1
