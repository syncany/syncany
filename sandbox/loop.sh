#!/bin/bash

CLIENT=$1

if [ -z "$CLIENT" ]; then
	echo "Usage: ./loop.sh (A|B)"
	exit 0;
fi

LOGFILE=results/log$CLIENT.log
rm $LOGFILE 2> /dev/null

echo "Logging to $LOGFILE"
echo -n "Starting down/up-loop every 5 seconds "
while true; do 
	./run.sh "$CLIENT" down >> $LOGFILE 2>&1
	./run.sh "$CLIENT" up >> $LOGFILE 2>&1

	echo -n "."
	sleep 5
done
