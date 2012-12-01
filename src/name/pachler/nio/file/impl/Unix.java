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

/**
 *
 * @author count
 */
public abstract class Unix {
	/**
	 * This function retreives a value that is defined in
	 * the operating system's headers. Because values like
	 * open()'s O_CREAT flag, errno values like EBADF and other values
	 * might be defined differently on different OS flavours, it is not
	 * sufficient to simply hard-code them in the OS wrapper class.
	 * Therefore, OS Wrapper classes use a lookup mechanism to initialize
	 * their static final variables. So for instance, the O_CREAT flag for
	 * Unix's open() is defined as static final int O_CREAT member variable,
	 * initialized to the return value of the getIntDefine("O_CREAT") function
	 * call. If a defined name is given that is not known to the native
	 * implementation, an IllegalArgumentException is thrown
	 * @param definedName	name of the define to look up
	 * @return	the integer value associated with the given definedName.
	 */
	protected static native int getIntDefine(String definedName);

	// errno codes
	static final int EINTR;
	static final int EINVAL;
	static final int EBADF;
	static final int ENOENT;

	// bits for open()
	static final int O_RDONLY;
	static final int O_WRONLY;
	static final int O_RDWR;
	static final int O_APPEND;
	static final int O_CREAT;
	static final int O_EXCL;
	static final int O_NOCTTY;
	static final int O_NONBLOCK;
	static final int O_SYNC;
	static final int O_TRUNC;

	// bits for file mode (for stat() and friends). Note that
	// the S_ISREG(), S_ISDIR() etc. functions can be used on the mode bits
	// to determine additional traits of the file
	static final int S_ISUID;	// set UID bit
	static final int S_ISGID;	// set-group-ID bit
	static final int S_IRWXU;	// mask for file owner permissions
	static final int S_IRUSR;	// owner has read permission
	static final int S_IWUSR;	// owner has write permission
	static final int S_IXUSR;	// owner has execute permission
	static final int S_IRWXG;	// mask for group permissions
	static final int S_IRGRP;	// group has read permission
	static final int S_IWGRP;	// group has write permission
	static final int S_IXGRP;	// group has execute permission
	static final int S_IRWXO;	// mask for permissions for others (not in group)
	static final int S_IROTH;	// others have read permission
	static final int S_IWOTH;	// others have write permission
	static final int S_IXOTH;	// others have execute permission

	static
	{
		NativeLibLoader.loadLibrary("jpathwatch-native");
		EINTR = getIntDefine("EINTR");
		EINVAL = getIntDefine("EINVAL");
		EBADF = getIntDefine("EBADF");
		ENOENT = getIntDefine("ENOENT");

		O_RDONLY = getIntDefine("O_RDONLY");
		O_WRONLY = getIntDefine("O_WRONLY");
		O_RDWR = getIntDefine("O_RDWR");
		O_APPEND = getIntDefine("O_APPEND");
		O_CREAT = getIntDefine("O_CREAT");
		O_EXCL = getIntDefine("O_EXCL");
		O_NOCTTY = getIntDefine("O_NOCTTY");
		O_NONBLOCK = getIntDefine("O_NONBLOCK");
		O_SYNC = getIntDefine("O_SYNC");
		O_TRUNC = getIntDefine("O_TRUNC");

		S_ISUID = getIntDefine("S_ISUID");
		S_ISGID = getIntDefine("S_ISGID");
		S_IRWXU = getIntDefine("S_IRWXU");
		S_IRUSR = getIntDefine("S_IRUSR");
		S_IWUSR = getIntDefine("S_IWUSR");
		S_IXUSR = getIntDefine("S_IXUSR");
		S_IRWXG = getIntDefine("S_IRWXG");
		S_IRGRP = getIntDefine("S_IRGRP");
		S_IWGRP = getIntDefine("S_IWGRP");
		S_IXGRP = getIntDefine("S_IXGRP");
		S_IRWXO = getIntDefine("S_IRWXO");
		S_IROTH = getIntDefine("S_IROTH");
		S_IWOTH = getIntDefine("S_IWOTH");
		S_IXOTH = getIntDefine("S_IXOTH");
	}
	public static class timespec {
		static {
			initNative();
		}
		private static native void initNative();

