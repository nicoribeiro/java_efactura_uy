#!/bin/bash


SERVER_GROUP=$1
#prod,test


BASE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/..
SCRIPTS=$BASE/scripts
CONFIG=$BASE/conf
DEPLOY_CONF=$BASE/deploy_conf
GITREPO=$BASE
PROYECT_DIR=$GITREPO
BUILDS=$BASE/builds
MINOR=$(cat $CONFIG/minor.conf)
MAJOR=$(cat $CONFIG/major.conf)
BUILD_LOG=$BUILDS/build.log
DEPLOYS=$BASE/deploys
VERSION=$MAJOR.$MINOR
BUILD_NAME=java_efactura_uy-$VERSION

rm -r $DEPLOYS/$SERVER_GROUP/*

/usr/bin/unzip $BUILDS/$BUILD_NAME.zip -d  $DEPLOYS/$SERVER_GROUP

# ENVIRONMENT CONFIG
cp $DEPLOY_CONF/$SERVER_GROUP/application.conf $DEPLOYS/$SERVER_GROUP/$BUILD_NAME/conf/application.conf
cp $DEPLOY_CONF/$SERVER_GROUP/hazelcast.xml $DEPLOYS/$SERVER_GROUP/$BUILD_NAME/conf/hazelcast.xml
cp -r $BASE/resources $DEPLOYS/$SERVER_GROUP/$BUILD_NAME/.

echo "Deploy done: push config and restart the app for the changes to take effect"
