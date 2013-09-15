package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({	
	ScenarioTests.class,
	OtherShortTests.class
})
public class AllTestsExceptLongRunning {
	// This class executes all tests
}
