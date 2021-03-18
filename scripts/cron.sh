#!/bin/bash

DATE_START=$1
RUT_DEFAULT_VALUE=210475270010 # especificar RUT aqui
RUT=${2:-$RUT_DEFAULT_VALUE}
PORT=9000
BASE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"/..
APP_NAME=java_efactura_uy
LOGS=/var/log/srv/$APP_NAME\_cron.log
REPORT_DATE=${DATE_START:=$(date -d "yesterday 13:00" '+%Y%m%d')}
REPORT_URL=http://localhost:$PORT/api/v1/empresa/$RUT/reporteDiario/$REPORT_DATE?cantReportes=1
RESULTADOS_URL=http://localhost:$PORT/api/v1/empresa/$RUT/resultados/$REPORT_DATE
AUTH_TOKEN=cd0428ac-e516-417d-adbb-aa9419427d63

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

INFO $RESULTADOS_URL
curl -v -X GET "$RESULTADOS_URL" -H "AUTH-TOKEN: $AUTH_TOKEN" --compressed >>$LOGS 2>&1

INFO $REPORT_URL
curl -v -X POST "$REPORT_URL" -H "AUTH-TOKEN: $AUTH_TOKEN" --compressed >>$LOGS 2>&1
