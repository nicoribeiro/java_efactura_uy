#!/bin/sh
SERVER_GROUP=$1

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


BASE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/..
DEPLOY=$BASE/deploys/$SERVER_GROUP
CONF=$BASE/conf
DEST=/srv/java_efactura_uy

instanceIp() {
        echo $(cat $CONF/servers.conf | grep $1 | awk -F "|" {'print $2'})
}

instancePort() {
	echo $(cat $CONF/servers.conf | grep $1 | awk -F "|" {'print $3'})
}

instanceKey() {
        echo $(cat $CONF/servers.conf | grep $1 | awk -F "|" {'print $5'})
}

instanceUser() {
        echo $(cat $CONF/servers.conf | grep $1 | awk -F "|" {'print $6'})
}

pushDeploy() {

        INFO "PUSHING DEPLOY TO SERVER GROUP: $SERVER_GROUP..."
        INFO

	instances=$(cat $CONF/servers.conf | grep $SERVER_GROUP | awk -F "|" {'print $1'})
        INFO $instances
        for instance in $instances; do

                instanceIp=$(instanceIp $instance)
		instancePort=$(instancePort $instance)
		instanceKey=$(instanceKey $instance)
		instanceUser=$(instanceUser $instance)
                INFO "Pushing config to Server: $instance ($instanceIp:$instancePort)"
                rsync -av -e "ssh -p $instancePort -i $instanceKey" --del $DEPLOY $instanceUser@$instanceIp:$DEST

        done

}


pushDeploy
