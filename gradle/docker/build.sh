#!/bin/bash

docker build -t syncany:latest release
docker build -t syncany:snapshot snapshot
