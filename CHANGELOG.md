Change Log
==========

### Release 0.1.2-alpha (Date: tbd.)

- Not released yet.

### Release 0.1.1-alpha (Date: 14 Apr 2014)

- Features:
  + Ignoring files using .syignore file #66/#77
  + Arch Linux package support; release version #80 and git version #99
  + Additional command-specific --help texts
- Windows-specific: 
  + Add Syncany binaries to PATH environment variable during setup #84/#91
  + Fixed HSQLDB-path issue #98
- Bugfixes
  + Timezone fix in tests #78/#90
  + Reference issue "Cannot determine file content for checksum" #92/#94
  + Atomic 'init' command (rollback on failure) #95/#96
- Other things
  + Tests for 'connect' command  
  + Tests for .syignore

### Release 0.1.0-alpha (Date: 30 March 2014)

- First developer/alpha release (NOT FOR PRODUCTION USE!)
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

