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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to manage a PID file in a platform-independent manner. The
 * class only offers two few public methods to create a PID file for the current
 * Java process, and to check whether the process indicated by a PID file is 
 * still running.   
 * 
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
*/
public class PidFileUtil {
	private static final Logger logger = Logger.getLogger(PidFileUtil.class.getSimpleName());
	
	/**
	 * Determines the PID for the current Java process. 
	 * 
	 * <p>This is a non-trivial action, since Java does not offer an easy API. This method tries to
	 * determine the PID using two different methods {@link #getProcessPidImpl1()} and 
	 * {@link #getProcessPidImpl2()} (if the first one fails) and returns the PID it it succeeds.
	 * 
	 * @return The Java process PID, or -1 if the PID cannot be determined
	 */
	public static int getProcessPid() {
		try {
			return getProcessPidImpl1();
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Retrieving Java Process PID failed with first method, trying second ...");

			try {
				return getProcessPidImpl2();
			}
			catch (Exception e1) {
				return -1;
			} 
		}
	}

	/**
	 * Determines the process identifier (PID) for the currently active Java process and writes this
	 * PID to the given file.
	 * 
	 * @see #getProcessPid()
	 */
	public static void createPidFile(File pidFile) throws IOException {
		pidFile.delete();
		
		try (FileWriter pidFileWriter = new FileWriter(pidFile)) {
			String pidStr = "" + getProcessPid();
			
			logger.log(Level.INFO, "Writing PID file (for PID " + pidStr + ") to " + pidFile + " ...");
			
			pidFileWriter.write(pidStr);
			pidFileWriter.close();
		}
		
		pidFile.deleteOnExit();		
	}
	
	/**
	 * Determines whether a process is running, based on the given PID file. The method
	 * reads the PID file and then calls {@link #isProcessRunning(int)}. If the PID file
	 * does not exist, it returns <code>false</code>.
	 */
	public static boolean isProcessRunning(File pidFile) {
		if (pidFile.exists()) {
			try (BufferedReader pidFileReader = new BufferedReader(new FileReader(pidFile))) {
				int pid = Integer.parseInt(pidFileReader.readLine());
				return isProcessRunning(pid);
			}
			catch (Exception e) {
				logger.log(Level.WARNING, "Cannot read pidfile from " + pidFile + ". Assuming process not running.");
				return false;
			}
		}
		else {
			return false;
		}
	}
	
	/**
	 * Determines whether a process with the given PID is running. Depending on the
	 * underlying OS, this method either calls {@link #isProcessRunningUnixLike(int)} 
	 * or {@link #isProcessRunningWindows(int)}. 
	 */
	private static boolean isProcessRunning(int pid) {
		if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			return isProcessRunningUnixLike(pid);
		}
		else if (EnvironmentUtil.isWindows()) {
			return isProcessRunningWindows(pid);
		}
		
		return false;
	}	
	
	/**
	 * Uses the {@link RuntimeMXBean}'s name to determine the PID. On Linux, this name 
	 * typically has a value like <code>12345@localhost</code> where 12345 is the PID.
	 * However, this is not guaranteed for every VM, so this is only one of two implementations.
	 *  
	 * @see http://stackoverflow.com/a/35885/1440785
	 */
	private static int getProcessPidImpl1() throws Exception {
		String pidStr = ManagementFactory.getRuntimeMXBean().getName();

		if (pidStr.contains("@")) {
			int processPid = Integer.parseInt(pidStr.split("@")[0]);
			
			logger.log(Level.INFO, "Java Process PID is " + processPid);
			return processPid;
		}
		else {
			throw new Exception("Cannot find pid from string: " + pidStr);
		}
	}

	/**
	 * Uses the private method <code>VMManagement.getProcessId()</code> of Sun's <code>sun.management.VMManagement</code>
	 * class to determine the PID (using reflection to make the relevant fields visible).
	 * 
	 * @see http://stackoverflow.com/a/12066696/1440785
	 */
	private static int getProcessPidImpl2() throws Exception {
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		Field jvmField = runtimeMXBean.getClass().getDeclaredField("jvm");
		jvmField.setAccessible(true);
		
		// The returned object is of the type 'sun.management.VMManagement', but since we
		// don't need the exact type here and we don't want to reference it in the
		// imports, we'll just hope it's there.
		
		Object vmManagement = jvmField.get(runtimeMXBean); 
		Method getProcessIdMethod = vmManagement.getClass().getDeclaredMethod("getProcessId");
		
		getProcessIdMethod.setAccessible(true);
		int processPid = (Integer) getProcessIdMethod.invoke(vmManagement);
		
		logger.log(Level.INFO, "Java Process PID is " + processPid);
		return processPid;
	}
	
	/**
	 * Determines whether a process with the given PID is running using the POSIX 
	 * <code>ps -p $pid</code> command.
	 * 
	 * @param pid Process ID (PID) to check
	 * @return True if process is running, false otherwise
	 */
	private static boolean isProcessRunningUnixLike(int pid) {
		try {
			Runtime runtime = Runtime.getRuntime();
			Process process = runtime.exec(new String[] { "/bin/ps", "-p", ""+pid });
			
			int killProcessExitCode = process.waitFor();
			boolean processRunning = killProcessExitCode == 0;

			logger.log(Level.INFO, "isProcessRunningUnixLike(" + pid + ") returned " + killProcessExitCode + ", process running = " + processRunning);
			return processRunning;
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Cannot retrieve status of PID " + pid + "; assuming process not running.");
			return false;
		}
	}

	/**
	 * Determines whether a process with the given PID is running using the Windows 
	 * <code>tasklist</code> command.
	 * 
	 * @see http://stackoverflow.com/questions/2533984/java-checking-if-any-process-id-is-currently-running-on-windows
	 */
	private static boolean isProcessRunningWindows(int pid) {
		try {
			Process tasklistProcess = Runtime.getRuntime().exec(new String[] { "cmd", "/c", "tasklist /FI \"PID eq " + pid + "\"" });
			BufferedReader tasklistOutputReader = new BufferedReader(new InputStreamReader(tasklistProcess.getInputStream()));
			
			String line = null;
			boolean processRunning = false;
			
			while ((line = tasklistOutputReader.readLine()) != null) {
				if (line.contains(" " + pid + " ")) {
					processRunning = true;
					break;
				}
			}

			logger.log(Level.INFO, "isProcessRunningWindows(" + pid + ") returned " + line + ", process running = " + processRunning);
			return processRunning;
		}
		catch (Exception ex) {
			logger.log(Level.SEVERE, "Cannot retrieve status of PID " + pid + "; assuming process not running.");
			return false;
		}
	}
}
