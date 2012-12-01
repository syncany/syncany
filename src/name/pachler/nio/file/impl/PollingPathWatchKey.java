/*
 * Copyright 2008-2011 Uwe Pachler
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. This particular file is
 * subject to the "Classpath" exception as provided in the LICENSE file
 * that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package name.pachler.nio.file.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import name.pachler.nio.file.Path;
import name.pachler.nio.file.StandardWatchEventKind;

/**
 *
 * @author count
 */
class PollingPathWatchKey extends PathWatchKey{
	private String[] fileNames;
	private long[] lastModifieds;
	private long[] lengths;

	PollingPathWatchKey(PathWatchService service, Path path, int flags) {
		super(service, path, flags);
	}

	/**
	 * creates a snapshot of all directory intries in the watched path.
	 * Note that this list can be out of date already when the function
	 * returns, so frequent updates will be a good idea.
	 * @throws FileNotFoundException	if the getPath() returns a path that
	 *	does not exist or is not accessible
	 */
	String[] makeDirectoryEntryList() throws FileNotFoundException{
		PathImpl pathImpl = (PathImpl)getPath();
		File parentDirectory = pathImpl.getFile();
		File[] newFiles = parentDirectory.listFiles();
		if(newFiles == null)
			throw new FileNotFoundException();
		String[] newFileNames = new String[newFiles.length];
		for(int i=0; i<newFiles.length; ++i)
			newFileNames[i] = newFiles[i].getName();
		Arrays.sort(newFileNames);
		return newFileNames;
	}

	// reads the modification date and file length for each entry in fileList.
	// Note that fileList might be out of date at this point; new entries
	// that have been added to the directory since the list was created won't
	// be in the list and will therefore simply be ignored. However entries
	// that have been removed since will still be in the list. If such an
	// entry is encountered, the deleteRemovedEntries determins whether it
	// will be simply ignored or if that entry is set to null in the entryList.
	// Either way, the modification time and size of such an entry is undefined.
	void readModtimesAndLengths(String[] entryList, long[] modtimes, long[] lengths, boolean deleteRemovedEntries){
		PathImpl pathImpl = (PathImpl)getPath();
		File parentDirectory = pathImpl.getFile();
		
		for(int i=0; i<entryList.length; ++i){
			File file = new File(parentDirectory, entryList[i]);
			long modtime = file.lastModified();
			long length = file.length();
			// modtime==0 means that the file does not exist or the modtime
			// cannot be read otherwise.
			if(modtime == 0 && deleteRemovedEntries){
				// if we can't get the modtime, something's
				//wrong with the file, so we assume it's been removed and
				// clear the corresponding entry in the fileList.
				entryList[i] = null;	
			} else {
				modtimes[i] = modtime;
				lengths[i] = length;
			}
		}
	}
	
	@Override
	public synchronized void setFlags(int flags){
		int oldModifyFlag = getFlags() & PathWatchService.FLAG_FILTER_ENTRY_MODIFY;
		int newModifyFlag = flags & PathWatchService.FLAG_FILTER_ENTRY_MODIFY;
		super.setFlags(flags);
		if(oldModifyFlag > newModifyFlag)
		{
			// if the modify filter flag has been removed, scrap the old modtimes and lengths
			lastModifieds = null;
			lengths = null;
		}
		else if(oldModifyFlag < newModifyFlag && fileNames != null)
		{
			// if the modify filter flag has been added, we'll need to
			// calculate the modtimes for the current fileNames list (even
			// though that list might be out of date). We can't remove
			// entries from the list, as this the list comparison function
			// won't see the change if we modify the old state that we're
			// comparing against (hence the 'false' argument to
			// calculateModtimes())
			lastModifieds = new long[fileNames.length];
			lengths = new long[fileNames.length];
			readModtimesAndLengths(fileNames, lastModifieds, lengths, false);
		}
	}

