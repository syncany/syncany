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
package org.syncany.connection.plugins.s3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.ProviderCredentials;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.connection.plugins.rest.RestConnection;

public class S3Connection extends RestConnection {   
    // cp. http://jets3t.s3.amazonaws.com/api/constant-values.html#org.jets3t.service.model.S3Bucket.LOCATION_ASIA_PACIFIC
    private String location;

    @Override
	public void init(Map<String, String> map) throws StorageException {
    	super.init(map);
    	
    	// Additional S3 settings
		location = map.get("location");
		
		if (location == null) {
			throw new StorageException("Config does not contain 'location' setting.");
		}
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
    
    public String[] getMandatorySettings() {
    	String[] superOptions = super.getMandatorySettings();
    	String[] allOptions = new String[superOptions.length+1];
    	
    	System.arraycopy(superOptions, 0, allOptions, 0, superOptions.length);
    	allOptions[allOptions.length-1] = "location";
    	
    	return allOptions;
    }

	@Override
	public String[] getOptionalSettings() {
		return super.getOptionalSettings();
	}

	@Override
	public List<String> getSensitiveSettings() {
		// TODO [medium] Which S3 settings should be hidden?
		return new ArrayList<String>();
	}
}
