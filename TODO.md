Syncany To-Do List
=====================
If you'd like to help developing Syncany, there are a few ways to do so.

#### **TODO markers in the Java code**
The code contains lots of *TODO* markers, classified in *high*, *medium* and *low*. Using
the *Tasks* tab in Eclipse, pick one or two and start coding. To get started, check out
the <a href="README.md">README</a> file. 

#### **Major To-Dos**
Besides the TODO markers, there are lots of major things that need doing. We'll eventually
get around to implementing all that, but for now we want to concentrate on making a stable
core. If you have questions, feel free to ask.

* **Mac OS support**: Even though the code base is entirely Java-based, there are currently
  a few platform specific commands in there. We still need people trying out the Mac OS 
  support, and (if it does not work) make it work.

* **Packaging**: Possibly connected to the modulization, Syncany should be releasable -- 
  meaning that there's an EXE file for Windows, a DEB-/RPM-package for Linux, etc. Starting 
  point could be the [installers made by StackSync][5] or [by iqbox][6].

* **Daemonization**: Currently Syncany has a ``watch`` command, but there is no possibility to
  connect a GUI to it. A REST-based server to control and query Syncany is required. Clients
  will be a wizard-based GUI for the setup, and the file manager integration.

* **File manager integration:** The little icons for the Windows Explorer, Nautilus and
  Finder need to be managed by separate native applications communicating with the Syncany
  daemon. The [liferay-nativity][9] is a good starting point. It offers file manager 
  integration on all platforms.

  [5]: https://github.com/stacksync/desktop/tree/master/installers
  [6]: https://code.google.com/p/iqbox-ftp/source/browse/#git/Installer-Linux%253Fstate%253Dclosed
  [9]: https://github.com/liferay/liferay-nativity
