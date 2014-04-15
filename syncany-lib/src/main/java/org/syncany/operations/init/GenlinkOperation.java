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
package org.syncany.operations.init;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.ConfigHelper;
import org.syncany.config.to.ConfigTO;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherSpecs;

/**
 * This operation generates a link which can be shared among users to connect to 
 * a repository. The operation is used by other initializing operations, e.g. connect
 * and init.  
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class GenlinkOperation extends AbstractInitOperation {
    private static final Logger logger = Logger.getLogger(GenlinkOperation.class.getSimpleName());            
    private ConfigTO configTO;
    
    public GenlinkOperation(Config config) {
        super(config);
    }
    
    public GenlinkOperation(ConfigTO configTO) {
        super(null);
        this.configTO = configTO;
    }        
            
    @Override
    public GenlinkOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'GenLink'");
		logger.log(Level.INFO, "--------------------------------------------");                      

		if (configTO == null) {
			configTO = ConfigHelper.loadConfigTO(config.getLocalDir());
		}

		String shareLink = null;
		boolean shareLinkEncrypted = false;
		
		if (configTO.getMasterKey() != null) {
			List<CipherSpec> cipherSpecs = CipherSpecs.getDefaultCipherSpecs(); // TODO [low] Shouldn't this be the same as the application?!
			
			shareLink = getEncryptedLink(configTO.getConnectionTO(), cipherSpecs, configTO.getMasterKey());
			shareLinkEncrypted = true;
		}	
		else {
			shareLink = getPlaintextLink(configTO.getConnectionTO());
			shareLinkEncrypted = false;
		}	
		
		return new GenlinkOperationResult(shareLink, shareLinkEncrypted);
    }          
}