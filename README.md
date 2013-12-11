Syncany
=======
> **Important:** Please be aware that this is still ALPHA code! Do not use it
                 for important files.

Syncany is an open-source cloud storage and filesharing application. It allows
users to backup and share certain folders of their workstations using any kind
of storage, e.g. FTP, Amazon S3 or Google Storage.

While the basic idea is similar to Dropbox and JungleDisk, Syncany is
open-source and additionally provides data encryption and more flexibility in
terms of storage type and provider:

- **Data encryption**: Syncany encrypts the files locally, so that any online
  storage can be used even for sensitive data.  
- **Any storage**: Syncany uses a plug-in based storage system. It can
  be used with any type of remote storage.


**Directly jump to ...**

- [Documentation, diagrams and screencasts](#documentation-diagrams-and-screencasts)
- [Build and test Syncany](#build-and-test-syncany)
- [Sample usage](#sample-usage)
- [Setup Eclipse IDE for development](#setup-eclipse-ide-for-development)
- [How can I help?](#how-can-i-help)
- [Licensing, website and contact](#licensing-website-and-contact)


Documentation, diagrams and screencasts
---------------------------------------

There is quite a bit of reading material on Syncany already. Check out the following links:

**Posts and papers**
- [Blog post: Syncany explained: idea, progress, development and future (part 1)](http://blog.philippheckel.com/2013/10/18/syncany-explained-idea-progress-development-future/) (Oct 2013)
- [Master's thesis: Minimizing remote storage usage and synchronization time using deduplication and multichunking: Syncany as an example](http://blog.philippheckel.com/2013/05/20/minimizing-remote-storage-usage-and-synchronization-time-using-deduplication-and-multichunking-syncany-as-an-example/) (2011)

**Screencasts**
- [Screencast: Developer How-to - Checkout code, compile and run two clients on Linux, using FTP plugin](http://www.youtube.com/watch?v=xE8nGL8U4Gg) (14 minutes)
- [Screencast: Conflict handling on Linux, using local plugin](http://www.youtube.com/watch?v=tvsZcuhVH8c) (2 minutes)
- [Screencast: Setup Amazon S3 for two users, and sync two clients with Syncany](http://www.youtube.com/watch?v=skKzqID_Zrc) (9 minutes)

**Diagrams**
- [Diagram: Syncany application flow example](https://raw.github.com/binwiederhier/syncany/15efd1df039253a3884dea36ca21f58628b32c04/docs/Diagram%20Application%20Flow%202.png)
- [Diagram: Chunking framework class diagram](https://raw.github.com/binwiederhier/syncany/15efd1df039253a3884dea36ca21f58628b32c04/docs/Diagram%20Chunking%20Framework.png)
- [Diagram: Storage plugins class diagram](https://raw.github.com/binwiederhier/syncany/15efd1df039253a3884dea36ca21f58628b32c04/docs/Diagram%20Connection%20Plugins.png)
- [Diagram: Database class diagram](https://raw.github.com/binwiederhier/syncany/15efd1df039253a3884dea36ca21f58628b32c04/docs/Diagram%20Database.png)

**JavaDoc**

We have a quite exhaustive JavaDoc (or at least we're trying to build it up). If you're a developer, be sure to check that out. A few starting points:

- Command line interface: [Syncany.java](https://github.com/binwiederhier/syncany/blob/15efd1df039253a3884dea36ca21f58628b32c04/src/org/syncany/Syncany.java), [Command.java](https://github.com/binwiederhier/syncany/blob/15efd1df039253a3884dea36ca21f58628b32c04/src/org/syncany/cli/Command.java)
- Chunking framework: [Deduper.java](https://github.com/binwiederhier/syncany/blob/15efd1df039253a3884dea36ca21f58628b32c04/src/org/syncany/chunk/Deduper.java), [Chunker.java](https://github.com/binwiederhier/syncany/blob/15efd1df039253a3884dea36ca21f58628b32c04/src/org/syncany/chunk/Chunker.java), [MultiChunker.java](https://github.com/binwiederhier/syncany/blob/15efd1df039253a3884dea36ca21f58628b32c04/src/org/syncany/chunk/MultiChunker.java)
- Storage plugins: [Plugins.java](https://github.com/binwiederhier/syncany/blob/15efd1df039253a3884dea36ca21f58628b32c04/src/org/syncany/connection/plugins/Plugins.java), [Connection.java](https://github.com/binwiederhier/syncany/blob/15efd1df039253a3884dea36ca21f58628b32c04/src/org/syncany/connection/plugins/Connection.java)
- Cryptography / Encryption: [MultiCipherOutputStream.java](https://github.com/binwiederhier/syncany/blob/15efd1df039253a3884dea36ca21f58628b32c04/src/org/syncany/crypto/MultiCipherOutputStream.java), [CipherSpec.java](https://github.com/binwiederhier/syncany/blob/15efd1df039253a3884dea36ca21f58628b32c04/src/org/syncany/crypto/CipherSpec.java), [CipherSession.java](https://github.com/binwiederhier/syncany/blob/15efd1df039253a3884dea36ca21f58628b32c04/src/org/syncany/crypto/CipherSession.java)


Build and test Syncany
----------------------

**Requirements**: Syncany is based on Java 7, so you need that installed. We use
Gradle for dependency management and as build tool. Gradle does all the
dependency magic. It should work out of the box.

**1. Checkout code and build**

        git clone http://github.com/binwiederhier/syncany
        cd syncany        
        ./gradlew installApp         (on Linux / Mac OS)
        gradlew installApp            (on Windows)

This compiles and installs the Syncany command line client to 
`syncany-cli/build/install/syncany-cli`. You can run it from there.

**2. Install command line client (to run `syncany` from anywhere)**

To be able to run `syncany` (or short: `sy`) from anywhere, you can install a 
symbolic link on your system. On Linux, the link is placed in `/usr/local/bin`,
on Windows, a batch file is placed in `C:\Windows`. To do this, run the
following commands:

*On Linux / Mac OS:*

        sudo ./gradlew fakeinstall   (on Linux / Mac OS)
        
*On Windows:*
  - Click *Start*, type `cmd`, and then press CTRL+SHIFT+ENTER. If a warning appear, click *Yes*.
  - In the command box, `cd` to your checkout directory and run `gradlew fakeinstall`
        
Please note: There is no easy way to permanently install Syncany on your system, 
yet. We're working on an installer for Windows, and packages for Linux.
        
**3. Run it!**

        syncany --help        
 

Sample usage
------------

Usage is pretty similar to a version control system. If you have used Git or
SVN, it should feel a lot alike.

**1. Initialize a local directory**

        $ syncany init 
        
        Choose a storage plugin. Available plugins are: ftp, local, s3
        Plugin: ftp
        
        Connection details for FTP connection:
        - hostname: myhost.example.com
        - username: myuser1
        - password: somepassword1
        - port (optional):

        The password is used to encrypt data on the remote storage.
        Please choose it wisely.

        Password: (secret password)
        Confirm: (repeat it)
        
This sets up a new repository on the given remote storage and initializes the
local folder. You can now use `syncany connect` to connect to this repository
from other clients.

**2. Add files and synchronize**

To let Syncany do everything automatically, simple use the `syncany watch` command. This command will synchronize your local files in a given interval. 

        $ syncany watch --interval=20

You can also manually trigger the upload of your local files or the download of remote changes:

        $ syncany up
        $ syncany down

For a detailed demo, please refer to a [screencast](#documentation-diagrams-and-screencasts). 


Setup Eclipse IDE for development
---------------------------------

1. Checkout Syncany on the command line: 

        cd /home/user/workplace
        git clone http://github.com/binwiederhier/syncany
        cd syncany
        
2. Generate Eclipse project files and download dependencies:

        ./gradlew eclipse      (on Linux / Mac OS)
        gradlew eclipse        (on Windows)   

3. Open Eclipse and create a new workplace, e.g. at "/home/user/workplace"
   
4. In Eclipse, go to File -> Import ... -> Existing Projects Into Workplace
   -> Select Root Directory --> Browse ...
   
   - Select "/home/user/workplace/syncany"
   - [x] Mark the *Search nested projects* checkbox 



How can I help?
---------------
Check out the <a href="TODO.md">TODO list</a> for starting points. If you have questions,
don't hesitate to ask!

We're **actively** discussing stuff on the [Syncany mailing list](https://launchpad.net/~syncany-team), 

 
Licensing, website and contact
------------------------------

Syncany is licensed under the GPLv2 open source license. It is mainly developed by [Philipp C. Heckel](http://blog.philippheckel.com/) and [Steffen Dangmann](https://www.xing.com/profiles/Steffen_Dangmann). We are always looking for people to join or help out. Feel free to contact us:

- [Syncany website](http://www.syncany.org/), still with screenshots of the old interface
- [@syncany on Twitter](http://twitter.com/#!/syncany), somewhat quiet there, though ...
- [Mailing list](https://launchpad.net/~syncany-team), still on Launchpad
