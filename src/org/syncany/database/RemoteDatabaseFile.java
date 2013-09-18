package org.syncany.database;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RemoteDatabaseFile extends DatabaseFile{
	private static Pattern namePattern = Pattern.compile("db-([^-]+)-(\\d+)");
	
	public RemoteDatabaseFile(File remoteFile) {
		super(remoteFile,namePattern);
	}
	
	public RemoteDatabaseFile(String fileName) {
		super(fileName,namePattern);
	}
	
	@Override
	void initializeClientName(Matcher matcher) {
		this.clientName = matcher.group(1);
	}

	@Override
	void initializeClientVersion(Matcher matcher) {
		this.clientVersion = Long.parseLong(matcher.group(2));		
	}


}
