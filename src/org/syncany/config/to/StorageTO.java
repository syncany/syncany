package org.syncany.config.to;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

@Root(name="storage")
@Namespace(reference="http://syncany.org/storage/1")
public class StorageTO {
	@Element(name="connection")
	private ConnectionTO connection;
	
	public ConnectionTO getConnection() {
		return connection;
	}

	public void setConnection(ConnectionTO connection) { 
		this.connection = connection;
	}

	public static class ConnectionTO extends TypedPropertyListTO {
		// Nothing special about this
	}
}
