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
package org.syncany.connection.plugins.s3;

import java.util.Map;

import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.ProviderCredentials;
import org.syncany.connection.plugins.PluginOptionSpec;
import org.syncany.connection.plugins.PluginOptionSpec.ValueType;
import org.syncany.connection.plugins.PluginOptionSpecs;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.connection.plugins.rest.RestConnection;

public class S3Connection extends RestConnection {   
    // cp. http://jets3t.s3.amazonaws.com/api/constant-values.html#org.jets3t.service.model.S3Bucket.LOCATION_ASIA_PACIFIC
    private String location;

    @Override
	public void init(Map<String, String> optionValues) throws StorageException {
    	super.init(optionValues);    	
		location = optionValues.get("location");
	}

    @Override
    public TransferManager createTransferManager() {
        return new S3TransferManager(this);
    }

    @Override
    protected ProviderCredentials createCredentials() {       
        return new AWSCredentials(getAccessKey(), getSecretKey());
    }    

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    
    @Override
    public PluginOptionSpecs getOptionSpecs() {
    	PluginOptionSpecs optionSpecs = super.getOptionSpecs();    	
    	optionSpecs.add(new PluginOptionSpec("location", "Location", ValueType.STRING, true, false, null));

    	return optionSpecs;
    }    
}
