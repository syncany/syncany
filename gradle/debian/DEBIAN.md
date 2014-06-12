Build Debian/Ubuntu packages and PPA upload
-------------------------------------------

Debian packages are built using the `debuild` tool, uploads to a PPA are managed via `dput`. The Gradle task `gradle debian` can be used to create a standalone unsigned .deb file, the task `gradle debianppa` can be used to creates .changes/.tar.gz/.dsc/.build files for dput-upload.

### Standalone .deb-file

That's easy. The gradle task `gradle debian` copies the `gradle/debian` folder to `build/debian/syncany/debian` and simply runs `debuild`. This creates a valid .deb-file.

### Signed PPA upload

That's hard! To upload to a PPA, we need to do the following things:

1. Generate RSA-4096 keypair, set a passphrase to "***key-priv***", and export it to `syncany-team.asc` (e.g. using Seahorse)
2. Encrypt private key with passphrase "***key-wrap***": `cat syncany-team.asc | gpg --symmetric --cipher-algo aes256 > syncany-team.asc.aes256`
3. Encrypt the keys for Travis-CI in the `.travis.yml` file like this:

    ```
    travis encrypt SYNCANY_GPG_KEY_WRAP=<key-wrap>
    travis encrypt SYNCANY_GPG_KEY_PRIV=<key-priv>
    ```

When running `gradle debianppa`, the `debuild-signed.sh` script will be called. This script does the following things:

1. Write the SYNCANY_GPG_KEY_* variables to the temporary files `key-priv` and `key-wrap`
2. Decrypt the private key file using the temporary key files: `cat syncany-team.asc.aes256 | gpg -d --no-tty --no-use-agent --passphrase-file=key-wrap > syncany-team.asc`
3. Import the private key to the GnuPG keyring: `gpg --homedir=.. --no-tty --no-use-agent --import syncany-team.asc`
4. And then build the .deb-file: `debuild -k<key-id> -S -p"gpg --homedir=.. --no-tty --no-use-agent --passphrase-file=key-priv"`

