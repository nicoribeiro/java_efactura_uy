#!/bin/bash


BASE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/..
SCRIPTS=$BASE/scripts
CONFIG=$BASE/conf
GITREPO=$BASE
PROYECT_DIR=$GITREPO
BUILDS=$BASE/builds
MINOR=$(cat $CONFIG/minor.conf)
MAJOR=$(cat $CONFIG/major.conf)
INCREMENT=0
VERSION=$MAJOR.$(($MINOR + $INCREMENT))
BUILD_LOG=$BUILDS/build.log
BUILD_NAME=java_efactura_uy-$VERSION.zip


if [ ! -d "$BUILDS" ]; then
	# Control will enter here if $BUILDS doesn't exist.
	mkdir $BUILDS
fi

# Increment minor version
echo $(($MINOR + $INCREMENT)) > $CONFIG/minor.conf

# CHANGE VERSION
sed 's/version.*:=.*\".*\"/version\ :=\ \"'$VERSION'\"/' $PROYECT_DIR/build.sbt > $PROYECT_DIR/build.sbt.new
mv $PROYECT_DIR/build.sbt.new $PROYECT_DIR/build.sbt

cd $PROYECT_DIR

sbt dist

mv $PROYECT_DIR/target/universal/$BUILD_NAME $BUILDS/.

echo FILE AT: $BUILDS/$BUILD_NAME

# leave a log
echo "$(date) BUILD version $VERSION" >> $BUILD_LOG

echo "Build $VERSION done ..."

