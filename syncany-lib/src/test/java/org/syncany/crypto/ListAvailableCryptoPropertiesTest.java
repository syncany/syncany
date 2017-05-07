/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.crypto;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.config.Logging;

public class ListAvailableCryptoPropertiesTest {
	private static final Logger logger = Logger.getLogger(ListAvailableCryptoPropertiesTest.class.getSimpleName());		
	
	static {
		Logging.init();
		CipherUtil.init();
	}		

	@Test 
	public void listCryptoSettingsAvailable() {
		logger.log(Level.INFO, "Listing security providers and properties:");
		
		for (Provider provider: Security.getProviders()) {
			logger.log(Level.INFO, "- Provider '"+provider.getName()+"' ");
			
			List<String> propertyNames = new ArrayList<String>();
			propertyNames.addAll(provider.stringPropertyNames());
			
			Collections.sort(propertyNames);
			
			for (String key : propertyNames) {
				logger.log(Level.INFO, "   "+provider.getName()+" / "+key+" = "+provider.getProperty(key));
			}
		}
	}		
}
