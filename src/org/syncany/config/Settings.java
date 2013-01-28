/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

package org.syncany.config;

import java.io.File;
//import java.net.InetAddress;

import org.syncany.Constants;

// TODO: Setting is required before starting up the syncany process
/**
 * 
 * @author Nikolai Hellwig
 */
public class Settings {
	
	private static Settings settings;
	
	private File appDir;
	private File appCacheDir;
	private String machineName;	
	
	public Settings(){
		appDir = null;
		appCacheDir = null;
		machineName = null;
	}
	
	public static Settings getInstance(){
		if(settings == null){
			settings = new Settings();
		}
		
		return settings;
	}
	
	public static Settings createInstance(File appDir, File appCacheDir, String machineName) {
		settings = Settings.getInstance();
		settings.setAppDir(appDir);
		settings.setAppCacheDir(appCacheDir);
		settings.setMachineName(machineName);
		
		return settings;
	}
	
	/* Getter & Setter */

	public File getAppDir() {
		return appDir;
	}

	public void setAppDir(File appDir) {
		this.appDir = appDir;
	}
	
	public String getChunkDbFile(){
		return getAppDir() + File.separator + Constants.DATABASE_FILENAME_CHUNKS;
	}
	
	public String getCloneFileDbFile(){
		return getAppDir() + File.separator + Constants.DATABASE_FILENAME_CLONEFILES;
	}
	
	public String getCloneClientDbFile(){
		return getAppDir() + File.separator + Constants.DATABASE_FILENAME_CLONECLIENT;
	}

	public void setAppCacheDir(File file) {
		appCacheDir = file;
	}

	public File getAppCacheDir() {
		return appCacheDir;
	}
	
	
	/**
	 * Return a wellformed machine-name suitable for creating repository file names
	 * @return
	 */
	public String getMachineName() {
		//String mName = machineName.replace('-', '_');
		return machineName;
	}

	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

}
