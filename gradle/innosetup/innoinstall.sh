#!/bin/bash

rm -rf /tmp/inno
mkdir /tmp/inno
cd /tmp/inno

wget -O is.exe http://www.jrsoftware.org/download.php/is.exe
innoextract is.exe
mkdir -p ~/".wine/drive_c/inno"
cp -a app/* ~/".wine/drive_c/inno"

#mkdir ~/".wine/drive_c/Program Files (x86)/Inno Setup 5"
#cp -a app/* ~/".wine/drive_c/Program Files (x86)/Inno Setup 5"

