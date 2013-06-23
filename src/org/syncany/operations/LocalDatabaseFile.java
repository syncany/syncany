package org.syncany.operations;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalDatabaseFile extends DatabaseFile {
	private static Pattern namePattern = Pattern.compile("local.db");
	
	public LocalDatabaseFile(File remoteFile) {
		super(remoteFile,namePattern);
	}
	
	@Override
	void initializeClientName(Matcher matcher) {
		this.clientName = "local";
	}

	@Override
	void initializeClientVersion(Matcher matcher) {
		this.clientVersion = 0;
	}
}
