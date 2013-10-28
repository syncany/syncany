package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.cli.CommandLineInterfaceTest;

@RunWith(Suite.class)
@SuiteClasses({
	// App
	CommandLineInterfaceTest.class
})
public class CliTests {
	// This class executes all tests
}
