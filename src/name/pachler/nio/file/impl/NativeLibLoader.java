/*
 * This file has been derived from the NatviveLibLoader class in rxtx.
 * Please see the licence attached below.
 */
    

/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright (c) 2008 by Trent Jarvi taj@www.linux.org.uk.
|
|   Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
|
|   The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
|
|   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
|
--------------------------------------------------------------------------*/
package name.pachler.nio.file.impl;

import java.io.*;
import java.nio.channels.FileLock;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class loads native libraries packaged in the JAR file that this class resides in.
 * The NativeLibLoader supports a number of preconfigured native libraries,
 * which it looks up by their name to find, extract and load their binary file.
 *
 * For each platform there exists a PlatformArchLibSet, which defines a list of
 * native libraries available for that platform in LibraryImplementation instances.
 *
 * When a library "foo" is requested through the loadLibrary() method,
 * all LibraryImplementation instances for the current platform's PlatformArchLibSet
 * is searched.
 *
 * If a matching LibraryImplementation is found, first the library extraction and
 * loading is attempted with a default library name. If that fails, extraction and
 * loading is attempted with a unique temporary file name for the library.
 * the steps are the following:
 * <ol>
 *	<li> It is attempted to open and lock file indicated by the library
 *	path for reading. If successful, the integrity if the file is checked (if
 *	it is the same as the one stored in the JAR file. If successful the library
 *	is loaded.</li>
 *	<li>If the step above fails, it is attempted to open and lock the default
 *	library file for writing. If successful, the library file is extracted to
 *	the path and the library is loaded from there.</li>
 * </ol>
 * If all of that fails, an attempt is to load the library with
 * System.loadLibrary(), which requires the library to be installed in the
 * system's native library path. This is the old (but common) way to load
 * native libraries in Java, but requires the library to be installed
 * separately in a native library directory accessible by the JVM.
 *
 * <h2>The Default Library Path</h2>
 *	The default library path is something like "/tmp/bar-1-0-mylib.so". Because
 * it remains the same on multiple invocations of the same program, one program
 * might find a library file from a previously running program that hasn't been
 * cleaned up yet. The library file might even be there from a program that is
 * still running, creating the potential for race conditions, so
 * the implementation is careful to avoid clashes in such cases (such as two
 * JVMs trying to extract the same library to the same file at the same time).
 *
 * The default library path is formed as
 *	{tempdir}/{productname}-{major}-{minor}-{binaryname}, where {productname}
 *	is the product name that's configured in NativeLibLoader's PRODUCTNAME
 *	static variable, {major} and {minor} are the major and minor versions
 *	configured for the LibraryImplementation, and {binaryname} is the name of
 *	the binary as it is stored in the JAR (without the path), like mylib.so.<br>
 *	So for a library "foo" stored in the JAR as "Windows/mylib.dll" with version
 *	1.0, for PRODUCTNAME being "bar", the default library path would be
 *	"c:\Documents and Settings\myuser\temp\bar-1-0-mylib.dll"
 *
 * @author Uwe Pachler, Trent Jarvi
 * @version %I%, %G%
*/
public class NativeLibLoader
{

	private static void loadDefaultLibrary(String name) {
		System.loadLibrary(name);
	}

	private static class LibraryImplementation
	{
		public String libraryName;
		public String libraryResource;
		LibraryImplementation(String libraryName, String libraryResource)
		{
			this.libraryName = libraryName;
			this.libraryResource = libraryResource;
		}
	}
	
	private static class PlatformArchLibSet
	{
		public String osName;
		public String archName;
		public LibraryImplementation[] implementations;
		private String[] command;
		PlatformArchLibSet(String osName, String archName, LibraryImplementation[] implementations, String[] command)
		{
			this.osName = osName;
			this.archName = archName;
			this.implementations = implementations;
			this.command = command;
		}
		LibraryImplementation findImplementation(String name)
		{
			for(int i=0; i<implementations.length; ++i)
			{
				if(name.equals(implementations[i].libraryName))
					return implementations[i];
			}
			return null;
		}
	}
	
	private static PlatformArchLibSet[] libSets = 
	{
		new PlatformArchLibSet("Windows", "x86", new LibraryImplementation[]{
				new LibraryImplementation("jpathwatch-native", "Windows-x86/jpathwatch-native.dll"),
			},
			null
		),
		new PlatformArchLibSet("Windows", "amd64", new LibraryImplementation[]{
				new LibraryImplementation("jpathwatch-native", "Windows-amd64/jpathwatch-native.dll"),
			},
			null
		),
		new PlatformArchLibSet("FreeBSD", "i386", new LibraryImplementation[]{
				new LibraryImplementation("jpathwatch-native", "FreeBSD-i386/libjpathwatch-native.so"),
			},
			null
		),
		new PlatformArchLibSet("Mac OS X", "x86_64", new LibraryImplementation[]{
				new LibraryImplementation("jpathwatch-native", "GNU-MacOSX/libjpathwatch-native-64.dylib"),
			},
			null
		),
		new PlatformArchLibSet("Mac OS X", "ppc", new LibraryImplementation[]{
				new LibraryImplementation("jpathwatch-native", "GNU-MacOSX/libjpathwatch-native-ppc-32.dylib"),
			},
			null
		),
		new PlatformArchLibSet("Mac OS X", null, new LibraryImplementation[]{
				new LibraryImplementation("jpathwatch-native", "GNU-MacOSX/libjpathwatch-native-32.dylib"),
			},
			null
		),

// no support yet on Solaris
/*
        new PlatformArchLibSet("Solaris", null, new LibraryImplementation[]{
				// rxtxSerial appears twice here so that unpacker unpacks both librxtxSerial*.so files
				new LibraryImplementation("rxtxSerial", "native/sparc-solaris/sparc32-sun-solaris2.8/librxtxSerial.so", 1, 0),
				new LibraryImplementation("rxtxSerial", "native/sparc-solaris/sparc32-sun-solaris2.8/librxtxSerial-2.1-7.so", 1, 0),
			},
			new String[]{"chmod", "u+x", "{library-file}"}
		),
 */

		new PlatformArchLibSet("Linux", "i386", new LibraryImplementation[]{
				new LibraryImplementation("jpathwatch-native", "GNU-Linux-x86/libjpathwatch-native.so"),
			},
			null

		),
		new PlatformArchLibSet("Linux", "amd64", new LibraryImplementation[]{
				new LibraryImplementation("jpathwatch-native", "GNU-Linux-x86_64/libjpathwatch-native.so"),
			},
			null
		),
		new PlatformArchLibSet("Linux", "x86_64", new LibraryImplementation[]{
				new LibraryImplementation("jpathwatch-native", "GNU-Linux-x86_64/libjpathwatch-native.so"),
			},
			null
		),
		// not sure if 'IA64' is right as architecture name
/*		new PlatformArchLibSet("Linux", "IA64", new LibraryImplementation[]{
				new LibraryImplementation("rxtxSerial", "native/Linux/ia64-unkown-linux-gnu/librxtxSerial.so", 1, 0),
			},
			null
		),
 */
	};
	
	private static Set<String> loadedLibraries = new HashSet<String>();
	private static PlatformArchLibSet libSet = findPlatformArchLibSet();
	private static String PRODUCTNAME = "jpathwatch";
	private static int VERSION_MAJOR=0;
	private static int VERSION_MINOR=92;

	private static String stripPathFromResourceName(String s)
	{
		int slashPos = s.lastIndexOf('/');
		if(slashPos == -1)
			return s;
		return s.substring(slashPos+1);
	}
	
	private static PlatformArchLibSet findPlatformArchLibSet()
	{
		String osName = System.getProperty("os.name");
		String archName = System.getProperty("os.arch");
		for(int i=0; i<libSets.length; ++i)
		{
			PlatformArchLibSet libSet = libSets[i];
			int osNameTruncatedSize = java.lang.Math.min(osName.length(), libSet.osName.length());
			String osNameTruncated = osName.substring(0, osNameTruncatedSize);
			if(osNameTruncated.equals(libSet.osName))
			{
				if(libSet.archName == null || libSet.archName.equals(archName))
					return libSet;
			}
		}
		return null;
	}
	
	public static synchronized void loadLibrary(String name)
	{
		if(loadedLibraries.contains(name))
			return;

		// find implementation for given library name
		LibraryImplementation libImpl = null;
		if(libSet != null)
			libImpl = libSet.findImplementation(name);

		// if there is none, try to load library the 'normal' way
		if(libImpl==null){
			loadDefaultLibrary(name);
			return;
		}

		String prefix = PRODUCTNAME + "-nativelib-v-" + VERSION_MAJOR + '-' + VERSION_MINOR + '-' ;
		String suffix = stripPathFromResourceName(libImpl.libraryResource);

		boolean loaded = false;

		try {
			// try default (shared) library name first
			File parentDir = new File(System.getProperty("java.io.tmpdir"));
			extractAndLoadLibrary(libImpl, new File(parentDir, prefix + suffix));
			loaded = true;
		} catch(Throwable t) {
			t.printStackTrace();
		}

		if(!loaded){
			try {
				// if something went wrong, try temporary file
				File tmpLibFile = File.createTempFile(prefix, suffix);
				extractAndLoadLibrary(libImpl, tmpLibFile);
				loaded = true;
			} catch(Throwable t){
				t.printStackTrace();
			}
		}

		// if all else fails, try System.loadLibrary();
		if(!loaded) {
			try {
				loadDefaultLibrary(name);
				loaded = true;
			}catch(Throwable t){
				t.printStackTrace();
			}
		}

		if(loaded)
			loadedLibraries.add(name);
	}

	static void extractAndLoadLibrary(LibraryImplementation libImpl, File libraryFile){
		InputStream is = null;
//
		RandomAccessFile readRaf = null;
		RandomAccessFile writeRaf = null;
		FileLock writeLock = null;
		FileLock readLock = null;

		try {

			byte[] nativeLibraryBuffer = null;
			{
				String resourceName = libImpl.libraryResource;
				ClassLoader classLoader = NativeLibLoader.class.getClassLoader();
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				try {
					is = classLoader.getResourceAsStream(resourceName);
					byte[] buffer = new byte[8192];
					int nread = 0;
					while(nread != -1){
						bos.write(buffer, 0, nread);
						nread = is.read(buffer);
					}
				} finally {
					if(is != null)
						is.close();
				}

				nativeLibraryBuffer = bos.toByteArray();
			}

			try {
				// attempt to acquire read lock
				readRaf = new RandomAccessFile(libraryFile, "r");
				readLock = readRaf.getChannel().lock(0, Long.MAX_VALUE, true);
			}catch(FileNotFoundException fnfx){
			}

			// Verify that library file on disk has integrity by checking
			// if it's the same as the one we have in the JAR file.
			// If not, we'll let go of the readLock so that the code below
			// can attempt to write a new library file
			if(readLock != null){
				boolean hasIntegrity = true;

				int libraryOnDiskSize = 0;
				byte[] libraryOnDiskBuffer = new byte[(int)readRaf.length()];
				while(libraryOnDiskSize < libraryOnDiskBuffer.length){
					int n = readRaf.read(libraryOnDiskBuffer, libraryOnDiskSize, libraryOnDiskBuffer.length-libraryOnDiskSize);
					if(n == -1)
					{
						// library files have different sizes
						hasIntegrity = false;
						break;
					}

					libraryOnDiskSize += n;
				}
				if(hasIntegrity && libraryOnDiskSize != nativeLibraryBuffer.length)
				{
					// library on disk has different size than library in JAR file
					hasIntegrity = false;
				}
				if(hasIntegrity && !Arrays.equals(nativeLibraryBuffer, libraryOnDiskBuffer))
				{
					// library found on disk is not the same as library in JAR file
					hasIntegrity = false;
				}

				if(!hasIntegrity){
					readLock.release();
					readLock = null;
					readRaf.close();
					readRaf = null;
				}
			}


			if(readLock == null){
				writeRaf = new RandomAccessFile(libraryFile, "rw");
				writeLock = writeRaf.getChannel().lock(0, Long.MAX_VALUE, false);

				// write native library to disk and truncate file to length of library
				writeRaf.write(nativeLibraryBuffer);
				writeRaf.setLength(nativeLibraryBuffer.length);

				// run custom OS specific command on library
				if(libSet.command != null){
					String[] commandArray = new String[libSet.command.length];
					for(int n=0; n<libSet.command.length; ++n){
						String actualCommand = libSet.command[n].replaceAll("\\{library-file\\}", libraryFile.getAbsolutePath());
						commandArray[n] = actualCommand;
					}
					Process p = Runtime.getRuntime().exec(commandArray, null, libraryFile.getAbsoluteFile().getParentFile());
					for(;;){
						try {
							p.waitFor();
						} catch (InterruptedException ex) {
							continue;
						}
						break;
					}
				}
				
				writeLock.release();
				writeLock = null;
				writeRaf.close();
				writeRaf = null;

				libraryFile.deleteOnExit();
			}

			if(readLock == null){
				readRaf = new RandomAccessFile(libraryFile, "r");
				readLock = readRaf.getChannel().lock(0, Long.MAX_VALUE, true);
			}

			String libpath = libraryFile.getAbsolutePath();
			Logger.getLogger(NativeLibLoader.class.getName()).log(Level.FINE, "loading library from: " + libpath);
			System.load(libpath);

			readLock.release();
			readLock = null;
			readRaf.close();
			readRaf = null;

		}catch(IOException iox){
			iox.printStackTrace();
		} finally {
			try{
 				if(writeLock != null)
					writeLock.release();
				if(writeRaf != null)
					writeRaf.close();
				if(readRaf != null)
					readRaf.close();
				if(readLock != null)
					readLock.release();
			}catch(IOException iox) {}
			try{if(is!=null)is.close();}catch(IOException iox) {}
		}
	}
}
