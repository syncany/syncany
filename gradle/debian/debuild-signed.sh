#!/bin/bash

SIGNKEYID="651D12BD"

SCRIPTDIR="$( cd "$( dirname "$0" )" && pwd )"
REPODIR="$SCRIPTDIR/../.."

if [ -n "$TRAVIS_PULL_REQUEST" -a "$TRAVIS_PULL_REQUEST" != "false" ]; then
	echo "NOTE: Skipping GPG stuff. This job is a PULL REQUEST."
	exit 0
fi

if [ "$SYNCANY_GPG_KEY_WRAP" == "" -o "$SYNCANY_GPG_KEY_PRIV" == "" ]; then
	echo "ERROR: SYNCANY_GPG_* environment variables not set."
	exit 1
fi

mkdir -p $REPODIR/build/debian/gpg
chmod 700 $REPODIR/build/debian/gpg

# Write keys to temporary files (yes, this is unsafe, but there is no other way!)
echo $SYNCANY_GPG_KEY_WRAP > $REPODIR/build/debian/gpg/key-wrap
echo $SYNCANY_GPG_KEY_PRIV > $REPODIR/build/debian/gpg/key-priv

# Decrypt signing key
echo -n "Decrypting signing key ... "
cat $REPODIR/gradle/debian/gpg/syncany-team.asc.aes256 | gpg -d --no-tty --no-use-agent --passphrase-file=$REPODIR/build/debian/gpg/key-wrap > $REPODIR/build/debian/gpg/syncany-team.asc

if [ ! -s "$REPODIR/build/debian/gpg/syncany-team.asc" ]; then
	echo "FAILED. EXITING"
	
	rm -rf $REPODIR/build/debian/gpg
	exit 1
else
	rm $REPODIR/build/debian/gpg/key-wrap
	echo "OK"
fi

# Import to keyring
echo -n "Importing signing key to keyring ... "
gpg --homedir=$REPODIR/build/debian/gpg --no-tty --no-use-agent --import $REPODIR/build/debian/gpg/syncany-team.asc

if [ $? -ne 0 ]; then
	echo "FAILED. EXITING"
	
	rm -rf $REPODIR/build/debian/gpg
	exit 1
else
	rm $REPODIR/build/debian/gpg/syncany-team.asc
	echo "OK"
fi

# Running debuild
echo -n "Running 'debuild' ... " 
cd $REPODIR/build/debian/syncany
debuild -k$SIGNKEYID -S -p"gpg --homedir=$REPODIR/build/debian/gpg --no-tty --no-use-agent --passphrase-file=$REPODIR/build/debian/gpg/key-priv"

if [ $? -ne 0 ]; then
	echo "FAILED. EXITING"
	
	rm -rf $REPODIR/build/debian/gpg
	exit 1
else
	rm $REPODIR/build/debian/gpg/key-priv
	echo "OK"
fi

rm -rf $REPODIR/build/debian/gpg

