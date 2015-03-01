#!/bin/bash

if [ -n "$TRAVIS_PULL_REQUEST" -a "$TRAVIS_PULL_REQUEST" != "false" ]; then
	exit 0
fi

echo "sonarRunner"

