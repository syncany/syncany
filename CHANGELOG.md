Change Log
==========

### Syncany 0.1.10-alpha (Date: Tbd.)
- Developer/alpha release (**STILL NOT FOR PRODUCTION USE!**)
- **Breaks compatiblity to 0.1.9-alpha repositories!**
- Features and significant changes:
  + Atomicity for changing operations (up/cleanup, major!) #64
  + Combined `sy` and `syd` script into one #210
  + Add folder to daemon config in `sy init` #215/#153
- Bugfixes and other things:
  + Fix daemon tests #185
  + Rewrite bash completion, fixes --localdir issue #209
  + Fix issue with `sy daemon force-stop` #212
  + Fix multichunk decryption failure cache issue (part of #59)

### Syncany 0.1.9-alpha (Date: 28 Aug 2014)
- Developer/alpha release (**STILL NOT FOR PRODUCTION USE!**)
- Features and significant changes:
  + HTTPS-only for WebSocket/REST-like API and web interface #185/#196
  + Add bash-completion functionality (Debian/Ubuntu) #201
  + New and awesome end user guide at https://syncany.org/r/userguide
- Bugfixes and other things:
  + Implement full cleanup to fix (big!) #187/#193 
  + Fixed database integrity issue (solved by #187)
  + Fixed winning branch race condition #178 (solved by #187)
  + Fixed file not recreated from winning branch #200/#203
  + Add LRU cache for local files; no re-download of exist. multichunks #169

### Syncany 0.1.8-alpha (Date: 9 Aug 2014)
- Developer/alpha release (**STILL NOT FOR PRODUCTION USE!**)
- Breaks compatiblity to 0.1.7-alpha repositories!
- Features and significant changes:
  + New Samba / Windows share plugin #172
  + Public key authentication for the SFTP plugin #134
  + WebSocket/REST-like daemon with user-password authentication. #171
- Bugfixes and other things:
  + Fixed cross dependency issue with plugins #162
  + Fixed signed plugin JAR dependency error #161
  + Fixed Windows/Linux attribute bingo #166
  + Fixed Windows batch script; start/stop PID file issue #163
  + Fixed Linux daemon script such that it can be symlinked #183
  + Fixed running CLI commands while sync is running #182
  + Tests: Added watch server tests for daemon #185
  + Tests: Implement plugin install test #170
  + Added 'type' attribute to database header (DEFAULT/PURGE) #181
  + Prevent standby/hibernate if uploading/downloading #164
  + Altered API backend to include 'conflicts-with' metadata #165
  + Update Arch Linux AUR package #179

### Syncany 0.1.7-alpha (Date: 28 Jul 2014)

- Developer/alpha release (**STILL NOT FOR PRODUCTION USE!**)
- Bugfixes and other things:
  + Fixed with XML restricted chars in filenames #145
  + Fixed issue with identical files during indexing #142
  + Fixed syd.bat classpath issue on Windows for plugins #156
  + Added 'conflicts-with' metadata to plugins, and warnings
    to the `sy plugin install <pluginid>` command #154
  + Updated wrong licenses to GPLv3 #147
  + Add gradle scripts to plugins #146
  + WebDAV plugin: Remove use of deprecated SSL code

### Syncany 0.1.6-alpha (Date: 25 Jun 2014)

- Developer/alpha release (**STILL NOT FOR PRODUCTION USE!**)
- Features and significant changes:
  + Revamp: List and filter file tree and file histories with `sy ls` #86
  + Revamp: Restore old/deleted files with `sy restore` #87
  + Implemented man pages for all Syncany commands: `man (sy|sy-up|..)` #87
  + Change plugin structure to allow arbitrary plugin (e.g. web inteface)
- Bugfixes:
  + Fixed issue with renaming to folder with quotes on Windows #124 
  + Fix uploading large files, i.e. action file handler using same connection
    as upload; #140

### Syncany 0.1.5-alpha (Date: 12 Jun 2014)

- Developer/alpha release (**STILL NOT FOR PRODUCTION USE!**)
- Features and significant changes:
  + Implemented Ubuntu PPA and signed deb files #69
  + Allow detailed daemon configuration:options for up/down/cleanup,
    interval times, announcements, etc. in daemon.xml #105
  + Added retriable transfer managers to support wobbly connections #125
  + Improve daemon wrapper script: PID-based, force-stop, etc. #105
- Bugfixes:
  + Fix undescriptive error message for unknown/removed plugin #119

### Syncany 0.1.4-alpha (Date: 25 May 2014)

- Developer/alpha release (**STILL NOT FOR PRODUCTION USE!**)
- Features and significant changes:
  + Rudimentary daemon implementation: `syd (start|stop|reload|status)`
  + Remove automatic cleanup in 'up', add interval-based cleanup to 'watch',
    relates to #64
- Bugfixes and other things:
  + Fix deletion of invalid merge/cleanup and integrity issues (tough one!) #58
  + Fix remove lost multichunks from other clients' dirty databases (major!) #132
  + Fix wrong 'down' output when only a purge file is applied #129
  + Make vector clock serialization pattern unambigious (only allow A-Z/a-z) #123
  + Fix ignore purged files when loading database files #135
  + Simplify Gradle build script by splitting to several scripts

### Syncany 0.1.3-alpha (Date: 12 May 2014)

- Developer/alpha release (**STILL NOT FOR PRODUCTION USE!**)
- Features:
  + [WebDAV](https://github.com/syncany/syncany-plugin-webdav) now supports 
    HTTP and HTTPS (ask user for certificate confirmation) #50
  + [SFTP](https://github.com/syncany/syncany-plugin-sftp) now implements 
    strict host checking (ask user for host fingerprint confirmation) #127
- Windows-specific:
  + Set JAVA_HOME during installation #121/#122
  + Replace uninstall icons with high-depth icons
  + Ship 'sy.bat' to allow `sy` command on Windows (not only `syncany`)
- Bugfixes:
  + Fix S3 plugin connect failure (delete repo file) #128
  + Proper remote locking for cleanup through action files #104
  + Fix WebDAV plugin/server compatibility #15
  
### Syncany 0.1.2-alpha (Date: 27 Apr 2014)

- Developer/alpha release (**NOT FOR PRODUCTION USE!**)
- Features:
  + Extracted non-core plugins, allow easy plugin installation through
    `sy plugin (list|install|remove)` #26/#104
    - Shipped plugins now only 'local'
    - Installable plugins:
      [FTP](https://github.com/syncany/syncany-plugin-ftp),
      [SFTP](https://github.com/syncany/syncany-plugin-sftp) (no host checking),
      [WebDAV](https://github.com/syncany/syncany-plugin-webdav) (HTTP only),
      [Amazon S3](https://github.com/syncany/syncany-plugin-s3)
  + Ignore files using wildcards in .syignore (e.g. *.bak, *.r??) #108
  + Added Arch Linux 'syncany-git' package #99
  + Allow speicifying HTTP(S)/WebDAV proxy and other global system 
    properties #109
- Bugfixes:
  + Fix semantic in TransferManager `test()` (incl. all plugins) #103/#102
  + WebDAV plugin fix to create "multichunks"/"databases" folder #110
  + Fix "Plugin not supported" stack trace #111
  + Windows build script fix for "Could not normalize path" #107
  + Fix database file name leak of username and hostname #114
  + Check plugin compatibility before installing (check appMinVersion) #104
  + Don't ignore local/remote notifications if sync already running #88
  + Uninstall plugins on Windows (JAR locked) #113/#117
  + Rotate logs to max. 4x25 MB #116
  + Fix multichunk resource close issue #118/#120
  
### Syncany 0.1.1-alpha (Date: 14 Apr 2014)

- Developer/alpha release (**NOT FOR PRODUCTION USE!**)
- Features:
  + Ignoring files using .syignore file #66/#77
  + Arch Linux package support; release version #80 and git version #99
  + Additional command-specific --help texts
- Windows-specific: 
  + Add Syncany binaries to PATH environment variable during setup #84/#91
  + Fixed HSQLDB-path issue #98
- Bugfixes:
  + Timezone fix in tests #78/#90
  + Reference issue "Cannot determine file content for checksum" #92/#94
  + Atomic 'init' command (rollback on failure) #95/#96
- Other things:
  + Tests for 'connect' command  
  + Tests for .syignore

### Syncany 0.1.0-alpha (Date: 30 March 2014)

- First developer/alpha release (**NOT FOR PRODUCTION USE!**)
- Command line interface (CLI) with commands
  + init: initialize local folder and remote repository
  + connect: connect to an existing remote repository
  + up: index and upload local files
  + down: download changes and apply locally
  + status: list local changes
  + ls-remote: list remote changes
  + watch: watches local dir, subscribes to pub/sub, and calls down/up 
           command in a set interval
  + restore: restores a given set of files (experimental)
  + log: Outputs formatted file histories (experimental)
  + genlink: Generates syncany:// links to share
  + cleanup: Deletes old file versions and frees remote space
- Storage plugins:
  + Local: Allows to store repository files in a local/mounted folder 
  + FTP: Allows the use of an FTP folder as repository
  + WebDAV: Allows using a WebDAV folder as repository (currently no HTTPS)

