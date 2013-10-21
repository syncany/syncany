package org.syncany.config.to;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

@Root(name="local")
@Namespace(reference="http://syncany.org/local/1")
public class LocalTO {
	@Element(name="machinename", required=true)
	private String machineName;
	
	@Element(name="password", required=false)
	private String password;

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
}
