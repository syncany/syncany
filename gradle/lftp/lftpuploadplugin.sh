#!/bin/bash

SCRIPTDIR="$( cd "$( dirname "$0" )" && pwd )"
REPODIR="$SCRIPTDIR/../../../"

TEMPDIR="$SCRIPTDIR/temp"
TEMPDISTDIR="$TEMPDIR/dist"

if [ -n "$TRAVIS_PULL_REQUEST" -a "$TRAVIS_PULL_REQUEST" != "false" ]; then
	echo "NOTE: Skipping FTP upload. This job is a PULL REQUEST."
	exit 0
fi

if [ "$SYNCANY_FTP_HOST" == "" -o "$SYNCANY_FTP_USER" == "" -o "$SYNCANY_FTP_PASS" == "" ]; then
	echo "ERROR: SYNCANY_FTP_* environment variables not set."
	exit 1
fi

# Reset tempdir
PLUGINID=$(basename $(ls -d build/resources/main/org/syncany/plugins/*))

rm -rf "$TEMPDIR"
mkdir -p $TEMPDIR
mkdir -p $TEMPDISTDIR

# Gather deb/tar-gz/zip

echo ""
echo "Gathering distributables ..."
echo "----------------------------"
echo "PLUGIN ID is $PLUGINID"

cp $REPODIR/build/upload/*.{jar,deb,exe} $TEMPDISTDIR 2> /dev/null

PWD=`pwd`
cd $TEMPDISTDIR
sha256sum * 2>/dev/null 
cd "$PWD"

find $TEMPDIR

# Copy to FTP 
echo ""
echo "Uploading files to Syncany FTP ..."
echo "------------------------------------"

FTPOK=/tmp/syncany.ftpok
touch $FTPOK

lftp -c "open ftp://$SYNCANY_FTP_HOST
user $SYNCANY_FTP_USER $SYNCANY_FTP_PASS
mirror --reverse --exclude javadoc/ --exclude reports/ --delete --parallel=3 --verbose $TEMPDIR /
put $FTPOK -o /syncany.ftpok
bye
"

# Delete tempdir
rm -rf "$TEMPDIR"
