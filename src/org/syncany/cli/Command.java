package org.syncany.cli;

import java.io.File;
import java.io.PrintStream;

import org.syncany.Client;

public abstract class Command {
	protected File localDir;
	protected Client client;
	protected PrintStream out;	
	
	public void setLocalDir(File localDir) {
		this.localDir = localDir;
	}

	public void setClient(Client client) {
		this.client = client;
	}
	
	public void setOut(PrintStream out) {
		this.out = out;
	}
	
	public abstract int execute(String[] operationArgs) throws Exception;
	public abstract boolean initializedLocalDirRequired();
}
