package org.syncany.tests;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.util.TestConfigUtil;

@RunWith(Suite.class)
@SuiteClasses({	
	AllTestsExceptLongRunning.class
})
public class AllTestsExceptLongRunningWithEncryption {
	@Before 
	public void enableEncryption() {
		TestConfigUtil.setCrypto(true);
	}
}
