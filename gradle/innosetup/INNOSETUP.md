Inno Setup 
----------

We use [Inno Setup](http://www.jrsoftware.org/isinfo.php) to create Windows
installers for Syncany. To completely integrate the exe-generation with the 
build process, we created a small Gradle task (*exe*) for it. 


#### Download an install Inno Setup

To use the *exe* task, you must first install Inno Setup and add its 
program directory to your PATH variable. Especially, the "iscc.exe" 
must be runnable by Gradle.

*On Windows*

1. [Download](http://www.jrsoftware.org/download.php/is.exe) and install Inno Setup 
2. Add "C:\Program Files\Inno Setup 5" to [your PATH variable](http://www.computerhope.com/issues/ch000549.htm)

*On Linux*

1. Install the Windows emulator Wine
2. [Download Inno Setup](http://www.jrsoftware.org/download.php/is.exe) and install it via Wine
3. Copy the wrapper script [iscc](iscc) to /usr/local/bin

In short:

	sudo apt-get install wine
	wget http://www.jrsoftware.org/download.php/is.exe
	wine is.exe
	sudo cp iscc /usr/local/bin/iscc


#### Gradle task "exe"

The Gradle task "exe" first copies the skeleton input script [setup.iss](setup.iss)
and replaces some variables (application version). It then calls the Inno Setup
compiler (ISCC) with this script and generates an exe-file to 
"syncany-cli/build/innosetup".


#### Create exe-installer under Windows

After all that you should be able to run `iscc` from the command line; and hence
Gradle should also be able to use the iscc compiler.

You can run it like this:

	gradle exe

