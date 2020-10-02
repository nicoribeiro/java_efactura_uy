#!/bin/bash

DATE_START=$1
PORT=9000
BASE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/..
APP_NAME=java_efactura_uy
LOGS=/var/log/srv/$APP_NAME\_cron.log
RUT=210475270010
REPORT_DATE=${DATE_START:=$(date -d "yesterday 13:00" '+%Y%m%d')}
URL=http://localhost:$PORT/api/v1/empresa/$RUT/reporteDiario/$REPORT_DATE?cantReportes=1
AUTH_TOKEN=cd0428ac-e516-417d-adbb-aa9419427d63

#LOG functions
LOG() {
echo "`date`:$@"
}

INFO() {
LOG "INFO: $@"
}

WARNING() {
LOG "WARNING: $@"
}

INFO $URL
curl -v -X POST "$URL" -H "AUTH-TOKEN: $AUTH_TOKEN" --compressed >> $LOGS 2>&1