		private static native long createPeer();
		private static native void destroyPeer(long peer);

		private long peer = createPeer();

		@Override
		protected void finalize() throws Throwable {
			try{
				destroyPeer(peer);
				peer = 0;
			}finally{
				super.finalize();
			}
		}

		public native long get_tv_sec();
		public native long get_tv_nsec();

		public native void set_tv_sec(long tv_sec);
		public native void set_tv_nsec(long tv_nsec);

		public native void set(long tv_sec, long tv_nsec);
	}

	static class stat {
		
		int     st_dev;     /* ID of device containing file */
		long     st_ino;     /* inode number */
		int    st_mode;    /* protection */
		int   st_nlink;   /* number of hard links */
		int     st_uid;     /* user ID of owner */
		int     st_gid;     /* group ID of owner */
		int     st_rdev;    /* device ID (if special file) */
		long     st_size;    /* total size, in bytes */
		int st_blksize; /* blocksize for file system I/O */
        long	st_blocks;  /* number of 512B blocks allocated */
        long    st_atime;   /* time of last access */
		long    st_mtime;   /* time of last modification */
		long    st_ctime;   /* time of last status change */
   	}

	static native int open(String pathname, int flags, int mode);

	static native int close(int fd);


	/**
	 * <p>Calls Unix' select() native platform function. The three arrays
	 * are translated into file descriptor sets (fd_set) and back. The native
	 * select() function only keeps the bits in the fd_set structures set that
	 * are signalled as having data available (or being able to write to); this
	 * is translated back into the given arrays like this:</p>
	 *
	 * <p>When one or more file descriptors in a provided array are signalled,
	 * they these array elements are left untouched by the function. Elements
	 * with file descriptor numbers that have not been signalled will be set to
	 * -1.</p>
	 *
	 * <p>For example, if the array int[]{5, 2, 8} is passed into select(), and
	 * file descriptors 2 and 8 are signalled, the result is int[]{-1 ,2 ,8}.</p>
	 *
	 * <p>Check the manual page for Unix' select() function for details.</p>
	 *
	 * @param readfds	the set of file descriptors which are observed
	 *	for being readable.
	 * @param writefds	the set of file descriptors which are checked for being
	 *	writable.
	 * @param exceptfds	the set of file descriptors which are checked for being
	 *	'special'
	 * @param timeout	in milliseconds. If 0, select will return immediately.
	 *	If -1, select() will block indefinitely.
	 * @return	the number of file descriptors that were signalled as result of
	 *	this call. If one of the file descriptors in the provided arrays is
	 *	not in the range [0,FD_SETSIZE], the function returns -1 and errno()
	 *	is set to EINVAL. This behaviour is not part of select(); this behaviour
	 *	is specific to the Java wrapper for select().
	 */
	native static int select(int[] readfds, int[] writefds, int[] exceptfds, long timeout);

	// determins the number of bytes available on a given file descriptor.
	// returns <0 on error
	native static int ioctl_FIONREAD(int fd);
	native static int read(int fd, byte[] buffer, int nbytes);
	native static int write(int fd, byte[] buffer, int nbytes);
	native static int pipe(int[] pipefds);
	native static int errno();
	native static String strerror(int error);


	native static boolean S_ISREG(int mode);	// is it a regular file?
	native static boolean S_ISDIR(int mode);	// directory?
	native static boolean S_ISCHR(int mode);	// character device?
	native static boolean S_ISBLK(int mode);	// block device?
	native static boolean S_ISFIFO(int mode);	// FIFO (named pipe)?
	native static boolean S_ISLNK(int mode);	// symbolic link? (Not in POSIX.1-1996.)
	native static boolean S_ISSOCK(int mode);	// is it a regular file?

	native static int stat(String path, stat st);
	native static int lstat(String path, stat st);

	native static int symlink(String linkedToPath, String symlinkPath);
}
