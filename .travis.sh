#!/bin/bash

set -e

# Run JUnit tests and generate reports
./gradlew testGlobal jacocoRootReport javadocAll

# SonarQube disabled for now. Took >43min on Travis!

# sonarRunnerTask=$([ -n "$TRAVIS_PULL_REQUEST" -a "$TRAVIS_PULL_REQUEST" != "false" ] && echo "" || echo "sonarRunner")
# ./gradlew testGlobal coberturaReport performCoverageCheck javadocAll $sonarRunnerTask

# Line of code stats
cloc --quiet --xml --out=build/reports/cloc.xml $(find -type d -name main | grep src/main)

# Generate manpages
./gradlew manpages

# Create distributables
./gradlew distTar
./gradlew distZip
./gradlew debian
./gradlew exe

# Upload distributables and JavaDoc
./gradle/upload/upload-application.sh
