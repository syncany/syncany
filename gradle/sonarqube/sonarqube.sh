#!/bin/bash

if [ -n "$TRAVIS_PULL_REQUEST" -a "$TRAVIS_PULL_REQUEST" != "false" ]; then
	echo "NOTE: Skipping SonarQube analysis. This job is a PULL REQUEST."
	exit 0
fi

./gradlew sonarRunner

