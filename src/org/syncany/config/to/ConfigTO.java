package org.syncany.config.to;

import java.io.File;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config.ConfigException;

@Root(name="config")
@Namespace(reference="http://syncany.org/config/1")
public class ConfigTO {
	@Element(name="machinename", required=true)
	private String machineName;
	
	@Element(name="password", required=false)
	private String password;
	
	@Element(name="connection", required=true)
	private ConnectionTO connectionTO;	
	
	public static ConfigTO load(File file) throws ConfigException {
		try {
			return new Persister().read(ConfigTO.class, file);
		}
		catch (Exception ex) {
			throw new ConfigException("Config file does not exist or is invalid: "+file, ex);
		}
	}	
	
	public static void save(ConfigTO configTO, File file) throws Exception {
		Serializer serializer = new Persister();
		serializer.write(configTO, file);	
	}
	
	public String getMachineName() {
		return machineName;
	}

	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public ConnectionTO getConnectionTO() {
		return connectionTO;
	}

	public void setConnection(ConnectionTO connectionTO) {
		this.connectionTO = connectionTO;
	}

	public static class ConnectionTO extends TypedPropertyListTO {
		// Nothing special about this
	}
}
