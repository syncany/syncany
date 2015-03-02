#!/bin/bash
set -e

PROJECTPATH=$(readlink -f "$(cd $(dirname $0) ; pwd -P)/../..")
SKELPATH=$PROJECTPATH/syncany-cli/build/resources/main/org/syncany/cli/cmd
MAKEMANPATH=$PROJECTPATH/gradle/man/makeman.pl

[ -d $SKELPATH ] || (echo -e "ERROR: Cannot find man page input path. Run './gradlew install' first.\nExpected at: $SKELPATH." && exit 1)
[ -f $MAKEMANPATH ] || (echo -e "ERROR: Cannot find 'makeman.pl' script.\nExpected at: $MAKEMANPATH." && exit 2)

mkdir -p $PROJECTPATH/build/debian/syncany/debian/man

echo "Writing man page for command 'sy' to build/debian/syncany/debian/man/sy.1 ..."
$MAKEMANPATH $SKELPATH/help.skel $PROJECTPATH/build/debian/syncany/debian/man/sy.1

for SKELFILE in $(ls $SKELPATH/help.*.skel); do
	SUBCMD_NAME=sy-$(echo $SKELFILE | sed -r 's/^.+help.([^.]+).skel$/\1/')

	echo "Writing man page for command '$SUBCMD_NAME' to build/debian/syncany/debian/man/$SUBCMD_NAME.1 ..."
	$MAKEMANPATH $SKELFILE $PROJECTPATH/build/debian/syncany/debian/man/$SUBCMD_NAME.1	
done

echo "Writing man page index to build/debian/syncany/debian/syncany.manpages ..."
cd $PROJECTPATH/build/debian/syncany
ls debian/man/*.1 > $PROJECTPATH/build/debian/syncany/debian/syncany.manpages

