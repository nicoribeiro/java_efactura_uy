#!/bin/bash
CMD=$2
INSTANCE_NAME=$1

SCRIPTS_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_NAME=java_efactura_uy
REMOTE_DIR=/srv/$APP_NAME
CONF=$SCRIPTS_HOME/../conf
PORT=9000
JAVA_OPTS="export JAVA_OPTS=-XX:+CMSClassUnloadingEnabled\ -XX:+UseConcMarkSweepGC\ -XX:NewSize=1024m\ -XX:MaxNewSize=1024m\ -XX:SurvivorRatio=6\ -Xmx2g\ -Xms2g"

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

start() {

        cant=$(ps ax | grep -v grep | grep Dconfig.file | grep $APP_NAME | grep -v grep | wc -l)

        if [ "$cant" != "0" ]; then
                INFO "$APP_NAME is running at $INSTANCE_NAME, aborting ..."
        else
                INFO "No $APP_NAME instances running on $INSTANCE_NAME"
                INFO "Starting instance $INSTANCE_NAME..."
				INFO "$JAVA_OPTS"
				APP_NAME_VERSION=$(ls $REMOTE_DIR/$INSTANCE_NAME)
                echo "$JAVA_OPTS; nohup $REMOTE_DIR/$INSTANCE_NAME/$APP_NAME_VERSION/bin/$APP_NAME -java-home /usr/lib/jvm/java-8-openjdk-amd64 -Dhttp.port=$PORT -Dconfig.file=$REMOTE_DIR/$INSTANCE_NAME/$APP_NAME_VERSION/conf/application.conf >> /var/log/srv/$APP_NAME.log 2>&1 &" | bash
				sleep 2

                cant=$(ps -ef | grep Dconfig.file | grep $APP_NAME | grep -v grep | wc -l)

                if [ "$cant" -gt "0" ]; then
                        INFO "$APP_NAME at instance $INSTANCE_NAME started"
                else
                        INFO "$APP_NAME at instance $INSTANCE_NAME could not be started, trying again..."
                        $0 $INSTANCE_NAME start
                fi
        fi
}


stop() {
        INFO "Stopping instance $INSTANCE_NAME..."

        APP_PID=$(ps -ef | grep -v grep | grep Dconfig.file | grep $APP_NAME | awk '{print $2}')
        #curl "http://$INSTANCE_HOSTNAME:9000/api/v1/application/shutdown" -H "X-BH-IPASS-AUTH-TOKEN: 013df144-dd02-430c-affc-33511bab6f8d"  --compressed

        cant=$(ps -ef | grep Dconfig.file | grep $APP_NAME | grep -v grep | wc -l)
        if [ "$cant" -gt "0" ]; then
                #ssh -t -i $instanceKey $instanceUser@$instanceIp -p $instancePort "kill $APP_PID" 2>/dev/null
                kill $APP_PID 2>/dev/null
				sleep 2
                $0 $INSTANCE_NAME stop
        else
                INFO "$APP_NAME at instance $INSTANCE_NAME stopped"
        fi

}


status() {
	cant=$(ps ax | grep -v grep | grep Dconfig.file | grep $APP_NAME | grep -v grep | wc -l)

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
        echo "Usage $0 INSTANCE_ID {start|stop|restart|status}"

        RETVAL=1
esac
