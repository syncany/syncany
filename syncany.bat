@echo off
SET mypath=%~dp0
SET syncanyjar=%mypath%SyncanyCore.jar

java -Xmx1024m -jar %syncanyjar% %* 
