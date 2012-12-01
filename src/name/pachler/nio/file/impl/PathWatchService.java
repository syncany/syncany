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

import name.pachler.nio.file.Path;
import java.io.IOException;
import name.pachler.nio.file.StandardWatchEventKind;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchService;
import name.pachler.nio.file.ext.ExtendedWatchEventKind;
import name.pachler.nio.file.ext.ExtendedWatchEventModifier;

/**
 *
 * @author count
 */
public abstract class PathWatchService extends WatchService{
	public abstract PathWatchKey register(Path path, WatchEvent.Kind<?>[] kinds, WatchEvent.Modifier[] modifiers) throws IOException;

	abstract void cancel(PathWatchKey pathWatchKey);

	abstract boolean reset(PathWatchKey patchWatchKey);

	protected static final int FLAG_WATCH_SUBTREE            = 0x1000;
	protected static final int FLAG_ACCURATE                 = 0x2000;
	protected static final int FLAG_FILTER_ENTRY_RENAME_FROM = 0x0002;
	protected static final int FLAG_FILTER_ENTRY_RENAME_TO   = 0x0004;
	protected static final int FLAG_FILTER_ENTRY_CREATE      = 0x0008;
	protected static final int FLAG_FILTER_ENTRY_DELETE      = 0x0010;
	protected static final int FLAG_FILTER_ENTRY_MODIFY      = 0x0020;
	protected static final int FLAG_FILTER_KEY_INVALID       = 0x0040;

	protected int makeFlagMask(WatchEvent.Kind<?>[] kinds, WatchEvent.Modifier[] modifiers){
		int flags = 0;
		for(WatchEvent.Kind<?> kind : kinds)
		{
			if(kind == StandardWatchEventKind.ENTRY_CREATE)
				flags |= FLAG_FILTER_ENTRY_CREATE;
			else if(kind == StandardWatchEventKind.ENTRY_DELETE)
				flags |= FLAG_FILTER_ENTRY_DELETE;
			else if(kind == StandardWatchEventKind.ENTRY_MODIFY)
				flags |= FLAG_FILTER_ENTRY_MODIFY;
			else if(kind == ExtendedWatchEventKind.ENTRY_RENAME_FROM)
				flags |= FLAG_FILTER_ENTRY_RENAME_FROM;
			else if(kind == ExtendedWatchEventKind.ENTRY_RENAME_TO)
				flags |= FLAG_FILTER_ENTRY_RENAME_TO;
			else if(kind == ExtendedWatchEventKind.KEY_INVALID)
				flags |= FLAG_FILTER_KEY_INVALID;
			else if(kind != StandardWatchEventKind.OVERFLOW)
				throw new UnsupportedOperationException("unsupported event kind " + kind.name());
		}
        
		// check that KEY_INVALID is not the only event kind specified
		if((flags & FLAG_FILTER_KEY_INVALID) == flags)
		{
			// this check is mainly there for the Linux implementation, since
			// inotify_add_watch() cannot accept IN_IGNORE as value for the flag
			// mask - but not specifying the flag mask makes it 0 in this case,
			// which is also an invalid argument for inotify_add_watch().
			// So we simply won't allow it (and we can, it's jpathwatch specific
			// event kind)
			throw new IllegalArgumentException("ExtendedWatchEventKind.KEY_INVALID cannot be the only event kind specified");
		}
		
		for(WatchEvent.Modifier<?> m : modifiers){
			if(m == ExtendedWatchEventModifier.FILE_TREE)
				flags |= FLAG_WATCH_SUBTREE;
			else if(m == ExtendedWatchEventModifier.ACCURATE)
				flags |= FLAG_ACCURATE;
			else
				throw new UnsupportedOperationException("unsupported modifier " + m.name());
		}
                
		return flags;
	}

	protected PathImpl checkAndCastToPathImpl(Path path) throws IOException {
		PathImpl pathImpl;
		try {
			pathImpl = (PathImpl) path;
		} catch (ClassCastException ccx) {
			throw new IllegalArgumentException("the provided Path was not created by the newPath factory method of name.pachler.nio.file.ext.Bootstrapper");
		}
		if (!((PathImpl) pathImpl).getFile().isDirectory()) {
			throw new IOException("path " + pathImpl.toString() + " is not a directory"); // JDK 1.7 throws NotDirectoryException in this case, but for now we'll just throw an IOException
		}
		return pathImpl;
	}
}
