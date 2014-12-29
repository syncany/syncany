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
package org.syncany.util;

import java.io.File;

public class EnvironmentUtil {
	public enum OperatingSystem {
		WINDOWS(false), 
		OSX(true), 
		UNIX_LIKE(true);
		
		private boolean unixLike;
		
		OperatingSystem(boolean unixLike) {
			this.unixLike = unixLike;
		}
		
		public boolean isUnixLike() {
			return unixLike;
		}
	};

	private static OperatingSystem operatingSystem;
	
	static {
		if (File.separatorChar == '\\') {
			operatingSystem = OperatingSystem.WINDOWS;
		}
		else if (System.getProperty("os.name").toUpperCase().contains("OS X")) {
			operatingSystem = OperatingSystem.OSX;
		}
		else {
			operatingSystem = OperatingSystem.UNIX_LIKE;
		}
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
	 * @see http://lopica.sourceforge.net/os.html
	 * @return x86, x86_64, sparc, ppc, armv41, i686, ppc64, powerpc, par-risc, ia64n, pa_risk2.0, pa_risk, power, power_rs, mips, alpha
	 */
	public static String getArchDescription() {
		String realArchDescription = System.getProperty("os.arch").toLowerCase();
		
		switch (realArchDescription) {
		case "i386":
			return "x86";
		case "amd64":
			return "x86_64";
		default:
			return realArchDescription;
		}
	}
	
	/**
	 * @see http://www.prepareitonline.com/forums/prepare/24-developer-discussions/question/1615-what-are-the-possible-values-system-getproperty-os-name-can-return-on-different-systems
	 * @return aix, digital, freebsd, hp, irix, linux, mac, mpe/ix, netware, os/2, solaris, windows
	 */
	public static String getOperatingSystemDescription() {
		String realOperatingSystemDescription = System.getProperty("os.name").toLowerCase().split(" ")[0];
		
		switch (realOperatingSystemDescription) {
		case "linux":
		case "solaris":
		case "freebsd":
		case "aix":
			return "linux";
			
		case "mac":
		case "macosx":
			return "macosx";
			
		case "windows":
			return "windows";
			
		default:
			return realOperatingSystemDescription;
		}
	}
}