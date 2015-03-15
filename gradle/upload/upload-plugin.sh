#!/bin/bash

set -e

SCRIPTDIR="$( cd "$( dirname "$0" )" && pwd )"
REPODIR=$(readlink -f "$SCRIPTDIR/../../../")

if [ -n "$TRAVIS_PULL_REQUEST" -a "$TRAVIS_PULL_REQUEST" != "false" ]; then
	echo "NOTE: Skipping upload. This job is a PULL REQUEST."
	exit 0
fi

source "$SCRIPTDIR/upload-functions"

properties_file=$(ls build/resources/main/org/syncany/plugins/*/plugin.properties)

if [ ! -f "$properties_file" ]; then
	echo "ERROR: Cannot find properties file with plugin details."
	exit 2
fi

mkdir -p $REPODIR/build/upload/

plugin_id=$(get_property $properties_file "pluginId")
release=$(get_property $properties_file "pluginRelease")
snapshot=$([ "$release" == "true" ] && echo "false" || echo "true") # Invert 'release'

echo ""
echo "Files to upload for $plugin_id"
echo "------------------------------"
PWD=`pwd`
cd $REPODIR/build/upload/
sha256sum * 2>/dev/null 
cd "$PWD"

echo ""
echo "Uploading plugin $plugin_id"
echo "------------------------------"

files_jar=$(ls $REPODIR/build/upload/*.jar 2> /dev/null || true)
files_deb=$(ls $REPODIR/build/upload/*.deb 2> /dev/null || true)

for file in $files_deb; do
	echo "Uploading DEB: $(basename $file) ..."
	upload_plugin "$file" "deb" "$plugin_id" "$snapshot" # Most likely to fail first
done

for file in $files_jar; do
	echo "Uploading JAR: $(basename $file) ..."
	upload_plugin "$file" "jar" "$plugin_id" "$snapshot"	
done

if [ "$plugin_id" == "gui" ]; then
	files_exe=$(ls $REPODIR/build/upload/*.exe 2> /dev/null || true)
	files_appzip=$(ls $REPODIR/build/upload/*.app.zip 2> /dev/null || true)

	for file in $files_exe; do
		echo "Uploading EXE: $(basename $file) ..."
		upload_plugin "$file" "exe" "$plugin_id" "$snapshot"	
	done

	for file in $files_appzip; do
		echo "Uploading APP.ZIP: $(basename $file) ..."
		upload_plugin "$file" "app.zip" "$plugin_id" "$snapshot"	
	done
fi
