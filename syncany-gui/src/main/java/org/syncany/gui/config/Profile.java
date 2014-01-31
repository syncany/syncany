/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.gui.config;

import java.io.File;

import org.syncany.config.Config;


/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 */
public class Profile {
	private String folder;
	private int watchInterval;
	private boolean automaticSync;
	
	public String getFolder() {
		return folder;
	}
	public void setFolder(String folder) {
		this.folder = folder;
	}
	
	public boolean isAutomaticSync() {
		return automaticSync;
	}
	public void setAutomaticSync(boolean automaticSync) {
		this.automaticSync = automaticSync;
	}
	
	public int getWatchInterval() {
		return watchInterval;
	}
	public void setWatchInterval(int watchInterval) {
		this.watchInterval = watchInterval;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj != null 
			&& (obj instanceof Profile) 
			&& ((Profile)obj).getFolder().equals(this.getFolder());
	}
	
	public boolean isValid() {
		File folderFile = new File(getFolder());
		
		if (folderFile.exists()) {
			for (File file : folderFile.listFiles()) {
				if (file.isDirectory() && file.getName().equals(Config.DIR_APPLICATION)) {
					return true;
				}
			}
			return false;
		}
		return false;
	}
	
	public static Profile getDefault(String f) {
		Profile p = new Profile();
		p.setFolder(f);
		p.setWatchInterval(3000);
		p.setAutomaticSync(true);
		return p;
	}
}