Change Log
==========

### Syncany 0.4.10-alpha (Date: TBA)
- Developer/alpha/maintanance release 
- Bugfixes and other things:
  + Fixed small bug with windows paths #598/#599

### Syncany 0.4.9-alpha (Date: 16 Jan 2017)
- Developer/alpha/maintanance release 
- Bugfixes and other things:
  + Fixed bug with `sy restore` #534/#535
  + Fix .syignore recursive behavior #555/#544
  + Fixed crictical bug preventing functioning with newer JVM #595/#596/#597
 
### Syncany 0.4.7-alpha (Date: 7 Nov 2015)
- Developer/alpha release (**We are now nearing the beta phase. Stay tuned!**)
- Bugfixes and other things:
  + Refactoring and simplification of UpOperation
  + Refactoring DownOperation (better memory management)
  + Refactoring of error handling (don't throw 'Exception')
  + Fix GUI crashes in 'Add folder' wizard #497
  + Fix OSX daemon start/stop to new style #281/#530
  + Fix Windows spaces in path issue #522/#529
  + Fix not resuming transactions if transaction files are corrupt #520

### Syncany 0.4.6-alpha (Date: 11 July 2015)
- Developer/alpha release (**We are now nearing the beta phase. Stay tuned!**)
- Features and significant changes:
  + Use smaller transactions in Up #364
  + Add 'Use short links' option to GUI
- Bugfixes and other things:
  + Update licensing to match GPLv3+ #457
  + Prepare fix for read-after-write issue with S3 and Swift plugin #456
  + Fix S3 read-after-write consistent issue #448
  + Change default crypto to just AES-128/GCM
  + Fix table creation process to not throw an error
  + Fix swift read after write consistent issue #276
  + Create target in `sy init` by default (disable with -T)
  + Fix API to support ARM for platform-independent plugins #495

### Syncany 0.4.5-alpha (Date: 8 May 2015)
- Developer/alpha release (**We are now nearing the beta phase. Stay tuned!**)
- Features and significant changes:
  + Add default .syignore file with typically unwanted files (.DS_Store, ...) #393
  + Add 'prevent standby' toggle in GUI #387
  + Add theme and tray icon selector in GUI
  + Add update check to GUI in general settings panel, and daily update check #415
  + Add support for enums as transfer setting values #280
  + Add automatic OAuth token handling #426
  + Add support for feature aware transfer managers #452
- Bugfixes and other things:
  + Make sure that plugin update on Windows is able to install snapshots #418
  + Working single repos for dropbox plugin #417
  + Fix Unity detection for tray icon selection #413
  + Fix Unity tray disappear after Python process crash by restarting #370
  + Several stability bugfixes #433
  + Don't use web sockets for IPC between daemon and GUI if in same JVM #373
  + Fix daemon PID empty error #439
  + Handle big repositories on dropbox backend better #353
  + Fix homebrew formula to use java >= 1.7 #449
  + Fix GUI not working with OAuth plugins #441

### Syncany 0.4.4-alpha (Date: 22 Mar 2015)
- Developer/alpha release (**We are now nearing the beta phase. Stay tuned!**)
- Features and significant changes:
  + Unit tests for daemon #384/#397
  + Completely re-written Syncany API and website, and open-sourced it
    in the [syncany-website](https://github.com/syncany/syncany-website) repository #167
  + Added 3rd-party plugins, and a 3rd party flag in `sy plugin list` responses
  + Added `sy update check` to manually check for application updates #412
  + Added `--no-delete` option for `up` #263/#399
- Bugfixes and other things:
  + Splitting test suite between unit and integration #384/#391
  + Allowing SFTP public key auth without private key password #390
  + Setting default maxMemory to 512M to enable booting in VMs/low memory devices.

### Syncany 0.4.3-alpha (Date: 25 Feb 2015)
- Developer/alpha release (**We are now nearing the beta phase. Stay tuned!**)
- Features and significant changes:
  + First Mac OSX release (.app.zip) #34
  + Native Mac OSX notifications #335
  + Added support for WS/REST in JSON format #285
  + Make plugins updatable via `sy plugin update` command #300
  + History browser in the GUI (excl. Mac OSX) #333
  + Better defaults for cleanup #345
- Bugfixes and other things:
  + Add man pages and bash completion to tar.gz/.zip distribution, 
    to allow using them in Arch and potentially other releases #148/#388
  + Fix black notification bubble on Linux #339
  + Fix file version table in browse history detail view has too
    much left cell spacing #346
  + Fix flickering and newline-problem on Windows tray icon #337
  + Fix ampersands (&) in menus #367
  + Fix last modified pre-1970 issue #374
  + Fix autostart not working on OSX #371
  + Fix tests to make them clean up after themselves #377/#386

### Syncany 0.4.2-alpha (Date: 10 Jan 2015)
- Patch release to fix serialization issue in 'sy ls'
  when daemon/GUI is running.

### Syncany 0.4.1-alpha (Date: 10 Jan 2015)
- Developer/alpha release (**We are now nearing the beta phase. Stay tuned!**)
- Features and significant changes:
  + New 'Preferences' dialog with ability to remove/add plugins, 
    enable/disable notifications, and change the proxy settings #334/#321
  + New 'Remove folder' feature in tray menu #330
  + New 'Copy link' feature in tray menu #336
  + Added recent changes command 'sy log' #298
- Bugfixes and other things:
  + Fix Dropbox plugin bug when deleting non-existing files #325
  + Cleanup status information / status texts in tray #317/#329
  + Code refactoring based on SonarQube output #302/#326
  + Fix invalid "uploading" status message #314
  + Fix confusing error message when plugin not installed #328
  + Fix sync-forever issue with Windows watcher #338
  + Removed `<hooks>` and `<runAfterDown>` because largly unused #311
  + Fix `sy ls` doesn't display deleted file versions; add `--deleted` #282
  + Fix Syncany trying to read all files in home dir on Arch Linux #342
  
### Syncany 0.4.0-alpha (Date: 28 Dec 2014)
- Developer/alpha release (**We are now nearing the beta phase. Stay tuned!**)
- Features and significant changes:
  + Implement resume functionality for up (**big!**) #141
  + Implement proper init/connect GUI wizards in GUI plugin, and supporting 
    backend REST/WS-requests in core, including OAuth support for plugins (**big!**) #297
  + Support S3-compatible backends in [Amazon S3 plugin](https://github.com/syncany/syncany-plugin-s3) #301
  + New [Flickr plugin](https://github.com/syncany/syncany-plugin-flickr) (store data in images) #304
  + Implement folder management commands: `sy daemon (list|add|remove)` #286
  + Make `sy daemon (add|remove) ..` work with multiple paths/IDs #313
- Bugfixes and other things:
  + `sy genlink -s` now works behind a proxy #291
  + `sy genlink -s` now works when folder is daemon-managed #293
  + GUI: Correctly working icon rotation #296
  + Fix left-over file issue in delete action #303
  + Fix `FileSystemActionReconciliator` issue with unexpected 'delete file' case #316

### Syncany 0.3.0-alpha (Date: 9 Dec 2014)
- **Breaks compatiblity to 0.2.0-alpha local folders!**
- Developer/alpha release (**STILL NOT FOR PRODUCTION USE!**)
- Features and significant changes:
  + Implement [OpenStack Swift plugin](https://github.com/syncany/syncany-plugin-swift) #251
  + Allow setting repo password in `sy connect` and `sy init` via 
    `--password` option #256
  + Rework of cleanup operation. Merge database files every cleanup. #266/#284
  + Added Mac OSX / Homebrew recipe #267/#281 
- Bugfixes and other things:
  + Fix Windows/CMD incompatibility with batch file #270
  + Remove empty database versions during cleanup #208 (part of #266/284)
  + Drops PURGE concept #265 (part of #266/284)
  + Fixing incorrect cleanup rollback #268 (part of #266/284)
  + Fixing 'Checksums do not match' exception; duplicate chunk issue #288
  
### Syncany 0.2.0-alpha (Date: 8 Nov 2014)
- Developer/alpha release (**STILL NOT FOR PRODUCTION USE!**)
- **Breaks compatiblity to 0.1.12-alpha local folders!**   
  From now on, breaking releases will always increase the minor version number.
- Features and significant changes:
  + Allow plugin nesting and interaction via SimpleXML-ification (major!) #192/#240
  + Implement [Dropbox plugin](https://github.com/syncany/syncany-plugin-dropbox) (use Dropbox-provided storage) #226
  + Implement [RAID0 plugin](https://github.com/syncany/syncany-plugin-raid0) (use two other plugins to extend storage) #191
  + Encrypt plugin credentials in config.xml #168
  + New syncany://-link format, support for short syncany://-links (with 
    Syncany link shortener service), support for arbitrary link shorteners.
- Bugfixes and other things:
  + Fix PURGE database file history entries disappear after merging (major!) #252
  + Fix database corruption issue (caused by #252) in #247
  + Fix cannot delete/rename folders on Windows #248
  + Set default log file if no log file given #258
  + Allow user and global plugins (Linux only) #259
  + Fix FTP plugin `testRepoFileExists()` with some FTP servers #262
  + Fix inconsistent DB after cleanup rollback (no issue ID)
  + Harmonize Plugin API OS description calls #264/#253

### Syncany 0.1.12-alpha (Date: 19 Oct 2014)
- Developer/alpha release (**STILL NOT FOR PRODUCTION USE!**)
- Features and significant changes:
  + Working GUI plugin with Windows installer, DEB-file and 
    as plugin installation
  + Allow OS/arch-dependent plugins (e.g. GUI) #245
  + Offer Debian packages for all plugins, and an APT archive
    for plugins and main application 
- Bugfixes and other things:
  + Fix integrity issue with DIRTY databases (major!) #227
  + Make 'sy daemon stop' more reliable on Windows #230
  + Fix permission issue with folders created on Windows #243

### Syncany 0.1.11-alpha (Date: 29 Sep 2014)
- Developer/alpha release (**STILL NOT FOR PRODUCTION USE!**)
- Features and significant changes:
  + Updated, more flexible WebSocket/REST-like API (basis for GUI/web) #205
  + Interactive CLI progress status while uploading/downloading #223/#237
  + Implemented basic daemon hooks for post-sync-down event #155/#237
- Bugfixes and other things:
  + Amazon S3 plugin now uses proxy setitngs #228
  + Windows/Limit: Detect 32-bit Java on 64-bit systems in installer; 
    limit JVM memory with `<maxMemory>` tag in userconfig.xml #222
  + Fixed usage of batch/shell scripts #238

### Syncany 0.1.10-alpha (Date: 16 Sep 2014)
- Developer/alpha release (**STILL NOT FOR PRODUCTION USE!**)
- **Breaks compatiblity to 0.1.9-alpha repositories!**
- Features and significant changes:
  + Atomicity for changing operations (up/cleanup, major!) #64
  + Combined `sy` and `syd` script into one #210
  + Add folder to daemon config in `sy init` #215/#153
  + Bash completion for Arch Linux users #220
- Bugfixes and other things:
  + Fix daemon tests #185
  + Rewrite bash completion, fixes --localdir issue #209
  + Fix issue with `sy daemon force-stop` #212
  + Fix multichunk decryption failure cache issue (part of #59)
  + Make syncany://-links Windows CMD compatible #219/#225

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

