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
public abstract class BSD extends Unix {

	public static final short EV_ADD;
	public static final short EV_ENABLE;
	public static final short EV_DISABLE;
	public static final short EV_DELETE;
	public static final short EV_ONESHOT;
	public static final short EV_CLEAR;
	public static final short EV_EOF;
	public static final short EV_ERROR;

	public static final short EVFILT_VNODE;
	public static final short EVFILT_PROC;

	// for EVFILT_VNODE
	public static final int NOTE_DELETE;
	public static final int NOTE_WRITE;
	public static final int NOTE_EXTEND;
	public static final int NOTE_ATTRIB;
	public static final int NOTE_LINK;
	public static final int NOTE_RENAME;
	public static final int NOTE_REVOKE;

	// for EVFILT_PROC
	public static final int NOTE_EXIT;
	public static final int NOTE_FORK;
	public static final int NOTE_EXEC;
	public static final int NOTE_TRACK;
	public static final int NOTE_TRACKERR;

	// for EVFILT_NETDEV
        // OSX doesn't have it :(
	//public static final int NOTE_LINKUP;
	//public static final int NOTE_LINKDOWN;
	//public static final int NOTE_LINKINV;

	static
	{
		NativeLibLoader.loadLibrary("jpathwatch-native");

		EV_ADD = (short)getIntDefine("EV_ADD");
		EV_ENABLE = (short)getIntDefine("EV_ENABLE");
		EV_DISABLE = (short)getIntDefine("EV_DISABLE");
		EV_DELETE = (short)getIntDefine("EV_DELETE");
		EV_ONESHOT = (short)getIntDefine("EV_ONESHOT");
		EV_CLEAR = (short)getIntDefine("EV_CLEAR");
		EV_EOF = (short)getIntDefine("EV_EOF");
		EV_ERROR = (short)getIntDefine("EV_ERROR");

		EVFILT_VNODE = (short)getIntDefine("EVFILT_VNODE");
		EVFILT_PROC = (short)getIntDefine("EVFILT_PROC");

		NOTE_DELETE = getIntDefine("NOTE_DELETE");
		NOTE_WRITE = getIntDefine("NOTE_WRITE");
		NOTE_EXTEND = getIntDefine("NOTE_EXTEND");
		NOTE_ATTRIB = getIntDefine("NOTE_ATTRIB");
		NOTE_LINK = getIntDefine("NOTE_LINK");
		NOTE_RENAME = getIntDefine("NOTE_RENAME");
		NOTE_REVOKE = getIntDefine("NOTE_REVOKE");

		// for EVFILT_PROC
		NOTE_EXIT = getIntDefine("NOTE_EXIT");
		NOTE_FORK = getIntDefine("NOTE_FORK");
		NOTE_EXEC = getIntDefine("NOTE_EXEC");
		NOTE_TRACK = getIntDefine("NOTE_TRACK");
		NOTE_TRACKERR = getIntDefine("NOTE_TRACKERR");

		// for EVFILT_NETDEV
                // OSX doesn't have it :(
		//NOTE_LINKUP = getIntDefine("NOTE_LINKUP");
		//NOTE_LINKDOWN = getIntDefine("NOTE_LINKDOWN");
		//NOTE_LINKINV = getIntDefine("NOTE_LINKINV");
	}

	public static native int kqueue();

	/**
	 *
<pre>
struct kevent {
	 uintptr_t ident;        // identifier for this event
	 short     filter;       // filter for event
	 u_short   flags;        // action flags for kqueue
	 u_int     fflags;       // filter flag value
	 intptr_t  data;         // filter data value
	 void      *udata;       // opaque user data identifier
};
</pre>
	 */
	public static class kevent{
		static class IntStringMapping{
			int i;
			String s;
			IntStringMapping(int i, String s){
				this.i = i;
				this.s = s;
			}
		}

		IntStringMapping [] flagsMapping = {
			new IntStringMapping(BSD.EV_ADD, "EV_ADD"),
			new IntStringMapping(BSD.EV_ENABLE, "EV_ENABLE"),
			new IntStringMapping(BSD.EV_DISABLE, "EV_DISABLE"),
			new IntStringMapping(BSD.EV_DELETE, "EV_DELETE"),
			new IntStringMapping(BSD.EV_CLEAR, "EV_CLEAR"),
			new IntStringMapping(BSD.EV_ONESHOT, "EV_ONESHOT"),
			new IntStringMapping(BSD.EV_EOF, "EV_EOF"),
			new IntStringMapping(BSD.EV_ERROR, "EV_ERROR"),
		};

