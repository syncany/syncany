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
PWD=`pwd`
cd $REPODIR/build
rm docs.zip 2> /dev/null || true 
rm reports.zip 2> /dev/null || true

if [ -d docs ]; then 
	zip --recurse-paths --quiet docs.zip docs/
	mv docs.zip $REPODIR/build/upload/
fi

if [ -d reports ]; then 
	zip --recurse-paths --quiet reports.zip reports/
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

echo ""
echo "Uploading"
echo "---------"

file_targz=$(ls $REPODIR/build/upload/syncany*.tar.gz)
file_zip=$(ls $REPODIR/build/upload/syncany*.zip)
file_deb=$(ls $REPODIR/build/upload/syncany*.deb)
file_exe=$(ls $REPODIR/build/upload/syncany*.exe)
file_reports=$(ls $REPODIR/build/upload/reports.zip)
file_docs=$(ls $REPODIR/build/upload/docs.zip)

echo "Uploading TAR.GZ: $(basename $file_targz) ..."
upload_app "$file_targz" "tar.gz" "$snapshot"

echo "Uploading ZIP: $(basename $file_zip) ..."
upload_app "$file_zip" "zip" "$snapshot"

echo "Uploading DEB: $(basename $file_deb) ..."
upload_app "$file_deb" "deb" "$snapshot" 

echo "Uploading EXE: $(basename $file_exe) ..."
upload_app "$file_exe" "exe" "$snapshot"

echo "Uploading REPORTS: $(basename $file_reports) ..."
upload_app "$file_reports" "reports" "$snapshot"

echo "Uploading DOCS: $(basename $file_docs) ..."
upload_app "$file_docs" "docs" "$snapshot"

