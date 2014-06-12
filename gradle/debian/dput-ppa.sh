#!/bin/bash

SCRIPTDIR="$( cd "$( dirname "$0" )" && pwd )"
REPODIR="$SCRIPTDIR/../.."

if [ -n "$TRAVIS_PULL_REQUEST" -a "$TRAVIS_PULL_REQUEST" != "false" ]; then
	echo "NOTE: Skipping GPG stuff. This job is a PULL REQUEST."
	exit 0
fi

# Choose PPA
IS_RELEASE=$(git log -n 1 --pretty=%d HEAD | grep master)

if [ -n "$IS_RELEASE" ]; then 
	TARGET_PPA="ppa:syncany/release"
else
	TARGET_PPA="ppa:syncany/snapshot"
fi

# Test files
PPA_FILE_COUNT=$(ls $REPODIR/build/debian/*.{dsc,changes,build,tar.gz} | wc -l)

if [ "4" != "$PPA_FILE_COUNT" ]; then
	echo "ERROR: Unexpected files in debian build dir."
	
	ls $REPODIR/build/debian/
	exit 1
fi

# Run dput
cd $REPODIR/build/debian/
dput --config $REPODIR/gradle/debian/dput.cf --force $TARGET_PPA *.changes
