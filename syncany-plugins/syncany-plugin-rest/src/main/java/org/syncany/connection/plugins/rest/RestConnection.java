/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.connection.plugins.rest;

import java.util.Map;
import java.util.TreeMap;

import org.jets3t.service.security.ProviderCredentials;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.PluginSetting;
import org.syncany.connection.plugins.PluginSetting.ValueType;

/**
 * The REST connection represents the settings required to create to a
 * backend based on a bucket-based storage such as Amazon S3 or Google Storage.
 * 
 * <p>This class is an abstraction for the concrete implementations offered
 * by the Jets3t library. It is used to configure the REST connection using the
 * {@link ProviderCredentials} class.
 * 
 * In addition to extending this class, implementations must offer a separate
 * {@link Plugin}, as well as an implementation of the {@link RestTransferManager}.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public abstract class RestConnection extends Connection {
    protected String accessKey;
    protected String secretKey; 
    protected String bucket;    
    protected ProviderCredentials credentials;
    protected Map<String, PluginSetting> settings = null;
    
    @Override
	public void init() {
    	Map<String, PluginSetting> map = getSettings();
		accessKey = map.get("accessKey").getValue();
		secretKey = map.get("secretKey").getValue();
		bucket = map.get("bucket").getValue();
	}   
    
    
    @Override
    public Map<String, PluginSetting> getSettings() {
    	if (settings == null) {
    		settings = new TreeMap<String, PluginSetting>();
    		settings.put("accessKey", new PluginSetting(ValueType.STRING, true, false));
    		settings.put("secretKey", new PluginSetting(ValueType.STRING, true, true));
    		settings.put("bucket", new PluginSetting(ValueType.STRING, true, false));
    	}
    	return settings;
    }
    
    public String getAccessKey() {
        return accessKey;
    }

    public String getBucket() {
        return bucket;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public ProviderCredentials getCredentials() {
        if (credentials == null) {
            credentials = createCredentials();
        }
        
        return credentials;
    }        
    
    protected abstract ProviderCredentials createCredentials();        
}
