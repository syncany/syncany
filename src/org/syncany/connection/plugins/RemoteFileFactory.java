package org.syncany.connection.plugins;

public class RemoteFileFactory {
	public static <T extends RemoteFile> T createRemoteFile(String name, Class<T> remoteFileClass) throws Exception {
		return remoteFileClass.getConstructor(String.class).newInstance(name);
	}
}
