package org.syncany;

import org.syncany.cli.CommandLineClient;

/**
 * 
 * General To-Do list:
 * 
 * TODO [high] Scenario: local timestamp of file changed, but rest identical --> currently a conflict file is changed. should be: change timestamp
 * TODO [medium] Unsufficient exception handling in the entire application!
 * TODO [low] Ivy-fy project, ad tried in http://bazaar.launchpad.net/~syncany-team/syncany/core3/files/head:/
 * TODO [low] All read-only/write-only files are ignored as of now. File must have +rw. Is this okay?
 * TODO [feature request] Linux symlinks not supported yet
 * TODO [feature request] Add ignore operation and .ignore file
 *
 */
public class Syncany extends Client {
	public static void main(String[] args) throws Exception {
		new CommandLineClient(args).start();
	}		
}
