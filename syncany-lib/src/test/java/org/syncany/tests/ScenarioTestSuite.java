/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import org.syncany.tests.scenarios.AllFilePossibilitiesScenarioTest;
import org.syncany.tests.scenarios.CallUpWhileStillWritingFileScenarioTest;
import org.syncany.tests.scenarios.ChangedAttributesScenarioTest;
import org.syncany.tests.scenarios.ChangedTypeScenarioTest;
import org.syncany.tests.scenarios.CleanupMergeDatabaseFilesScenarioTest;
import org.syncany.tests.scenarios.CreateSimilarFileParallelScenarioTest;
import org.syncany.tests.scenarios.DirtyDatabaseScenarioTest;
import org.syncany.tests.scenarios.DirtyDatabaseVersionsScenarioTest;
import org.syncany.tests.scenarios.DoSameActionAtTwoClientsTest;
import org.syncany.tests.scenarios.EmptyFileScenarioTest;
import org.syncany.tests.scenarios.EmptyFolderScenarioTest;
import org.syncany.tests.scenarios.EvilCUpWithoutDownScenarioTest;
import org.syncany.tests.scenarios.FileLockedScenarioTest;
import org.syncany.tests.scenarios.FileTreeMoveToSubfolderScenarioTest;
import org.syncany.tests.scenarios.FileVanishedScenarioTest;
import org.syncany.tests.scenarios.FilenameCapitalizationWindowsScenarioTest;
import org.syncany.tests.scenarios.FirstVersionDirtyScenarioTest;
import org.syncany.tests.scenarios.FolderPermissionChangeScenarioTest;
import org.syncany.tests.scenarios.IdenticalFileMoveScenarioTest;
import org.syncany.tests.scenarios.IgnoredFileScenarioTest;
import org.syncany.tests.scenarios.Issue143ScenarioTest;
import org.syncany.tests.scenarios.Issue227_2_ScenarioTest;
import org.syncany.tests.scenarios.ManyRenamesScenarioTest;
import org.syncany.tests.scenarios.ManySyncUpsAndDatabaseFileCleanupScenarioTest;
import org.syncany.tests.scenarios.ManySyncUpsAndOtherClientSyncDownScenarioTest;
import org.syncany.tests.scenarios.ManySyncUpsLargeFileScenarioTest;
import org.syncany.tests.scenarios.MixedUpDownScenarioTest;
import org.syncany.tests.scenarios.MoveFolderWithQuotesOnWindowsScenario;
import org.syncany.tests.scenarios.RenameDeleteScenarioTest;
import org.syncany.tests.scenarios.RenameFileWithDiffModifiedDateScenarioTest;
import org.syncany.tests.scenarios.RenameNoDownloadMultiChunksScenarioTest;
import org.syncany.tests.scenarios.RenameToExistingFileScenarioTest;
import org.syncany.tests.scenarios.RestoreFileScenarioTest;
import org.syncany.tests.scenarios.RestrictedXmlCharInFilenameScenarioTest;
import org.syncany.tests.scenarios.SingleFileNoConflictsScenarioTest;
import org.syncany.tests.scenarios.SingleFolderNoConflictsScenarioTest;
import org.syncany.tests.scenarios.SymlinkSyncScenarioTest;

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
