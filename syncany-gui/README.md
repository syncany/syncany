Syncany GUI Readme
==================

Syncany GUI is based on the following principles

- **SWT Library** graphical library used is SWT

Launch Syncany GUI
------------------

1. Checkout Syncany on the command line and switch to 'gui' branch

        cd /home/user/workplace
        git clone http://github.com/binwiederhier/syncany
        cd syncany
		git checkout gui
		
2. Start Syncany GUI client

*On Linux / Mac OS:*

        sudo ./gradlew runGui
		
For Ubuntu Unity users (from Ubuntu 13) you need to first install the following dependencies to get tray icon properly working
		sudo apt-get install python-appindicator
		sudo apt-get install python-pip
		sudo pip install websocket-client
        
*On Windows:*
  - Click *Start*, type `cmd`, and then press ENTER.
  - In the command box, `cd` to your checkout directory and run `gradlew runGui`