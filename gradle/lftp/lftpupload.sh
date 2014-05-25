#!/bin/bash

SCRIPTDIR="$( cd "$( dirname "$0" )" && pwd )"
REPODIR="$SCRIPTDIR/../.."

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
rm -rf "$TEMPDIR"
mkdir $TEMPDIR
mkdir $TEMPDISTDIR

# Gather deb/tar-gz/zip
echo ""
echo "Gathering distributables ..."
echo "----------------------------"
cp $REPODIR/syncany-lib/build/resources/main/application.properties $TEMPDIR
cp $REPODIR/build/distributions/*.{zip,tar.gz} $TEMPDISTDIR
cp $REPODIR/build/linux-package/*.deb $TEMPDISTDIR
cp $REPODIR/build/innosetup/*.exe $TEMPDISTDIR

PWD=`pwd`
cd $TEMPDISTDIR
sha256sum * 2>/dev/null 
cd "$PWD"

if [ $(ls $TEMPDISTDIR | wc -l) != "4" ]; then
	echo "ERROR: Wrong files in $TEMPDISTDIR: "
	ls $TEMPDISTDIR
	
	rm -rf "$TEMPDIR"
	exit 2
fi

# Gather JavaDoc & Test Reports
echo ""
echo "Gathering JavaDoc & Test Reports ..."
echo "------------------------------------"

PWD=`pwd`
cd $REPODIR/build
rm javadoc.tar.gz 2> /dev/null
rm reports.tar.gz 2> /dev/null
tar -czf javadoc.tar.gz javadoc/
tar -czf reports.tar.gz reports/
mv javadoc.tar.gz $TEMPDIR/
mv reports.tar.gz $TEMPDIR/
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
