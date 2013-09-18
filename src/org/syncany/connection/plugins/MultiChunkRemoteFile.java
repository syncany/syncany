package org.syncany.connection.plugins;

public class MultiChunkRemoteFile extends RemoteFile {
	public MultiChunkRemoteFile(String name) {
		super(name);
	}
	
    public MultiChunkRemoteFile(String name, Object source) {
        super(name, source);
    }	
}
