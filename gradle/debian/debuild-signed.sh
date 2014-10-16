#!/bin/bash

SIGNKEYID="651D12BD"

SCRIPTDIR="$( cd "$( dirname "$0" )" && pwd )"

if [ ! -f "$1" -o ! -d "$2" -o ! -d "$3" ]; then
	echo "Syntax: $0 <encrypted-key-file> <build-dir> <deb-dir>"
	exit 1
fi

ENCRYPTED_KEYFILE="$1"
BUILDDIR="$2"
DEBBUILDDIR="$3"

if [ -n "$TRAVIS_PULL_REQUEST" -a "$TRAVIS_PULL_REQUEST" != "false" ]; then
	echo "NOTE: Skipping GPG stuff. This job is a PULL REQUEST."
	exit 0
fi

if [ "$SYNCANY_GPG_KEY_WRAP" == "" -o "$SYNCANY_GPG_KEY_PRIV" == "" ]; then
	echo "ERROR: SYNCANY_GPG_* environment variables not set."
	exit 1
fi

mkdir -p $BUILDDIR/gpg
chmod 700 $BUILDDIR/gpg

# Write keys to temporary files (yes, this is unsafe, but there is no other way!)
echo $SYNCANY_GPG_KEY_WRAP > $BUILDDIR/gpg/key-wrap
echo $SYNCANY_GPG_KEY_PRIV > $BUILDDIR/gpg/key-priv

# Decrypt signing key
echo -n "Decrypting signing key ... "
cat $ENCRYPTED_KEYFILE | gpg -d --no-tty --no-use-agent --passphrase-file=$BUILDDIR/gpg/key-wrap > $BUILDDIR/gpg/syncany-team.asc

if [ ! -s "$BUILDDIR/gpg/syncany-team.asc" ]; then
	echo "FAILED. EXITING"
	
	rm -rf $BUILDDIR/gpg
	exit 1
else
	rm $BUILDDIR/gpg/key-wrap
	echo "OK"
fi

# Import to keyring
echo -n "Importing signing key to keyring ... "
gpg --homedir=$BUILDDIR/gpg --no-tty --no-use-agent --import $BUILDDIR/gpg/syncany-team.asc

if [ $? -ne 0 ]; then
	echo "FAILED. EXITING"
	
	rm -rf $BUILDDIR/gpg
	exit 1
else
	rm $BUILDDIR/gpg/syncany-team.asc
	echo "OK"
fi

# Running debuild
echo -n "Running 'debuild' ... " 
cd $DEBBUILDDIR
debuild -k$SIGNKEYID -S -p"gpg --homedir=$BUILDDIR/gpg --no-tty --no-use-agent --passphrase-file=$BUILDDIR/gpg/key-priv"

if [ $? -ne 0 ]; then
	echo "FAILED. EXITING"
	
	rm -rf $BUILDDIR/gpg
	exit 1
else
	rm $BUILDDIR/gpg/key-priv
	echo "OK"
fi

rm -rf $BUILDDIR/gpg
