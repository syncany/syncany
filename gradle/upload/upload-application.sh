#!/bin/bash

set -e

SCRIPTDIR="$( cd "$( dirname "$0" )" && pwd )"
REPODIR=$(readlink -f "$SCRIPTDIR/../../")

if [ -n "$TRAVIS_PULL_REQUEST" -a "$TRAVIS_PULL_REQUEST" != "false" ]; then
	echo "NOTE: Skipping upload. This job is a PULL REQUEST."
	exit 0
fi

source "$SCRIPTDIR/upload-functions"

properties_file=$REPODIR/syncany-lib/build/resources/main/application.properties

if [ ! -f "$properties_file" ]; then
	echo "ERROR: Cannot find properties file application details."
	exit 2
fi

mkdir -p $REPODIR/build/upload/

# Gather Docs & Test Reports
echo ""
echo "Gathering Docs & Test Reports ..."
echo "------------------------------------"

PWD=`pwd`
cd $REPODIR/build
rm docs.zip || true 2> /dev/null
rm reports.zip || true 2> /dev/null

if [ -d docs ]; then 
	zip -r docs.zip docs/
	mv docs.zip $REPODIR/build/upload/
fi

if [ -d reports ]; then 
	tar -czf reports.zip reports/
	mv reports.zip $REPODIR/build/upload/
fi

cd "$PWD"

# List files to upload
release=$(get_property $properties_file "applicationRelease")
snapshot=$([ "$release" == "true" ] && echo "false" || echo "true") # Invert 'release'

echo ""
echo "Files to upload"
echo "---------------"
PWD=`pwd`
cd $REPODIR/build/upload/
sha256sum * 2>/dev/null 
cd "$PWD"

if [ $(ls $REPODIR/build/upload/ | wc -l) != "6" ]; then # 4 releases, docs and reports
	echo "ERROR: Wrong files in $REPODIR/build/upload/: "
	ls -l $REPODIR/build/upload/
	
	exit 2
fi

echo ""
echo "Uploading"
echo "---------"

file_targz=$(ls $REPODIR/build/upload/syncany*.tar.gz)
file_zip=$(ls $REPODIR/build/upload/syncany*.zip)
file_deb=$(ls $REPODIR/build/upload/syncany*.deb)
file_exe=$(ls $REPODIR/build/upload/syncany*.exe)
file_reports=$(ls $REPODIR/build/upload/reports.zip)
file_docs=$(ls $REPODIR/build/upload/docs.zip)

[ -f "$file_targz" ] || (echo "ERROR: Not found: $file_targz" && exit 3)
[ -f "$file_zip" ] || (echo "ERROR: Not found: $file_zip" && exit 3)
[ -f "$file_deb" ] || (echo "ERROR: Not found: $file_deb" && exit 3)
[ -f "$file_exe" ] || (echo "ERROR: Not found: $file_exe" && exit 3)
[ -f "$file_reports" ] || (echo "ERROR: Not found: $file_reports" && exit 3)
[ -f "$file_docs" ] || (echo "ERROR: Not found: $file_docs" && exit 3)

echo "Uploading TAR.GZ: $(basename $file_targz) ..."
upload_app "$file_targz" "tar.gz" "$snapshot"

echo "Uploading ZIP: $(basename $file_zip) ..."
upload_app "$file_zip" "zip" "$snapshot"

echo "Uploading DEB: $(basename $file_deb) ..."
upload_app "$file_deb" "deb" "$snapshot" 

echo "Uploading EXE: $(basename $file_exe) ..."
upload_app "$file_exe" "exe" "$snapshot"

echo "Uploading REPORTS: $(basename $file_reports) ..."
upload_file "$file_reports" "app/reports" "snapshot=$snapshot"

echo "Uploading DOCS: $(basename $file_docs) ..."
upload_file "$file_docs" "app/docs" "snapshot=$snapshot"