		IntStringMapping [] vnodeNoteMapping = {
			new IntStringMapping(BSD.NOTE_DELETE, "NOTE_DELETE"),
			new IntStringMapping(BSD.NOTE_WRITE, "NOTE_WRITE"),
			new IntStringMapping(BSD.NOTE_EXTEND, "NOTE_EXTEND"),
			new IntStringMapping(BSD.NOTE_ATTRIB, "NOTE_ATTRIB"),
			new IntStringMapping(BSD.NOTE_LINK, "NOTE_LINK"),
			new IntStringMapping(BSD.NOTE_RENAME, "NOTE_RENAME"),
			new IntStringMapping(BSD.NOTE_REVOKE, "NOTE_REVOKE"),
		};
		IntStringMapping [] procNoteMapping = {
			new IntStringMapping(BSD.NOTE_EXIT, "NOTE_EXIT"),
			new IntStringMapping(BSD.NOTE_FORK, "NOTE_FORK"),
			new IntStringMapping(BSD.NOTE_EXEC, "NOTE_EXEC"),
			new IntStringMapping(BSD.NOTE_TRACK, "NOTE_TRACK"),
			new IntStringMapping(BSD.NOTE_TRACKERR, "NOTE_TRACKERR"),
		};
/*		IntStringMapping [] netdevNoteMapping = {
			new IntStringMapping(BSD.NOTE_LINKUP, "NOTE_LINKUP"),
			new IntStringMapping(BSD.NOTE_LINKDOWN, "NOTE_LINKDOWN"),
			new IntStringMapping(BSD.NOTE_LINKINV, "NOTE_LINKINV"),
		};
*/
		private long peer = createPeer();

		private static native void initNative();
		private static native long createPeer();
		private static native void destroyPeer(long peer);

		static
		{
			initNative();
		}

		@Override
		protected void finalize() throws Throwable {
			try {
				destroyPeer(peer);
				peer = 0;
			}finally{
				super.finalize();
			}
		}


		public native long get_ident();
		public native void set_ident(long ident);

		public native short get_filter();
		public native void set_filter(short filter);

		public native short get_flags();
		public native void set_flags(short flags);

		public native int get_fflags();
		public native void set_fflags(int fflags);

		public native long get_data();
		public native void set_data(long data);

		public native Object get_udata();
		public native void set_udata(Object udata);

		private String bitmaskToString(int value, IntStringMapping[] mapping){
			String bitmaskString = "";
			for(int i=0; i<mapping.length; ++i){
				if((value & mapping[i].i) != 0){
					if(bitmaskString.length()!=0)
						bitmaskString += '|';
					bitmaskString += mapping[i].s;
				}
			}
			return bitmaskString;
		}

		@Override
		public String toString(){
			String flags = bitmaskToString(get_flags(), flagsMapping);
			String filter = "?";
			String fflags = "?";
			if(get_filter()==BSD.EVFILT_VNODE)
			{
				fflags = bitmaskToString(get_fflags(), vnodeNoteMapping);
				filter = "EVFILT_VNODE";
			}
			else if(get_filter()==BSD.EVFILT_PROC)
			{
				fflags = bitmaskToString(get_fflags(), procNoteMapping);
				filter = "EVFILT_PROC";
			}
			return "{ident="+get_ident()+";filter="+filter+";flags="+flags+";fflags="+fflags+";data="+get_data()+";udata="+get_udata()+"}";
		}
	};

	/**
	 *
	 * @param kq	the kqueue to read events from/change events in. Obtained with kqueue().
	 * @param changelist	an array of kevent instances that indicate events to add/remove or modify. If
	 *	null, no changes will be applied to the kqueue.
	 * @param eventlist	an array that the function will fill with events that occurred. If there are
	 *	more events available than there is space in this array, the array will be filled. A
	 *	subsequent call to kevent() will yield the remaining events. Use select() on the kqueue
	 *	file descriptor to check if there are events pending. Non-null array elements will be
	 *	kept; their values will be overwritten. The function will assign new
	 *  kqueue objects to null array elements. Note that providing an eventlist
	 *	array pre-filled with kevent objects will improve performance. If
	 *	null is specified, no events will be read, and the function returns
	 *	immediately.
	 * @param timeout	timeout value. If null, the function will wait
	 * indefinitely until events are available (unless an error occurred).
	 * Otherwise, the function will wait for the specified timeout for events
	 * (which may be zero), again, unless an error occurs. Note that the function
	 * will return immediately if eventlist is null or it's length is zero,
	 * regardless of the timeout specified.
	 * @return	the function returns the number of events written to eventlist.
	 *	Zero may be returned if the timeout passed before events could be read.
	 *	-1 is returned if an error occurred; call errno() to get the error code.
	 */
	public static native int kevent(int kq, kevent[] changelist, kevent[] eventlist, timespec timeout);
}
