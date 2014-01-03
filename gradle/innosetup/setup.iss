; Syncany Inno Setup SKELETON Script
;
; PLEASE NOTE:
;
; 1. This script is a SKELETON and is meant to be parsed by the Gradle 
;    task "innosetup" before handing it to the Inno Setup compiler (ISCC)
;
; 2. All VARIABLES with a dollar sign and curly brackets are replaced
;    by Gradle, e.g. "applicationVersion" below
;
; 3. The script is COPIED to syncany-cli/build/innosetup before its run,
;    so all relative paths refer to this path!
;
; 4. All BACKSLASHES must be escaped 
;

[Setup]
AppName=Syncany
AppVersion=${applicationVersion}
DefaultDirName={pf}\\Syncany
PrivilegesRequired=none

SourceDir=..\\install\\syncany
OutputDir=..\\..\\innosetup
OutputBaseFilename=syncany-${applicationVersion}

WizardImageFile=..\\..\\innosetup\\setup.bmp
;WizardSmallImageFile=..\\..\\innosetup\\setup-small.bmp

[Files]
Source: "bin\\*"; DestDir: "{app}\\bin"; Excludes: "syncany"
Source: "lib\\*"; DestDir: "{app}\\lib"
