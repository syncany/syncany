package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.tests.scenarios.CreateSimilarFileParallelScenarioTest;
import org.syncany.tests.scenarios.EmptyFileScenarioTest;
import org.syncany.tests.scenarios.EmptyFolderScenarioTest;
import org.syncany.tests.scenarios.EvilCUpWithoutDownScenarioTest;
import org.syncany.tests.scenarios.FileTreeMoveToSubfolderScenarioTest;
import org.syncany.tests.scenarios.ManyRenamesScenarioTest;
import org.syncany.tests.scenarios.ManySyncUpsAndDatabaseFileCleanupTest;
import org.syncany.tests.scenarios.ManySyncUpsAndOtherClientSyncDownTest;
import org.syncany.tests.scenarios.MixedUpDownScenarioTest;
import org.syncany.tests.scenarios.SingleFileNoConflictsScenarioTest;
import org.syncany.tests.scenarios.SingleFolderNoConflictsScenarioTest;

@RunWith(Suite.class)
@SuiteClasses({
	MixedUpDownScenarioTest.class,
	ManyRenamesScenarioTest.class,
	EvilCUpWithoutDownScenarioTest.class,
	EmptyFolderScenarioTest.class,
	EmptyFileScenarioTest.class,
	FileTreeMoveToSubfolderScenarioTest.class,
	SingleFileNoConflictsScenarioTest.class,
	SingleFolderNoConflictsScenarioTest.class,
	CreateSimilarFileParallelScenarioTest.class,
	ManySyncUpsAndDatabaseFileCleanupTest.class,
	ManySyncUpsAndOtherClientSyncDownTest.class
})
public class ScenarioTests {
	// This class executes all tests
}