	/**
	 * Polls for changes on the Path for that is represented by this
	 * PollingPathWatchKey instance. The method will take a snapshot of the
	 * watched directory and compare it against the previously taken snapshot.
	 * If it detected changes (differences between the snapshots), it will
	 * add events to this instance.
	 * @return	true if the watched directory has changed since the last call
	 *	to poll(), false otherwise.
	 * @throws FileNotFoundException	will be thrown if the watched directory
	 *	has become invalid. Note that a KEY_INVALID event will queued in this
	 *	case.
	 */
	boolean poll() throws FileNotFoundException {
		int flags = getFlags();
		int queuedEventsBefore = getNumQueuedEvents();

		// read current file name state in watched directory and
		// create sorted list of the new state (newFileNames)
		String[] newFileNames = makeDirectoryEntryList();
		
		long[] newLastModifieds = null;
		long[] newLengths = null;
		if((flags & PathWatchService.FLAG_FILTER_ENTRY_MODIFY) != 0)
		{

			newLastModifieds = new long[newFileNames.length];
			newLengths = new long[newFileNames.length];
			readModtimesAndLengths(newFileNames, newLastModifieds, newLengths, true);
		}

		// if we had no last modification dates before, use the new ones
		if(lastModifieds == null || lengths == null)
		{
			lastModifieds = newLastModifieds;
			lengths = newLengths;
		}

		// if we've scanned the directory for the first time, we can't create
		// events yet, because there's no old state to to compare to
		if(fileNames == null){
			fileNames = newFileNames;
			return false;
		}

		// compare the old file name list (fileNames) against
		// the new list and produce events for the event list
		int oldIndex = 0;
		int newIndex = 0;
		while(oldIndex < fileNames.length || newIndex < newFileNames.length){

			String oldName = null;
			String newName = null;
			int comparison = 0;

			// the names in the fileNames array can be null if it was
			// discovered that the file was removed after the fileNames array
			// has been obtained with File.listFiles(). This will be the
			// case if we're watching for file modification.
			if(oldIndex < fileNames.length){
				oldName = fileNames[oldIndex];
				if(oldName == null){
					++oldIndex;
					continue;
				}
			} else
				comparison = 1;	// if there's no old entry left, indicate
				// that the new one is 'larger', and will be picked up
				// below

			if(newIndex < newFileNames.length){
				newName = newFileNames[newIndex];
				if(newName == null){
					++newIndex;
					continue;
				}
			}
			else
				comparison = -1;	// if there's no new entry left, indicate
				// that the old one is 'larger', and will be picked up
				// below

			// compare strings if we haven't come to a conclusion above
			if(comparison == 0)
				comparison = oldName.compareTo(newName);

			if(comparison < 0){
				// this means that what is in old is not in new -> a file
				// has been removed, so we need a remove event
				if((flags & PathWatchService.FLAG_FILTER_ENTRY_DELETE)!=0){
					Path p = new PathImpl(new File(oldName));
					PathWatchEvent e = new PathWatchEvent(StandardWatchEventKind.ENTRY_DELETE, p, 1);
					addWatchEvent(e);
				}
				++oldIndex;
			} else if(comparison > 0){
				// we have a file on the new side that wasn't on the old side
				// before - so it must have been created.
				if((flags & PathWatchService.FLAG_FILTER_ENTRY_CREATE)!=0){
					Path p = new PathImpl(new File(newName));
					PathWatchEvent e = new PathWatchEvent(StandardWatchEventKind.ENTRY_CREATE, p, 1);
					addWatchEvent(e);
				}
				// also check if the size is > 0, which indicates that it
				// was written to as well after it has been created - so we
				// have to report ENTRY_MODIFY too in this case.
				if((flags & PathWatchService.FLAG_FILTER_ENTRY_MODIFY)!= 0 ){
					long newLength = newLengths[newIndex];
					if(newLength > 0){
						Path p = new PathImpl(new File(newName));
						PathWatchEvent e = new PathWatchEvent(StandardWatchEventKind.ENTRY_MODIFY, p, 1);
						addWatchEvent(e);
					}
				}
				++newIndex;
			} else {

				if((flags & PathWatchService.FLAG_FILTER_ENTRY_MODIFY)!= 0 ){
					long lastModified = lastModifieds[oldIndex];
					long newLastModified = newLastModifieds[newIndex];
					long length = lengths[oldIndex];
					long newLength = newLengths[newIndex];

					if(lastModified != newLastModified || length != newLength){
						Path p = new PathImpl(new File(newName));
						PathWatchEvent e = new PathWatchEvent(StandardWatchEventKind.ENTRY_MODIFY, p, 1);
						addWatchEvent(e);
					}
				}
				// equal, there's nothing to do here. Advance on both lists
				++oldIndex;
				++newIndex;
			}
		}

		// make the new state the 'current' state.
		fileNames = newFileNames;
		lastModifieds = newLastModifieds;
		lengths = newLengths;
		
		int queuedEventsAfter = getNumQueuedEvents();
		return queuedEventsBefore < queuedEventsAfter;
	}
}
