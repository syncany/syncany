package org.syncany;

import org.syncany.cli.CommandLineClient;

/**
 * 
 * General To-Do list:
 * 
 * TODO [medium] Unsufficient exception handling in the entire application!
 * TODO [low] Ivy-fy project, ad tried in http://bazaar.launchpad.net/~syncany-team/syncany/core3/files/head:/
 * TODO [feature request] Add ignore operation and .ignore file
 *
 */
public class Syncany extends Client {
	public static void main(String[] args) throws Exception {
		new CommandLineClient(args).start();
	}		
}
