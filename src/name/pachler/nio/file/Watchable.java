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

import java.io.IOException;

/**
 * A watchable is an object that can be registered with a {@link WatchService}
 * to be monitored for changes via it's {@link #register register} methods. 
 * The watchable only defines an interface, use {@link Path} to actually
 * watch directories.
 * @author count
 */
public interface Watchable {
	/**
	 * Registers the file system path (a directory) with the given WatchService
	 * and provides a WatchKey as a handle for that registration.
	 * Equivalent to calling
	 * <code>register(watcher, events, new WatchEvent.Modifier[0]);</code>
	 * @see Path#register(name.pachler.nio.file.WatchService, name.pachler.nio.file.WatchEvent.Kind<?>[], name.pachler.nio.file.WatchEvent.Modifier[])
	 */
	public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException;

	/**
	 * Registers the file system path (a directory) with the given
	 * {@link WatchService}and provides a WatchKey as a handle for that registration.
	 * The events and modifier lists determine the events that the
	 * {@link WatchService} will report.<br/>
	 * If a path instance is passed in that represents the same file system
	 * object that has been specified in a previous call to this method with the
	 * same {@link WatchService}, the same {@link WatchKey} will be returned. In this case,
	 * the {@link WatchKey}'s watch settings are altered to match the new event
	 * and modifier lists. Note that such changes may result in
	 * {@link StandardWatchEventKind#OVERFLOW} events to be reported on some platforms.<br/>
	 * Not all event kinds and modifiers defined in
	 * {@link StandardWatchEventKind}, {@link name.pachler.nio.file.ext.ExtendedWatchEventKind} and
	 * {@link name.pachler.nio.file.ext.ExtendedWatchEventModifier} may be supported on a target platform
	 * (typically though, in this implementation, all event kinds defined in
	 * {@link StandardWatchEventKind} are always supported).
	 * @param watcher	a valid {@link WatchService} instance.
	 * @param events	The events to register for. The event kinds defined in
	 *  {@link StandardWatchEventKind}and {@link name.pachler.nio.file.ext.ExtendedWatchEventKind}
	 *  define valid event kinds that can
	 *	be passed in here. Not that not all event kinds may be supported on
	 *	a given platform, so see the documentation of a specific event kind.
	 * @param modifiers	The event modifiers to use when registering. {@link WatchEvent$Modifier}s
	 *	define special behaviour that's expected from the {@link WatchService}. Note
	 *	that some event modifiers may not be supported on a given platform;
	 *	see the specific modifier for details.
	 * @return	a new {@link WatchKey} that represents the registration.
	 * @throws java.lang.UnsupportedOperationException	If unsupported event kinds have been provided
	 * @throws java.lang.IllegalArgumentException	If an unsupported combination of event kinds has been provided
	 * @throws ClosedWatchServiceException	If the given WatchService is already closed.
	 * @throws NotDirectoryException	If a directory is required for the
	 *	given registration options (which is typically the case) and the path
	 *	instance does not reference a directory
	 * @throws java.io.IOException	for general I/O errors
	 * @see StandardWatchEventKind
	 * @see name.pachler.nio.file.ext.ExtendedWatchEventKind
	 * @see name.pachler.nio.file.ext.ExtendedWatchEventModifier
	 */
	public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException;
}
