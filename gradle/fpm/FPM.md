Using the Effing Package Manager (FPM)
--------------------------------------

There's a Gradle task to use [FPM](https://github.com/jordansissel/fpm) 
to create Debian (*debian*) and RPM packages (*rpm*). To use these tasks,
you must install the following dependencies.


#### Requirements

Install FPM using apt-get (or your system's package manager):

        $ sudo apt-get install ruby ruby-dev build-essential rubygems
        $ sudo gem install fpm
        
Now you should be able to build Debian packages. If you also want to build
RPM packages, install "rpm":

        $ sudo apt-get install rpm        


#### Gradle tasks "debian" and "rpm"

Support for FPM to Gradle is added by the [FPM plugin](https://github.com/kenshoo/gradle-fpm-plugin).
The Gradle tasks *debian* and *rpm* are configured in Gradle build scripts of 
all the distributables. As of now, that's only the syncany-cli package. Later on
this will also be the GUI.

The "postinst" and "prerm" scripts are kept in the "gradle/fpm" subdirectory.


#### Running the Gradle tasks
        
After that, you should be able to use these tasks like this:

        $ gradle debian    (for Debian)
        $ gradle rpm       (for RPM)
