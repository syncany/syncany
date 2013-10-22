package org.syncany.cli;

import java.util.List;

import org.syncany.connection.plugins.RemoteFile;

public class LsRemoteCommand extends Command {
	@Override
	public boolean needConfigFile() {	
		return true;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		List<RemoteFile> remoteStatus = client.remoteStatus();
		
		if (remoteStatus.size() > 0) {
			for (RemoteFile unknownRemoteFile : remoteStatus) {
				out.println("? "+unknownRemoteFile.getName());
			}
		}
		else {
			out.println("No remote changes.");
		}
		
		return 0;
	}
}
