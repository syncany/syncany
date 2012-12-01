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
public class Solaris extends Unix {
	// event source types
	public static final int PORT_SOURCE_AIO;	// currently unused
	public static final int PORT_SOURCE_FD;
	public static final int PORT_SOURCE_MQ;	// currently unused
	public static final int PORT_SOURCE_TIMER;	// currently unused
	public static final int PORT_SOURCE_USER;	// currently unused
	public static final int PORT_SOURCE_ALERT;	// currently unused
	public static final int PORT_SOURCE_FILE;

	static
	{
		NativeLibLoader.loadLibrary("jpathwatch-native");

		PORT_SOURCE_AIO = getIntDefine("PORT_SOURCE_AIO");	// currently unused
		PORT_SOURCE_FD = getIntDefine("PORT_SOURCE_FD");
		PORT_SOURCE_MQ = getIntDefine("PORT_SOURCE_MQ");	// currently unused
		PORT_SOURCE_TIMER = getIntDefine("PORT_SOURCE_TIMER");	// currently unused
		PORT_SOURCE_USER = getIntDefine("PORT_SOURCE_USER");	// currently unused
		PORT_SOURCE_ALERT = getIntDefine("PORT_SOURCE_ALERT");	// currently unused
		PORT_SOURCE_FILE = getIntDefine("PORT_SOURCE_FILE");
	}

	/**
	 * http://docs.sun.com/app/docs/doc/819-2243/port-create-3
	 * @return
	 */
	public static native int port_create();

	/**
	 * http://docs.sun.com/app/docs/doc/819-2243/port-associate-3c
	 */
	public static native int port_associate(int port, int source, Object object, int events, Object user);

	/**
	 * http://docs.sun.com/app/docs/doc/819-2243/port-associate-3c
	 * @param port
	 * @param source
	 * @param object
	 * @return
	 */
	public static native int port_dissociate(int port, int source, Object object);

	public static class file_obj{
		int fo_atime;
		int fo_ctime;
		int fo_mtime;
		String fo_filename;
	}
}
