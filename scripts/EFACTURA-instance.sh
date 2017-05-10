#!/bin/sh
CMD=$2
INSTANCE_NAME=$1
SERVER_GROUP=$3
SCRIPTS_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_NAME=java_efactura_uy
REMOTE_DIR=/srv/$APP_NAME
CONF=$SCRIPTS_HOME/../conf
PORT=9000

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

instanceIp() {
        echo $(cat $CONF/servers.conf | grep $1 | grep -v "#" | awk -F "|" {'print $2'})
}

instancePort() {
        echo $(cat $CONF/servers.conf | grep $1 | grep -v "#" | awk -F "|" {'print $3'})
}

instanceKey() {
        echo $(cat $CONF/servers.conf | grep $1 | grep -v "#" | awk -F "|" {'print $5'})
}

instanceUser() {
        echo $(cat $CONF/servers.conf | grep $1 | grep -v "#" | awk -F "|" {'print $6'})
}

instanceIp=$(instanceIp $INSTANCE_NAME)
instancePort=$(instancePort $INSTANCE_NAME)
instanceKey=$(instanceKey $INSTANCE_NAME)
instanceUser=$(instanceUser $INSTANCE_NAME)

start() {

        cant=$(ssh -i $instanceKey $instanceUser@$instanceIp -p $instancePort  "ps ax | grep -v grep | grep Dconfig.file | grep $APP_NAME | grep -v grep | wc -l")

        if [ "$cant" != "0" ]; then
                INFO "$APP_NAME is running at $INSTANCE_NAME, aborting ..."
        else
                INFO "No $APP_NAME instances running on $INSTANCE_NAME"
                INFO "Starting instance $INSTANCE_NAME..."
                APP_NAME_VERSION=$(ssh -i $instanceKey $instanceUser@$instanceIp -p $instancePort "ls $REMOTE_DIR/$SERVER_GROUP")
                ssh -i $instanceKey $instanceUser@$instanceIp -p $instancePort "echo 'nohup $REMOTE_DIR/$SERVER_GROUP/$APP_NAME_VERSION/bin/$APP_NAME -Dhttp.port=$PORT -Dconfig.file=$REMOTE_DIR/$SERVER_GROUP/$APP_NAME_VERSION/conf/application.conf >> /var/log/srv/$APP_NAME.log 2>&1 &' | bash"
                sleep 2

                cant=$(ssh -i $instanceKey $instanceUser@$instanceIp -p $instancePort "ps -ef | grep Dconfig.file | grep $APP_NAME | grep -v grep | wc -l")

                if [ "$cant" -gt "0" ]; then
                        INFO "$APP_NAME at instance $INSTANCE_NAME started"
                else
                        INFO "$APP_NAME at instance $INSTANCE_NAME could not be started, trying againg..."
                        $0 $INSTANCE_NAME start $SERVER_GROUP
                fi
        fi
}


stop() {
        INFO "Stoping instance $INSTANCE_NAME..."

        APP_PID=$(ssh -i $instanceKey $instanceUser@$instanceIp -p $instancePort "ps -ef | grep -v grep | grep Dconfig.file | grep $APP_NAME" | awk '{print $2}')
        #curl "http://$INSTANCE_HOSTNAME:9000/api/v1/application/shutdown" -H "X-BH-IPASS-AUTH-TOKEN: 013df144-dd02-430c-affc-33511bab6f8d"  --compressed

        cant=$(ssh -i $instanceKey $instanceUser@$instanceIp -p $instancePort "ps -ef | grep Dconfig.file | grep $APP_NAME | grep -v grep | wc -l")
        if [ "$cant" -gt "0" ]; then
                ssh -t -i $instanceKey $instanceUser@$instanceIp -p $instancePort "kill $APP_PID" 2>/dev/null
                sleep 2
                $0 $INSTANCE_NAME stop $SERVER_GROUP
        else
                INFO "$APP_NAME at instance $INSTANCE_NAME stopped"
        fi

}


status() {
	cant=$(ssh -i $instanceKey $instanceUser@$instanceIp -p $instancePort  "ps ax | grep -v grep | grep Dconfig.file | grep $APP_NAME | grep -v grep | wc -l")

        if [ "$cant" != "0" ]; then
		INFO "$APP_NAME running instances at $INSTANCE_NAME:  $cant "
        else
		INFO "No $APP_NAME instances running on $INSTANCE_NAME"
        fi
 
}


case "$CMD" in
    start)
        start "start"
        ;;
    stop)
        stop "stop"
        ;;
    restart)
        stop "stop"
	start "start"
	;;
    status)
	status "status"
	;;
    *)
        echo "Usage $0 INSTANCE_ID {start|stop|restart|status} <serverGroup>"

        RETVAL=1
esac
