Syncany [![Build Status](https://travis-ci.org/binwiederhier/syncany.png?branch=master)](https://travis-ci.org/binwiederhier/syncany) [![Coverage Status](http://api.syncany.org/badge/coverage.php)](http://syncany.org/reports/coverage/) [![Test Status](http://api.syncany.org/badge/tests.php)](http://syncany.org/reports/tests/) [![Lines of Code](http://api.syncany.org/badge/lines.php)](http://syncany.org/reports/cloc.xml)
=======
> **Important:** Please be aware that this is still **ALPHA code**! Do not use it
                 for important files.

Syncany is an open-source cloud storage and filesharing application. It allows
users to backup and share certain folders of their workstations using any kind
of storage, e.g. FTP, Amazon S3 or Google Storage.

While the basic idea is similar to Dropbox, Syncany is
open-source and additionally provides data encryption and more flexibility in
terms of storage type and provider:

- **Data encryption**: Syncany encrypts the files locally, so that any online
  storage can be used even for sensitive data.  
- **Any storage**: Syncany uses a plug-in based storage system. It can
  be used with any type of remote storage.


**Directly jump to ...**

- [Download and install Syncany](#download-and-install-syncany)
- [Sample usage: Try Syncany](#sample-usage-try-syncany)
- [Build and development instructions](#build-and-development-instructions)
- [Buy us a coffee](#buy-us-a-coffee)
- [Licensing, website and contact](#licensing-website-and-contact)


Download and install Syncany
----------------------------
You can download the current binary packages and installers from the [releases page](https://github.com/binwiederhier/syncany/releases), or from the Syncany [download site](http://syncany.org/dist/). **Please be aware that this is still ALPHA code! Do not use it for important files.**

**Latest release:**   
Syncany 0.1.0-alpha, 30 March 2014, [[tar.gz]](http://syncany.org/dist/syncany-0.1.0-alpha.tar.gz) [[zip]](http://syncany.org/dist/syncany-0.1.0-alpha.zip) [[deb]](http://syncany.org/dist/syncany_0.1.0-alpha_all.deb) [[exe]](http://syncany.org/dist/syncany-0.1.0-alpha.exe)

Quick [install and usage instructions](https://github.com/binwiederhier/syncany/wiki/CLI-quick-howto) can be found in the wiki.   
If you like it a bit more detailed, [there's lots more you can explore](https://github.com/binwiederhier/syncany/wiki).


Sample usage: Try Syncany
-------------------------

Usage is pretty similar to a version control system. If you have used Git or
SVN, it should feel a lot alike.

**1. Initialize a local directory**

```
$ sy init
Choose a storage plugin. Available plugins are: ftp, local, webdav
Plugin: ftp

Connection details for FTP connection:
- Hostname: example.com
- Username: ftpuser
- Password (not displayed): 
- Path: /repo-folder
- Port (optional, default is 21): 

Password (min. 10 chars): (user enters repo password)
Confirm: (user repeats repo password)

Repository created, and local folder initialized. To share the same repository
with others, you can share this link: syncany://storage/1/csbxyS6AA+bSK7OxbOxYQXyeouMeoU...
```
        
This sets up a new repository on the given remote storage and initializes the
local folder. You can now use `sy connect` to connect to this repository
from other clients.

**2. Add files and synchronize**

To let Syncany do everything automatically, simple use the `sy watch` command. 
This command will synchronize your local files. 

```
$ sy watch 
```

You can also manually trigger the upload of your local files or the download of remote changes:

```
$ sy up
$ sy down
```

**3. Connect other clients**   
To connect new clients to an existing repository, use the `sy connect` command.
This will set up your local folder to sync with the chosen remote repository.

```
$ sy connect syncany://storage/1/csbxyS6AA+bSK7OxbOxYQXyeouMeoU...

Password: (user enters repo password)

Repository connected, and local folder initialized.
You can now use the 'syncany' command to sync your files.
```

For a detailed demo, please refer to a [screencast](https://github.com/binwiederhier/syncany/wiki/Documentation).


Build and development instructions
----------------------------------
Excited? Want to help? Or just build it yourself? For information about building, development, documentation, screencasts, diagrams and contributions, please check out **[the Syncany wiki page](https://github.com/binwiederhier/syncany/wiki)**. It'll hopefully give you all the information you need!


Buy us a coffee
---------------
If you like what you see and you want to support us, you can buy us a coffee or a beer. There are maaanny ways to do so.

Break some hashes for us and [donate some Bitcoins](https://blockchain.info/address/1626wjrw3uWk9adyjCfYwafw4sQWujyjn8); or be a charmer and [flattr us](https://flattr.com/thing/290043/Syncany). If that's not for you, why not give us some change [with PayPal](http://www.syncany.org/donate.html)? Any contributions are much appreciated! 

 
Licensing, website and contact
------------------------------

Syncany is licensed under the GPLv2 open source license. It is mainly developed by [Philipp C. Heckel](http://blog.philippheckel.com/). We are always looking for people to join or help out. Feel free to contact us:

- [Syncany website](http://www.syncany.org/), still with screenshots of the old interface
- [Syncany wiki page](https://github.com/binwiederhier/syncany/wiki), **most important resource, and always updated**
- [Mailing list](https://launchpad.net/~syncany-team), still on Launchpad (**active!**)
- [IRC channel #syncany on Freenode](http://webchat.freenode.net/?channels=syncany) (my nick is *binwiederhier*)
- [@syncany on Twitter](http://twitter.com/#!/syncany), somewhat quiet there, though ...
