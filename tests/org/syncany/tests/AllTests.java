package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({	
	AllTestsExceptLongRunning.class,
	LongRunningTests.class
})
public class AllTests {
	// This class executes all tests
}
