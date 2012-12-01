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
import name.pachler.nio.file.impl.PathWatchEventModifier;

/**
 * This class holds non-standard modifiers that can be passed to
 * Path.register(). Note that these might not be supported on all platforms, so
 * specifying them to {@link Path#register register} might cause an
 * {@link java.lang.UnsupportedOperationException UnsupportedOperationException}
 * or an {@link java.lang.IllegalArgumentException IllegalArgumentException}
 * to be thrown.
 * @author count
 */
public class ExtendedWatchEventModifier {

	/**
	 * The {@link #FILE_TREE} modifier makes a WatchKey recursive. Without this modifier,
	 * a file watch is shallow: For a watched directory <code>foo</code>,
	 * WatchEvents are only generated for direct children such as
	 * <code>foo/bar</code> or <code>foo/oof</code>. For a changes to files in a
	 * subdirectory of <code>foo</code>, such as <code>foo/adir/file</code>
	 * will only be reported if the {@link #FILE_TREE} modifier is specified.
	 *
	 * Note that this modifier is only available on the Windows platform. If
	 * specified on other platforms, Path.register() will throw an
	 * UnsupportedOperationException.
	 *
	 */
	public static final WatchEvent.Modifier<Path> FILE_TREE = new PathWatchEventModifier("FILE_TREE");

	/**
	 * The {@link #ACCURATE} modifier indicates that the WatchKey may not miss events
	 * under any circumstances.<p/>
	 * If a {@link name.pachler.nio.file.WatchService WatchService} implementation is based on polling it may miss files
	 * that are created and deleted between two polls of the key (see below how
	 * this can happen). The {@link #ACCURATE} modifier demands that no events may be
	 * missed, however, not all platforms support this. <p/>
	 * Note that even without specifying ACCURATE events are still guaranteed to be
	 * reported as symmetric - if both {@link StandardWatchEventKind#ENTRY_CREATE ENTRY_CREATE}
	 * and {@link StandardWatchEventKind#ENTRY_DELETE ENTRY_DELETE} have been
	 * specified, only whole pairs of creation/deletion can be
	 * missed, not just one of them.<p/>
	 * On some platforms, ACCURATE is implicit as the underlying operating
	 * system guarantees to deliver every event, even without specifying
	 * {@link #ACCURATE} (however, the queue might still overflow, generating
	 * an event of the {@link name.pachler.nio.file.StandardWatchEventKind#OVERFLOW OVERFLOW} kind)<p/>
	 * Consider the following scenario in which an implementation which
	 * does not support ACCURATE can miss events:<br/>
	 * Say, at 0ms, the key polls, and detects no files in a directory. The
	 * key goes to sleep for 200ms. Say, at 50ms, a file is created. Then, at
	 * 100ms, the same file is deleted again. When the WatchKey wakes up after
	 * 200ms, it will see the same as it saw before: An empty directory.
	 */
	public static final WatchEvent.Modifier<Path> ACCURATE = new PathWatchEventModifier("ACCURATE");
}
