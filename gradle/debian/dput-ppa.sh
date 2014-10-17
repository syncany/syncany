#!/bin/bash

if [ -n "$TRAVIS_PULL_REQUEST" -a "$TRAVIS_PULL_REQUEST" != "false" ]; then
	echo "NOTE: Skipping GPG stuff. This job is a PULL REQUEST."
	exit 0
fi

if [ ! -f "$1" -o ! -d "$2" -o -z "$3" ]; then
	echo "Syntax: $0 <dput-config> <deb-dir> <ppa>"
	echo "   e.g. $0 gradle/debian/dput.cf build/debian ppa:syncany/snapshot"
	exit 1
fi

DPUTCONFFILE="$1"
DEBDIR="$2"
TARGETPPA="$3"

# Test files
PPA_FILE_COUNT=$(ls $DEBDIR/*.{dsc,changes,build,tar.gz} | wc -l)

if [ "4" != "$PPA_FILE_COUNT" ]; then
	echo "ERROR: Unexpected files in debian build dir."
	
	ls $DEBDIR/
	exit 1
fi

# Run dput
echo "Uploading to $TARGETPPA ..."

cd $DEBDIR/
dput --config $DPUTCONFFILE --force $TARGETPPA *.changes
