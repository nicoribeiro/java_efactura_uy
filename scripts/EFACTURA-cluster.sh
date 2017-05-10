#!/bin/sh
#
APP_NAME=EFACTURA

SCRIPTS_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

$SCRIPTS_HOME/APP-cluster.sh $APP_NAME $1 $2 
