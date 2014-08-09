/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.config.to;

import java.io.File;

import javax.crypto.spec.SecretKeySpec;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Complete;
import org.simpleframework.xml.core.Persist;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.ConfigException;
import org.syncany.crypto.CipherParams;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.util.StringUtil;

/**
 * The config transfer object is used to create and load the local config
 * file from/to XML. The config file contains local config settings of a client,
 * namely the machine and display name, the master key as well as connection
 * information (for the connection plugin).
 * 
 * <p>It uses the Simple framework for XML serialization, and its corresponding
 * annotation-based configuration.  
 *  
 * @see <a href="http://simple.sourceforge.net/">Simple framework</a> at simple.sourceforge.net
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
@Root(name="config", strict=false)
@Namespace(reference="http://syncany.org/config/1")
public class ConfigTO {
	@Element(name="machinename", required=true)
	private String machineName;

	@Element(name="displayname", required=false)
	private String displayName; 
	
	@Element(name="masterkey", required=false)
	private String masterKeyEncoded;
	private SaltedSecretKey masterKey;
	
	@Element(name="connection", required=true)
	private ConnectionTO connectionTO;

	public static ConfigTO load(File file) throws ConfigException {
		try {
			return new Persister().read(ConfigTO.class, file);
		}
		catch (Exception ex) {
			throw new ConfigException("Config file does not exist or is invalid: " + file, ex);
		}
	}

	public String getMachineName() {
		return machineName;
	}

	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public ConnectionTO getConnectionTO() {
		return connectionTO;
	}

	public void setConnectionTO(ConnectionTO connectionTO) {
		this.connectionTO = connectionTO;
	}
	
	public SaltedSecretKey getMasterKey() {
		return masterKey;
	}

	public void setMasterKey(SaltedSecretKey masterKey) {
		this.masterKey = masterKey;
	}

	@Persist
	public void prepare() {
		if (masterKey != null) {
			masterKeyEncoded = StringUtil.toHex(masterKey.getSalt())+"/"+StringUtil.toHex(masterKey.getEncoded());
		}
		else {
			masterKeyEncoded = null;
		}
	}

	@Complete
	public void release() {
		masterKeyEncoded = null;
	}
	
	@Commit
	public void commit() {
		if (masterKeyEncoded != null && !"".equals(masterKeyEncoded)) {
			String[] masterKeyEncodedParts = masterKeyEncoded.split("/");
			
			byte[] saltBytes = StringUtil.fromHex(masterKeyEncodedParts[0]);
			byte[] masterKeyBytes = StringUtil.fromHex(masterKeyEncodedParts[1]);
			
			masterKey = new SaltedSecretKey(new SecretKeySpec(masterKeyBytes, CipherParams.MASTER_KEY_DERIVATION_FUNCTION), saltBytes);
		}
		else {
			masterKey = null;
		}
	}

	public static class ConnectionTO extends TypedPropertyListTO {
		// Nothing special about this
	}
}
