package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.scenarios.LongRunningNewAndDeleteScenarioTest;

@RunWith(Suite.class)
@SuiteClasses({	
	// Scenarios
	LongRunningNewAndDeleteScenarioTest.class
})
public class AllLongTestsSuite {
	// This class executes all tests
}
