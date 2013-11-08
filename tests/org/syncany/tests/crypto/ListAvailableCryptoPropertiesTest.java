package org.syncany.tests.crypto;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.config.Logging;
import org.syncany.crypto.CipherUtil;

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
