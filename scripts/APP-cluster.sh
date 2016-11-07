#!/bin/sh
#
APP_NAME=$1
INSTANCE_SCRIPT_NAME=$APP_NAME-instance.sh
SCRIPTS_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CMD=$2
SERVER_GROUP=$3
BASE=$SCRIPTS_HOME/..
CONF=$BASE/conf
instances=$(cat $CONF/servers.conf | grep $SERVER_GROUP | grep -v "#" | awk -F "|" {'print $1'})


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

send_command() {

	INFO $2
	#
	# Sending command to instances with no ORDER
	for instance in $instances; do
		INFO "###################################"
		$SCRIPTS_HOME/$INSTANCE_SCRIPT_NAME $instance $1 $SERVER_GROUP
        done


}


case "$CMD" in
    start)
        send_command "start" "Starting $APP_NAME $SERVER_GROUP instances ..." 
        ;;
    stop)
        send_command "stop" "Stoping $APP_NAME $SERVER_GROUP instances ..." "inv"
        ;;
    restart-full)
        send_command "stop" "Stoping $APP_NAME $SERVER_GROUP instances ..." "inv"
	send_command "start" "Starting $APP_NAME $SERVER_GROUP instances ..."
        ;;
    restart-hot)
	send_command "restart" "Restarting $APP_NAME $SERVER_GROUP instances ..." 
	;;
    status)
	send_command "status" "Status of $APP_NAME $SERVER_GROUP instances ..."
	;;
    *)
        echo "Usage $0 {start|stop|restart-full|restart-hot|status <SERVER GROUP>}"
        RETVAL=1
esac
