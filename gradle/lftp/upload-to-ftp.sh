#!/bin/bash

SCRIPTDIR="$( cd "$( dirname "$0" )" && pwd )"
REPODIR="$SCRIPTDIR/../.."

TEMPDIR="$SCRIPTDIR/temp"
TEMPDISTDIR="$TEMPDIR/dist"
TEMPJAVADOCDIR="$TEMPDIR/javadoc"

if [ "$SYNCANY_FTP_HOST" == "" -o "$SYNCANY_FTP_USER" == "" -o "$SYNCANY_FTP_PASS" == "" ]; then
	echo "ERROR: SYNCANY_FTP_* environment variables not set."
	exit 1
fi

# Reset tempdir
rm -rf "$TEMPDIR"
mkdir $TEMPDIR
mkdir $TEMPDIR/dist
mkdir $TEMPDIR/javadoc

# Gather deb/tar-gz/zip
cp $REPODIR/syncany-cli/build/distributions/*.{zip,tar.gz} $TEMPDISTDIR
cp $REPODIR/syncany-cli/build/linux-package/*.deb $TEMPDISTDIR

if [ $(ls $TEMPDISTDIR | wc -l) != "3" ]; then
	echo "ERROR: Wrong files in $TEMPDISTDIR: "
	ls $TEMPDISTDIR
	
	exit 2
fi

# Gather JavaDoc
cp -a $REPODIR/build/javadoc/* $TEMPJAVADOCDIR/

# Copy to FTP 
echo "Uploading files to Syncany FTP ..."

lftp -c "open ftp://$SYNCANY_FTP_HOST
user $SYNCANY_FTP_USER $SYNCANY_FTP_PASS
mirror --reverse --only-newer --delete --parallel=2 --verbose $TEMPDIR /
bye
"

# Delete tempdir
rm -rf "$TEMPDIR"
