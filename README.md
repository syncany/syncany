Syncany [![Build Status](https://travis-ci.org/binwiederhier/syncany.png?branch=master)](https://travis-ci.org/binwiederhier/syncany) [![Coverage Status](http://api.syncany.org/badge/coverage.php)](http://syncany.org/reports/coverage/) [![Test Status](http://api.syncany.org/badge/tests.php)](http://syncany.org/reports/tests/) [![Lines of Code](http://api.syncany.org/badge/lines.php)](http://syncany.org/reports/cloc.xml)
=======
> **Important:** Please be aware that this is still **ALPHA code**! Do not use it
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

- [Download and install Syncany](#download-and-install-syncany)
- [Sample usage](#sample-usage)
- [Build and test Syncany](#build-and-test-syncany)
- [Licensing, website and contact](#licensing-website-and-contact)

Download and install Syncany
----------------------------

Sample usage
------------

Usage is pretty similar to a version control system. If you have used Git or
SVN, it should feel a lot alike.

**1. Initialize a local directory**

        $ sy init 
        
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
local folder. You can now use `sy connect` to connect to this repository
from other clients.

**2. Add files and synchronize**

To let Syncany do everything automatically, simple use the `sy watch` command. 
This command will synchronize your local files. 

        $ sy watch 

You can also manually trigger the upload of your local files or the download of remote changes:

        $ sy up
        $ sy down

For a detailed demo, please refer to a [screencast](https://github.com/binwiederhier/syncany/wiki/Documentation).


Build and test Syncany
----------------------
Check out the wiki page for instructions about [how to build Syncany](https://github.com/binwiederhier/syncany/wiki/Building), [documentation and screencasts](https://github.com/binwiederhier/syncany/wiki/Documentation) and [other things](https://github.com/binwiederhier/syncany/wiki). If you're a developer, be sure to look at
the [issue tracker](https://github.com/binwiederhier/syncany/issues?state=open), in particular at the 
[issues marked help:needed](https://github.com/binwiederhier/syncany/issues?labels=status%3Ahelp-needed).


Buy us a coffee
---------------
If you like what you see and you want to support us, you can buy us a coffee or a beer. There are maaanny ways to do so. Any contribution is much appreciated!

- Give a pal some bucks [with PayPal](http://www.syncany.org/donate.html)
- Break some hashes for us with Bitcoins: **1626wjrw3uWk9adyjCfYwafw4sQWujyjn8**
- Be a charmer and [flattr us](https://flattr.com/thing/290043/Syncany)
 
Licensing, website and contact
------------------------------

Syncany is licensed under the GPLv2 open source license. It is mainly developed by [Philipp C. Heckel](http://blog.philippheckel.com/). We are always looking for people to join or help out. Feel free to contact us:

- [Syncany website](http://www.syncany.org/), still with screenshots of the old interface
- [Mailing list](https://launchpad.net/~syncany-team), still on Launchpad (**active!**)
- [IRC channel #syncany on Freenode](http://webchat.freenode.net/?channels=syncany) (my nick is *binwiederhier*)
- [@syncany on Twitter](http://twitter.com/#!/syncany), somewhat quiet there, though ...
