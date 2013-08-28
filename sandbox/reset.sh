#!/bin/bash

./clean.sh

mkdir results

mkdir results/clientA
mkdir results/clientA/local
cp /etc/hosts results/clientA/local
cp /etc/motd results/clientA/local

mkdir results/clientB
mkdir results/clientB/local
cp /etc/profile results/clientB/local

mkdir results/repo

ln -s ../configA.json results/configA.json
ln -s ../configB.json results/configB.json
ln -s ../../SyncanyCore.jar results/SyncanyCore.jar
