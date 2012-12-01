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

import name.pachler.nio.file.Path;
import name.pachler.nio.file.StandardWatchEventKind;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchService;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.impl.PathWatchEventKind;
import name.pachler.nio.file.impl.VoidWatchEventKind;

/**
 * Provides non-standard watch event kinds that enable more specialized
 * watch features.
 * <h2>File rename tracking</h2>
 * The {@link #ENTRY_RENAME_FROM} and {@link #ENTRY_RENAME_TO} event
 * kinds enable more fine grained file rename tracing. When only the standard
 * event kinds are specified with Path.register(), renaming a file in a
 * watched directory will only yield an {@link StandardWatchEventKind#ENTRY_CREATE} and an
 * {@link StandardWatchEventKind#ENTRY_DELETE}
 * event, which makes it indistinguishable from actual file creation and
 * deletion events.<p/>
 * If {@link #ENTRY_RENAME_FROM} and/or {@link #ENTRY_RENAME_TO} are
 * specified, renamed files will be reported with respective events. Note that
 * if both {@link #ENTRY_RENAME_FROM} and {@link #ENTRY_RENAME_TO}
 * are specified, a {@link #ENTRY_RENAME_FROM} event for a file will
 * be immediately be followed by a {@link #ENTRY_RENAME_TO} event for the
 * same file.<p>
 * @author count
 */
public class ExtendedWatchEventKind {
	/**
	 * Indicates the old file name of a renamed file. The
	 * {@link WatchEvent}'s {@link WatchEvent#context} method will return
	 * a Path that indicates the previous name that the file had.
	 */
	public static WatchEvent.Kind<Path> 	ENTRY_RENAME_FROM = new PathWatchEventKind("ENTRY_RENAME_FROM");
	/**
	 * Indicates the new file name of a renamed file. The
	 * {@link WatchEvent}'s {@link WatchEvent#context} method will return
	 * a Path that indicates the new name of the file.
	 */
	public static WatchEvent.Kind<Path> 	ENTRY_RENAME_TO = new PathWatchEventKind("ENTRY_RENAME_TO");

	/**
	 * Indicates that the given {@link WatchKey} has become invalid. This
	 * can happen for a number of reasons:
	 * <ul>
	 *	<li>The key has been invalidated programmatically by calling
	 *		{@link WatchKey#cancel}.</li>
	 *	<li>The {@link WatchService} has been closed.</li>
	 *	<li>The path that the key has been registered with has become
	 *		unavailable, e.g because it was deleted or the file system on which
	 *		it resides has been unmounted.</li>
	 * </ul>
	 * Note that not all operating systems will invalidate the WatchKey
	 * (and report KEY_INVALID) under the same circumstances. On most platforms,
	 * moving or renaming the directory that the key was registered with will
	 * invalidate it, however, on Windows it will not (only when it is
	 * deleted or unmounted).
	 */
	public static WatchEvent.Kind<Void> 	KEY_INVALID = new VoidWatchEventKind("KEY_INVALID");
}
