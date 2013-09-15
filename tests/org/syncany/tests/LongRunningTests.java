package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.scenarios.longrunning.LongRunningLargeFileScenarioTest;
import org.syncany.tests.scenarios.longrunning.LongRunningNewAndDeleteScenarioTest;

@RunWith(Suite.class)
@SuiteClasses({
	LongRunningLargeFileScenarioTest.class,
	LongRunningNewAndDeleteScenarioTest.class
})
public class LongRunningTests {
	// This class executes all tests
}
