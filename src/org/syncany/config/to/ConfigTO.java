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
	
	@Element(name="localdir", required=false)
	private String localDir;
	
	@Element(name="appdir", required=false)
	private String appDir;
	
	@Element(name="databasedir", required=false)
	private String databaseDir;
	
	@Element(name="cachedir", required=false)
	private String cacheDir;
	
	@Element(name="logdir", required=false)
	private String logDir;	
		
	@Element(name="connection", required=true)
	private ConnectionTO connectionTO;	

	private String configFile;
	
	public static ConfigTO load(File file) throws ConfigException {
		try {
			Serializer serializer = new Persister();
			ConfigTO configTO = serializer.read(ConfigTO.class, file);
			configTO.configFile = file.getAbsolutePath();
			
			return configTO;
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

	public String getLocalDir() {
		return localDir;
	}

	public void setLocalDir(String localDir) {
		this.localDir = localDir;
	}

	public String getAppDir() {
		return appDir;
	}

	public void setAppDir(String appDir) {
		this.appDir = appDir;
	}

	public String getDatabaseDir() {
		return databaseDir;
	}

	public void setDatabaseDir(String databaseDir) {
		this.databaseDir = databaseDir;
	}

	public String getCacheDir() {
		return cacheDir;
	}

	public void setCacheDir(String cacheDir) {
		this.cacheDir = cacheDir;
	}

	public String getLogDir() {
		return logDir;
	}

	public void setLogDir(String logDir) {
		this.logDir = logDir;
	}

	public String getConfigFile() {
		return configFile;
	}

	public void setConfigFile(String configFile) {
		this.configFile = configFile;
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
