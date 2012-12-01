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

package name.pachler.nio.file;

import name.pachler.nio.file.ext.ExtendedWatchEventModifier;

/**
 * Instances of this class hold the information of a particular change to
 * an element below the watched file system object (e.g. a file modification
 * or a rename). These objects can be retreived from a WatchKey once it has
 * been returned by the WatchService (using the <code>poll()</code> or
 * <code>take()</code> methods).
 * 
 * @author count
 */
public abstract class WatchEvent<T> {
	/**
	 * Instances of this class act as tags to identify different kinds of
	 * events (like file creation or deletion)
	 * @param <T> The context this kind is intended for. In jpathwatch, only
	 * Path and Void are used as context types.
	 */
    public static interface Kind<T>
    {
		/**
		 * @return the name of this modifier
		 */
		String name();
		/**
		 * @return the type of the WatchEvent's context value.
		 */
		Class<T> type();
    }

	/**
	 * A modifier can be specified to {@link Watchable#register register} to
	 * change the way changes to a watchable are reported. jpathwatch
	 * defines a set of modifiers in the {@link ExtendedWatchEventModifier}
	 * class.
	 * @param <T> The context type for the modifier.
	 * @see ExtendedWatchEventModifier
	 */
    public static interface Modifier<T>
    {
		/**
		 * The modifier's name should be used for informative purposes
		 * only (like error reporting).
		 * @return	the modifier's name
		 */
		String name();
    }

    protected WatchEvent()
    {}

	/**
	 * @return the context of this event, which is usually a reference to the
	 * object that has changed. In the case of WatchEvents for Path, the
	 * context will be a <code>Path</code> to the file that this event
	 * refers to, relative to the watched <code>Path</code>
	 */
    public abstract T context();
	/**
	 * The number of times this event occurred, if it is cumulative. It
	 * is not specified how events cumulate, so use this value for informational
	 * purposes only.
	 * @return the number of times this event has occurred, in case events of
	 *	this kind have been aggregated into one WatchEvent instance.
	 */
    public abstract int count();

	/**
	 * @return the kind of event that occurred. This will indicate what
	 * actually happened, for instance, StandardWatchEventKind#ENTRY_CREATE
	 * indicates that a file has been created.
	 */
    public abstract WatchEvent.Kind<T> kind();
}
