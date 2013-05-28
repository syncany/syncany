package org.syncany.operations;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoteDatabaseFile {
	private static Pattern namePattern = Pattern.compile("db-([^-]+)-(\\d+)");
	
	private String clientName;
	private long clientVersion;
	
	public RemoteDatabaseFile(String remoteFile) {
		Matcher matcher = namePattern.matcher(remoteFile);
		
		if (!matcher.matches()) {
			throw new RuntimeException("Remote database filename pattern does not match: db-xxxx..-nnnn.. expected.");
		}
		
		this.clientName = matcher.group(1);
		this.clientVersion = Long.parseLong(matcher.group(2));
	}

	public String getClientName() {
		return clientName;
	}

	public long getClientVersion() {
		return clientVersion;
	}	
}
