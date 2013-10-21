package org.syncany.config.to;

import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementMap;

public abstract class TypedPropertyListTO {
	@Attribute(required=true)
	private String type;

	@ElementMap(entry="property", key="name", required=false, attribute=true, inline=true)
	protected Map<String, String> settings;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public Map<String, String> getSettings() {
		return settings;
	}

	public void setSettings(Map<String, String> settings) {
		this.settings = settings;
	}				
}
