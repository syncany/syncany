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

package name.pachler.nio.file.ext;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import name.pachler.nio.file.WatchService;
import name.pachler.nio.file.impl.LinuxPathWatchService;
import name.pachler.nio.file.impl.PathImpl;
import name.pachler.nio.file.Path;
import name.pachler.nio.file.impl.BSDPathWatchService;
import name.pachler.nio.file.impl.PollingPathWatchService;
import name.pachler.nio.file.impl.WindowsPathWatchService;

/**
 * The Bootstrapper is used to instantiate WatchService and Path instances.
 * Because jpathwatch does not implement all the underlying infrastructure
 * of JDK7's nio implementation, the non-standard Bootstrapper class is used
 * for these chores.
 * @author count
 */
public class Bootstrapper {
	private static final int OSTYPE_UNKNOWN = 0;
	private static final int OSTYPE_LINUX = 1;
	private static final int OSTYPE_WINDOWS = 2;
	private static final int OSTYPE_BSD = 3;

	private static boolean forcePolling = false;
	private static long defaultPollingInterval = 2000;	// by default, polling WatchService implementation poll every 2 seconds

	private static final int ostype;
	static {
		String osName = System.getProperty("os.name");
		if(osName.contains("Windows"))
			ostype = OSTYPE_WINDOWS;
		else if(osName.equals("Linux"))
			ostype = OSTYPE_LINUX;
		else if(osName.equals("Mac OS X") || osName.equals("FreeBSD"))
			ostype = OSTYPE_BSD;
		else
			ostype = OSTYPE_UNKNOWN;
	}

	/**
	 * Creates a new WatchService. This is a shortcut for calling
	 * <code>FileSystems.getDefault().newWatchService()</code> and
	 * is not source-compatible to JDK7
	 * @return	a new WatchService implementation instance.
	 * @see name.pachler.nio.file.FileSystem#newWatchService()
	 * @see name.pachler.nio.file.FileSystems#getDefault()
	 */
    public static WatchService newWatchService(){
		WatchService ws = null;
		try {
			if(!forcePolling){
				switch(ostype){
					case OSTYPE_LINUX:
						ws = new LinuxPathWatchService();
						break;
					case OSTYPE_WINDOWS:
						ws = new WindowsPathWatchService();
						break;
					case OSTYPE_BSD:
						ws = new BSDPathWatchService();
						break;
				}
			}
		} catch(Throwable t){
			Logger.getLogger(Bootstrapper.class.getName()).log(Level.WARNING, null, t);
		} finally {
			// if for whatever reason we don't have a
			// WatchService, we'll create a polling one as fallback.
			if(ws == null)
				ws = new PollingPathWatchService();
			return ws;
		}
    }

	/**
	 * Creates a new Path instance for a given File.
	 * @param file	that a new Path is created for
	 * @return	a new Path() corresponding to the given File
	 */
	public static Path newPath(File file){
		return new PathImpl(file);
	}

	/**
	 * Gets the File that corresponds to the given path.
	 * @param path Path for with to retreive the corresponding File
	 * @return	The file which corresponds to the given Path instance.
	 */
	public static File pathToFile(Path path){
		return ((PathImpl)path).getFile();
	}

	/**
	 * @return whether polling is enforced.
	 */
	public static boolean isForcePollingEnabled(){
		return forcePolling;
	}

	/**
	 * When force polling is enabled, the Bootstrapper's {@link #newWatchService()}
	 * method will only produce polling watch services. This feature is mostly
	 * useful for testing and debugging (and not not much else really).
	 * @param forcePollingEnabled true to enable force polling
	 */
	public static void setForcePollingEnabled(boolean forcePollingEnabled){
		forcePolling = forcePollingEnabled;
	}
	
	/**
	 * <p>This method allows to set the default polling time interval for
	 * new WatchService implementations that use polling. Note that polling
	 * is only used on a few supported platforms when certain event kinds
	 * are used or on unsupported platforms (fallback implementation).</p>
	 * <p>The polling interval determines how often a thread that calls
	 * {@link WatchService#take() } will wake up to check if files in the
	 * watched directory have changed. Longer time intervals will make
	 * a polling service less accurate, but costs less in CPU and disk
	 * resources, while shorter time intervals lead to higher accuracy but
	 * more consumed resources (up to the point where polling takes longer than
	 * the set interval in which case the machine will become very slow).</p>
	 * @param pollInterval	the polling time interval in milliseconds
	 */
	public static void setDefaultPollingInterval(long pollInterval){
		if(pollInterval <= 0)
			throw new IllegalArgumentException("polling interval must be greater than zero");
		defaultPollingInterval = pollInterval;
	}
	
	/**
	 * Retrieves the default polling interval.
	 * @see setDefaultPollingInterval
	 * @return	the default polling interval, in milliseconds
	 */
	public static long getDefaultPollingInterval() {
		return defaultPollingInterval;
	}
}
