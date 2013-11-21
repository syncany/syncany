Syncany To-Do List
=====================
If you'd like to help developing Syncany, there are a few ways to do so.

#### **TODO markers in the Java code**
The code contains lots of *TODO* markers, classified in *high*, *medium* and *low*. Using the *Tasks* tab in Eclipse, pick one or two and start coding. To get started, check out the <a href="README.md">README</a> file. 

#### **Major To-Dos**
Besides the TODO markers, there are lots of major things that need doing. We'll eventually get around to implementing all that, but for now we want to concentrate on making a stable core. If you have questions, feel free to ask.

* **Mac OS support**: Even though the code base is entirely Java-based, there are currently
  a few platform specific commands in there. We still need people trying out the Mac OS 
  support, and (if it does not work) make it work.

* **Modulization:** Currently Syncany is just one [Ivy][1] module and includes the plugins
   and the command line client. To allow easier plugin-development and module separation, the
   project should be split in the following modules: 
     
    * *syncany-lib*: Main library -- operations, chunking, crypto stuff, database, etc.
    * *syncany-cli*: Command line interface classes (cli-package)
    * *syncany-plugin-xy*: Each plugin in a separate module, with separate dependencies
     
   I already tried doing that with [Ivy on Launchpad][2], based on the 
   [Ivy multi-module demo][3]. I am not convinced that Ivy is the right way to go. You can
   also try using [Maven][4].

* **Packaging**: Possibly connected to the modulization, Syncany should be releasable -- 
  meaning that there's an EXE file for Windows, a DEB-/RPM-package for Linux, etc. Starting 
  point could be the [installers made by StackSync][5].

* **Daemonization**: Currently Syncany has a ``watch`` command, but there is no possibility to
  connect a GUI to it. A REST-based server to control and query Syncany is required. Clients
  will be a wizard-based GUI for the setup, and the file manager integration.

* **Graphical user interface:** We need a platform-independent GUI, i.e. a few wizard-like
  setup screens and a corresponding tray icon. It would be very nice to have a Java-based
  GUI, but if Python or a Qt-based frontend is easier, that's also okay. The GUI should 
  connect to the daemon (as described above).

* **File manager integration:** The little icons for the Windows Explorer, Nautilus and
  Finder need to be managed by separate native applications communicating with the Syncany
  daemon. The [liferay-nativity][6] is a good starting point. It offers file manager 
  integration on all platforms.


  [1]: http://ant.apache.org/ivy/
  [2]: http://bazaar.launchpad.net/~syncany-team/syncany/core3/files
  [3]: http://ant.apache.org/ivy/history/latest-milestone/tutorial/multiproject.html
  [4]: http://maven.apache.org/
  [5]: https://github.com/stacksync/desktop/tree/master/installers
  [6]: https://github.com/liferay/liferay-nativity
