package org.syncany.cli;

import java.io.PrintStream;

import org.syncany.Client;

public abstract class Command {
	protected Client client;
	protected PrintStream out;
	
	public void setClient(Client client) {
		this.client = client;
	}
	
	public void setOut(PrintStream out) {
		this.out = out;
	}
	
	public abstract int execute(String[] operationArgs) throws Exception;
}
