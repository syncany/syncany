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

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * A service that provides monitoring for {@link Watchable}s, reporting changes
 * on these objects (in the case of jpathwatch, these are {@link Path}
 * instances).</p>
 * To utilise file monitoring, a program needs to acquire a
 * watch service first, like this:<br/>
 * {@code
 * WatchService ws = FileSystems.getDefault().newWatchService();
 * }<p/>
 * A path needs to be constructed for the directory to be watched, by simply
 * using the {@link Paths} class:<br/>
 * {@code
 * // assuming we want to watch the '/tmp' directory on a Unix system.
 * Path path = Paths.get("/tmp");
 * }<p/>
 * We can now register the path with the watch service. In this case, we
 * want to watch for files created under the /tmp directory, so we register
 * using {@link Path#register} with
 * the {@link StandardWatchEventKind#ENTRY_CREATE} event kind and no modifiers,
 * like this:<br/>
 * {@code
 * WatchKey key = path.register(ws, ENTRY_CREATE);
 * }<p/>
 * Note that we receive a {@link WatchKey} now, which represents the registration
 * of our path with the watch service. The key is also used subsequently to
 * retreive events. We'll now wait for an event to occur, like this:<br/>
 * {@code
 * // this will block until an event has occurred, or the WatchService is closed.
 * WatchKey k = ws.take();
 * }
 * @author count
 */
public abstract class WatchService implements Closeable {
protected 	WatchService()
{}
public abstract void close()throws IOException;
public abstract WatchKey poll() throws InterruptedException, ClosedWatchServiceException;
public abstract WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException, ClosedWatchServiceException;
public abstract WatchKey take() throws InterruptedException, ClosedWatchServiceException;
}
