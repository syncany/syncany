/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import org.syncany.tests.integration.scenarios.AllFilePossibilitiesScenarioTest;
import org.syncany.tests.integration.scenarios.CallUpWhileStillWritingFileScenarioTest;
import org.syncany.tests.integration.scenarios.ChangedAttributesScenarioTest;
import org.syncany.tests.integration.scenarios.ChangedTypeScenarioTest;
import org.syncany.tests.integration.scenarios.CleanupMergeDatabaseFilesScenarioTest;
import org.syncany.tests.integration.scenarios.CreateSimilarFileParallelScenarioTest;
import org.syncany.tests.integration.scenarios.DirtyDatabaseScenarioTest;
import org.syncany.tests.integration.scenarios.DirtyDatabaseVersionsScenarioTest;
import org.syncany.tests.integration.scenarios.DoSameActionAtTwoClientsTest;
import org.syncany.tests.integration.scenarios.EmptyFileScenarioTest;
import org.syncany.tests.integration.scenarios.EmptyFolderScenarioTest;
import org.syncany.tests.integration.scenarios.EvilCUpWithoutDownScenarioTest;
import org.syncany.tests.integration.scenarios.FailedSplitSyncUpScenarioTest;
import org.syncany.tests.integration.scenarios.FileLockedScenarioTest;
import org.syncany.tests.integration.scenarios.FileTreeMoveToSubfolderScenarioTest;
import org.syncany.tests.integration.scenarios.FileVanishedScenarioTest;
import org.syncany.tests.integration.scenarios.FilenameCapitalizationWindowsScenarioTest;
import org.syncany.tests.integration.scenarios.FirstVersionDirtyScenarioTest;
import org.syncany.tests.integration.scenarios.FolderPermissionChangeScenarioTest;
import org.syncany.tests.integration.scenarios.IdenticalFileMoveScenarioTest;
import org.syncany.tests.integration.scenarios.IgnoredFileScenarioTest;
import org.syncany.tests.integration.scenarios.Issue143ScenarioTest;
import org.syncany.tests.integration.scenarios.Issue227_2_ScenarioTest;
import org.syncany.tests.integration.scenarios.Issue247ScenarioTest;
import org.syncany.tests.integration.scenarios.Issue288ScenarioTest;
import org.syncany.tests.integration.scenarios.Issue303ScenarioTest;
import org.syncany.tests.integration.scenarios.Issue316ScenarioTest;
import org.syncany.tests.integration.scenarios.Issue374Pre1965DateScenarioTest;
import org.syncany.tests.integration.scenarios.Issue429ScenarioTest;
import org.syncany.tests.integration.scenarios.Issue520NoResumeOnCorruptXmlScenarioTest;
import org.syncany.tests.integration.scenarios.ManyRenamesScenarioTest;
import org.syncany.tests.integration.scenarios.ManySyncUpsAndDatabaseFileCleanupScenarioTest;
import org.syncany.tests.integration.scenarios.ManySyncUpsAndOtherClientSyncDownScenarioTest;
import org.syncany.tests.integration.scenarios.ManySyncUpsLargeFileScenarioTest;
import org.syncany.tests.integration.scenarios.MixedUpDownScenarioTest;
import org.syncany.tests.integration.scenarios.MoveFolderWithQuotesOnWindowsScenario;
import org.syncany.tests.integration.scenarios.RenameDeleteScenarioTest;
import org.syncany.tests.integration.scenarios.RenameFileWithDiffModifiedDateScenarioTest;
import org.syncany.tests.integration.scenarios.RenameNoDownloadMultiChunksScenarioTest;
import org.syncany.tests.integration.scenarios.RenameToExistingFileScenarioTest;
import org.syncany.tests.integration.scenarios.RestoreFileScenarioTest;
import org.syncany.tests.integration.scenarios.RestrictedXmlCharInFilenameScenarioTest;
import org.syncany.tests.integration.scenarios.SingleFileNoConflictsScenarioTest;
import org.syncany.tests.integration.scenarios.SingleFolderNoConflictsScenarioTest;
import org.syncany.tests.integration.scenarios.SymlinkSyncScenarioTest;

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
		FailedSplitSyncUpScenarioTest.class,
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
		Issue520NoResumeOnCorruptXmlScenarioTest.class,
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
