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
package org.syncany.util;

import java.io.File;

public class EnvironmentUtil {
	public enum OperatingSystem {
		WINDOWS(false), 
		OSX(true), 
		UNIX_LIKE(true);
		
		private boolean unixLike;
		
		OperatingSystem(boolean unixLike){
			this.unixLike = unixLike;
		}
		
		public boolean isUnixLike() {
			return unixLike;
		}
	};

	private static OperatingSystem operatingSystem;
	
	static {
		operatingSystem = (File.separatorChar == '\\') ? OperatingSystem.WINDOWS : 
			(System.getProperty("os.name").toUpperCase().contains("OS X") ? OperatingSystem.OSX : OperatingSystem.UNIX_LIKE);
	}		

	public static void setOperatingSystem(OperatingSystem aOperatingSystem) {
		operatingSystem = aOperatingSystem;
	}
	
	public static OperatingSystem getOperatingSystem() {
		return operatingSystem;
	}
	
	public static boolean isUnixLikeOperatingSystem() {
		return operatingSystem.isUnixLike();
	}

	public static boolean isWindows() {
		return operatingSystem == OperatingSystem.WINDOWS;
	}	
	
	public static boolean isMacOSX() {
		return operatingSystem == OperatingSystem.OSX;
	}

	public static boolean symlinksSupported() {
		return isUnixLikeOperatingSystem();
	}

	/**
	 * Returns environment running syncany
	 * @return windows_86, windows_64, linux_86, linux_64, mac_64, mac_86
	 */
	public static String getEnvironmentName() {
		String os = System.getProperty("os.name").toLowerCase().split(" ")[0]; 
		String arch = System.getProperty("os.arch");
		String realArch = arch.substring(arch.length()-2, arch.length());

		return os + "_" + realArch;
	}
}
