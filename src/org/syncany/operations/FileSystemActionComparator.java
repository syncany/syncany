package org.syncany.operations;

import java.util.Comparator;

import org.syncany.database.FileVersion.FileType;
import org.syncany.operations.actions.ChangeFileSystemAction;
import org.syncany.operations.actions.DeleteFileSystemAction;
import org.syncany.operations.actions.FileSystemAction;
import org.syncany.operations.actions.NewFileSystemAction;
import org.syncany.operations.actions.RenameFileSystemAction;

/**
 * Sorts file system actions according to their natural order to prevent scenarios 
 * in which a non-empty directory is deleted, ...
 * 
 * TODO [low] write unit test for FileSystemActionComparator, maybe move it in own class
 */
public class FileSystemActionComparator implements Comparator<FileSystemAction> {
	private static final Object[][] TARGET_ORDER =  new Object[][] {
		new Object[] { DeleteFileSystemAction.class, FileType.FILE }, 
		new Object[] { DeleteFileSystemAction.class, FileType.SYMLINK }, 
		new Object[] { NewFileSystemAction.class, FileType.FOLDER },
		new Object[] { RenameFileSystemAction.class, FileType.FOLDER },
		new Object[] { NewFileSystemAction.class, FileType.FILE },
		new Object[] { NewFileSystemAction.class, FileType.SYMLINK },
		new Object[] { RenameFileSystemAction.class, FileType.FILE },
		new Object[] { RenameFileSystemAction.class, FileType.SYMLINK },
		new Object[] { ChangeFileSystemAction.class, FileType.FOLDER },
		new Object[] { ChangeFileSystemAction.class, FileType.FILE },
		new Object[] { ChangeFileSystemAction.class, FileType.SYMLINK },
		new Object[] { DeleteFileSystemAction.class, FileType.FOLDER }, // FIXME TODO [high] This order is flawed! It works for normal cases, because directories have to be emptied before they are deleted. However, in the case a new file is created at the same name as a directory that is being deleted, this does not work! 
	};
			
	@Override
	public int compare(FileSystemAction a1, FileSystemAction a2) {
		int a1Position = determinePosition(a1);
		int a2Position = determinePosition(a2);
		
		if (a1Position > a2Position) {
			return 1;
		}
		else if (a1Position < a2Position) {
			return -1;
		}
		
		return compareByFullName(a1, a2);
	}
			
	private int compareByFullName(FileSystemAction a1, FileSystemAction a2) {
		// For renamed/deleted, do the longest path first
		if (a1.getClass().equals(DeleteFileSystemAction.class) || a1.getClass().equals(RenameFileSystemAction.class)) {
			return -1 * a1.getFile2().getPath().compareTo(a2.getFile2().getPath());
		}
		
		// For the rest, do the shortest path first
		else if (a1.getClass().equals(NewFileSystemAction.class) || a1.getClass().equals(ChangeFileSystemAction.class)) {
			return a1.getFile2().getPath().compareTo(a2.getFile2().getPath());
		}
		
		return 0;
	}

	@SuppressWarnings("rawtypes")
	private int determinePosition(FileSystemAction a) {
		for (int i=0; i<TARGET_ORDER.length; i++) {
			Class targetClass = (Class) TARGET_ORDER[i][0];
			FileType targetFileType = (FileType) TARGET_ORDER[i][1];
			
			if (a.getClass().equals(targetClass) && a.getType() == targetFileType) {					
				return i;
			}
		}
		
		return -1;
	}
}
