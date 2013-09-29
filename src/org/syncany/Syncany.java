package org.syncany;

import org.syncany.cli.CommandLineClient;

/**
 * 
 * General To-Do list:
 * 
 * TODO [medium] Unsufficient exception handling in the entire application!
 * TODO [low] Linux symlinks not supported yet
 *
 */
public class Syncany extends Client {
	public static void main(String[] args) throws Exception {
		new CommandLineClient(args).start();
	}		
}
