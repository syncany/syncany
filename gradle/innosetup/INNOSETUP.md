Inno Setup 
----------

We use [Inno Setup](http://www.jrsoftware.org/isinfo.php) to create Windows
installers for Syncany. To completely integrate the exe-generation with the 
build process, we created a small Gradle task (*exe*) for it. 


#### 1. Download an install Inno Setup

To use the *exe* task, you must first install Inno Setup and add its 
program directory to your PATH variable. Especially, the "iscc.exe" 
must be runnable by Gradle.

##### 1.1. On Windows

1. [Download](http://www.jrsoftware.org/download.php/is.exe) and install Inno Setup 
2. Add "C:\Program Files\Inno Setup 5" to [your PATH variable](http://www.computerhope.com/issues/ch000549.htm)

##### 1.2. On Linux

1. Install the Windows emulator Wine
2. [Download Inno Setup](http://www.jrsoftware.org/download.php/is.exe) and install it via Wine
3. Copy the wrapper script [iscc](iscc) to /usr/local/bin

In short:

	sudo apt-get install wine
	wget http://www.jrsoftware.org/download.php/is.exe
	wine is.exe
	sudo cp iscc /usr/local/bin/iscc

If the machine is a machien without display manager, Inno Setup cannot be installed. Instead, you
can extract the is.exe file using [innoextract](http://constexpr.org/innoextract/). The 
innoextract tool is available in many distro repos (e.g. Ubuntu >= 12.10, Debian >= 7).
So in most cases, the following commands are enough:

	sudo apt-get install innoextact
	innoextract is.exe
	mkdir ~/".wine/drive_c/Program Files (x86)/Inno Setup 5"
	cp -a app/* ~/".wine/drive_c/Program Files (x86)/Inno Setup 5"

In case innoextract is not available, use the author's repo:

	sudo apt-get install python-software-properties     (for the add-apt-repository command)
	sudo add-apt-repository ppa:arx/release
	sudo apt-get update
	sudo apt-get install innoextract


#### 2. Create exe-installer with the Gradle "exe" task

The Gradle task "exe" first copies the skeleton input script [setup-cli.iss.skel](setup-cli.iss.skel)
and replaces some variables (application version). It then calls the Inno Setup
compiler (ISCC) with this script and generates an exe-file to 
`syncany-cli/build/innosetup`.

After all that you should be able to run `iscc` from the command line; and hence
Gradle should also be able to use the iscc compiler. You can run it like this:

	gradle exe

