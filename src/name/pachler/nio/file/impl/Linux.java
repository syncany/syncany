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
public class Linux extends Unix {
	/* Supported events suitable for MASK parameter of INOTIFY_ADD_WATCH.  */
	static final int IN_ACCESS        = 0x00000001;	/* File was accessed (read) (*)  */
	static final int IN_MODIFY        = 0x00000002;	/* File was modified.  */
	static final int IN_ATTRIB        = 0x00000004;	/* Metadata  changed, e.g., permissions, timestamps, extended attributes, link count (since
								 Linux 2.6.25), UID, GID, etc. (*).  */
	static final int IN_CLOSE_WRITE   = 0x00000008;	/* Writtable file was closed.  */
	static final int IN_CLOSE_NOWRITE = 0x00000010;	/* Unwrittable file closed.  */
	static final int IN_CLOSE         = (IN_CLOSE_WRITE | IN_CLOSE_NOWRITE); /* Close.  */
	static final int IN_OPEN          = 0x00000020;	/* File was opened.  */
	static final int IN_MOVED_FROM    = 0x00000040;	/* File was moved from X.  */
	static final int IN_MOVED_TO      = 0x00000080;	/* File was moved to Y.  */
	static final int IN_MOVE          = (IN_MOVED_FROM | IN_MOVED_TO); /* Moves.  */
	static final int IN_CREATE        = 0x00000100;	/* Subfile was created.  */
	static final int IN_DELETE        = 0x00000200;	/* Subfile was deleted.  */
	static final int IN_DELETE_SELF   = 0x00000400;	/* Self was deleted.  */
	static final int IN_MOVE_SELF     = 0x00000800;	/* Self was moved.  */

	/* Events sent by the kernel.  */
	static final int IN_UNMOUNT       = 0x00002000;	/* Backing fs was unmounted.  */
	static final int IN_Q_OVERFLOW    = 0x00004000;	/* Event queued overflowed.  */
	static final int IN_IGNORED       = 0x00008000;	/* File was ignored.  */

	/* Special flags.  */
	static final int IN_ONLYDIR       = 0x01000000;	/* Only watch the path if it is a
											   directory.  */
	static final int IN_DONT_FOLLOW   = 0x02000000;	/* Do not follow a sym link.  */
	static final int IN_MASK_ADD      = 0x20000000;	/* Add to the mask of an already
											   existing watch.  */
	static final int IN_ISDIR         = 0x40000000;	/* Event occurred against dir.  */
	static final int IN_ONESHOT       = 0x80000000;	/* Only send event once.  */

	/* All events which a program can wait on.  */
	static final int IN_ALL_EVENTS    = (IN_ACCESS | IN_MODIFY | IN_ATTRIB | IN_CLOSE_WRITE
							  | IN_CLOSE_NOWRITE | IN_OPEN | IN_MOVED_FROM
							  | IN_MOVED_TO | IN_CREATE | IN_DELETE
							  | IN_DELETE_SELF | IN_MOVE_SELF);

	// these functions are direct mappings to linux kernel functions
	native static int inotify_init();
	native static int inotify_add_watch(int fd, String pathname, int mask);
	native static int inotify_rm_watch(int fd, int wd);


	// some file system identifiers (from linux/magic.h). This list is by no
	// way complete.
	static final int NFS_SUPER_MAGIC = 0x6969;
	static final int SMB_SUPER_MAGIC = 0x517B;

	static class statfs {
		void set(long type, long bsize, long blocks, long bfree, long bavail, long files, long ffree, int namelen){
			f_type = type;
			f_bsize = bsize;
			f_blocks = blocks;
			f_bfree = bfree;
			f_bavail = bavail;
			f_files = files;
			f_ffree = ffree;
			f_namelen = namelen;
		}

		long f_type;
		long f_bsize;
		long f_blocks;
		long f_bfree;
		long f_bavail;
		long f_files;
		long f_ffree;
		int f_namelen;

	}

	native static int statfs(String path, statfs stfs);

}
