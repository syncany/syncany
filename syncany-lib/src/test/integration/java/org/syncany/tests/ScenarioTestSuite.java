/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.syncany.integration.tests.scenarios.AllFilePossibilitiesScenarioTest;
import org.syncany.integration.tests.scenarios.CallUpWhileStillWritingFileScenarioTest;
import org.syncany.integration.tests.scenarios.ChangedAttributesScenarioTest;
import org.syncany.integration.tests.scenarios.ChangedTypeScenarioTest;
import org.syncany.integration.tests.scenarios.CleanupMergeDatabaseFilesScenarioTest;
import org.syncany.integration.tests.scenarios.CreateSimilarFileParallelScenarioTest;
import org.syncany.integration.tests.scenarios.DirtyDatabaseScenarioTest;
import org.syncany.integration.tests.scenarios.DirtyDatabaseVersionsScenarioTest;
import org.syncany.integration.tests.scenarios.DoSameActionAtTwoClientsTest;
import org.syncany.integration.tests.scenarios.EmptyFileScenarioTest;
import org.syncany.integration.tests.scenarios.EmptyFolderScenarioTest;
import org.syncany.integration.tests.scenarios.EvilCUpWithoutDownScenarioTest;
import org.syncany.integration.tests.scenarios.FileLockedScenarioTest;
import org.syncany.integration.tests.scenarios.FileTreeMoveToSubfolderScenarioTest;
import org.syncany.integration.tests.scenarios.FileVanishedScenarioTest;
import org.syncany.integration.tests.scenarios.FilenameCapitalizationWindowsScenarioTest;
import org.syncany.integration.tests.scenarios.FirstVersionDirtyScenarioTest;
import org.syncany.integration.tests.scenarios.FolderPermissionChangeScenarioTest;
import org.syncany.integration.tests.scenarios.IdenticalFileMoveScenarioTest;
import org.syncany.integration.tests.scenarios.IgnoredFileScenarioTest;
import org.syncany.integration.tests.scenarios.Issue143ScenarioTest;
import org.syncany.integration.tests.scenarios.Issue227_2_ScenarioTest;
import org.syncany.integration.tests.scenarios.Issue247ScenarioTest;
import org.syncany.integration.tests.scenarios.Issue288ScenarioTest;
import org.syncany.integration.tests.scenarios.Issue303ScenarioTest;
import org.syncany.integration.tests.scenarios.Issue316ScenarioTest;
import org.syncany.integration.tests.scenarios.Issue374Pre1965DateScenarioTest;
import org.syncany.integration.tests.scenarios.ManyRenamesScenarioTest;
import org.syncany.integration.tests.scenarios.ManySyncUpsAndDatabaseFileCleanupScenarioTest;
import org.syncany.integration.tests.scenarios.ManySyncUpsAndOtherClientSyncDownScenarioTest;
import org.syncany.integration.tests.scenarios.ManySyncUpsLargeFileScenarioTest;
import org.syncany.integration.tests.scenarios.MixedUpDownScenarioTest;
import org.syncany.integration.tests.scenarios.MoveFolderWithQuotesOnWindowsScenario;
import org.syncany.integration.tests.scenarios.RenameDeleteScenarioTest;
import org.syncany.integration.tests.scenarios.RenameFileWithDiffModifiedDateScenarioTest;
import org.syncany.integration.tests.scenarios.RenameNoDownloadMultiChunksScenarioTest;
import org.syncany.integration.tests.scenarios.RenameToExistingFileScenarioTest;
import org.syncany.integration.tests.scenarios.RestoreFileScenarioTest;
import org.syncany.integration.tests.scenarios.RestrictedXmlCharInFilenameScenarioTest;
import org.syncany.integration.tests.scenarios.SingleFileNoConflictsScenarioTest;
import org.syncany.integration.tests.scenarios.SingleFolderNoConflictsScenarioTest;
import org.syncany.integration.tests.scenarios.SymlinkSyncScenarioTest;

@RunWith(Suite.class)
@SuiteClasses({
		AllFilePossibilitiesScenarioTest.class,
		CallUpWhileStillWritingFileScenarioTest.class,
		ChangedAttributesScenarioTest.class,
		ChangedTypeScenarioTest.class,
		CleanupMergeDatabaseFilesScenarioTest.class,
		CreateSimilarFileParallelScenarioTest.class,
		DirtyDatabaseScenarioTest.class,
		DirtyDatabaseVersionsScenarioTest.class,
		DoSameActionAtTwoClientsTest.class,
		EmptyFileScenarioTest.class,
		EmptyFolderScenarioTest.class,
		EvilCUpWithoutDownScenarioTest.class,
		FileLockedScenarioTest.class,
		FilenameCapitalizationWindowsScenarioTest.class,
		FileTreeMoveToSubfolderScenarioTest.class,
		FileVanishedScenarioTest.class,
		FirstVersionDirtyScenarioTest.class,
		FolderPermissionChangeScenarioTest.class,
		IdenticalFileMoveScenarioTest.class,
		IgnoredFileScenarioTest.class,
		Issue143ScenarioTest.class,
		Issue227_2_ScenarioTest.class,
		Issue247ScenarioTest.class,
		Issue288ScenarioTest.class,
		Issue303ScenarioTest.class,
		Issue316ScenarioTest.class,
		Issue374Pre1965DateScenarioTest.class,
		ManyRenamesScenarioTest.class,
		ManySyncUpsAndDatabaseFileCleanupScenarioTest.class,
		ManySyncUpsLargeFileScenarioTest.class,
		ManySyncUpsAndOtherClientSyncDownScenarioTest.class,
		MixedUpDownScenarioTest.class,
		MoveFolderWithQuotesOnWindowsScenario.class,
		RenameFileWithDiffModifiedDateScenarioTest.class,
		RenameToExistingFileScenarioTest.class,
		RenameNoDownloadMultiChunksScenarioTest.class,
		RenameDeleteScenarioTest.class,
		RestrictedXmlCharInFilenameScenarioTest.class,
		RestoreFileScenarioTest.class,
		SingleFileNoConflictsScenarioTest.class,
		SingleFolderNoConflictsScenarioTest.class,
		SymlinkSyncScenarioTest.class,
})
public class ScenarioTestSuite {
	// This class executes all tests
}
