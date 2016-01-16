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
package org.syncany.util;

import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class LimitedDosFileAttributes implements DosFileAttributes {
	private String dosAttributes;
	
	public LimitedDosFileAttributes(String dosAttributes) {
		if (dosAttributes == null || dosAttributes.length() != 4) {
			throw new IllegalArgumentException("Given DOS attribute string is invalid: " + dosAttributes);
		}
		
		this.dosAttributes = dosAttributes.toLowerCase();		
	}
	
	@Override
	public boolean isReadOnly() {
		return dosAttributes.charAt(0) == 'r';
	}

	@Override
	public boolean isHidden() {
		return dosAttributes.charAt(1) == 'h';
	}

	@Override
	public boolean isArchive() {
		return dosAttributes.charAt(2) == 'a';
	}

	@Override
	public boolean isSystem() {
		return dosAttributes.charAt(3) == 's';
	}
	
	@Override 
	public String toString() {
		return toString(this);
	}
	
	public static String toString(DosFileAttributes dosFileAttributes) {
		StringBuilder sb = new StringBuilder();

		sb.append(dosFileAttributes.isReadOnly() ? "r" : "-");
		sb.append(dosFileAttributes.isHidden() ? "h" : "-");
		sb.append(dosFileAttributes.isArchive() ? "a" : "-");
		sb.append(dosFileAttributes.isSystem() ? "s" : "-");

		return sb.toString();		
	}

	@Override
	public long size() {
		throw new RuntimeException("Not supported.");
	}

	@Override
	public FileTime lastModifiedTime() {
		throw new RuntimeException("Not supported.");
	}

	@Override
	public FileTime lastAccessTime() {
		throw new RuntimeException("Not supported.");
	}

	@Override
	public boolean isSymbolicLink() {
		throw new RuntimeException("Not supported.");
	}

	@Override
	public boolean isRegularFile() {
		throw new RuntimeException("Not supported.");
	}

	@Override
	public boolean isOther() {
		throw new RuntimeException("Not supported.");
	}

	@Override
	public boolean isDirectory() {
		throw new RuntimeException("Not supported.");
	}

	@Override
	public Object fileKey() {
		throw new RuntimeException("Not supported.");
	}

	@Override
	public FileTime creationTime() {
		throw new RuntimeException("Not supported.");
	}
}
