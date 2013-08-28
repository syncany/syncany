#!/bin/bash

CLIENT=$1
OPERATION=$2

if [ -z "$CLIENT" -o -z "$OPERATION" ]; then
	echo "Usage: ./run.sh (A|B) (up|down)"
	exit 0;
fi

cd results
java -jar SyncanyCore.jar -c config$CLIENT.json $OPERATION
cd ..
