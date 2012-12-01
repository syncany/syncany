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

import java.util.List;

/**
 * A {@link WatchKey} represents a {@link Watchable}'s registration for events with a
 * {@link WatchService}. It is created on registration and subsequently used to retreive
 * {@link WatchEvent}s.<br/>
 * To stop receiving events from a {@link WatchService} (that is, undoing registration),
 * use the {@link #cancel} method. Note that this will invalidate the {@link WatchKey}, which
 * should then be discarded (as it cannot be reused).
 * @author count
 * @see WatchService#poll
 * @see WatchService#take
 * @see WatchKey#pollEvents
 */
public abstract class WatchKey {
protected 	WatchKey()
{}
/**
 * This cancels the registration with the WatchService that this WatchKey
 * was registered with. This means that no new events will be delivered to this
 * key any more. Events that are pending can still be retreived with
 * pollEvents(), and if the WatchKey is still marked as signalled a call to
 * WatchService's poll() or take() functions will still return it.
 */
public abstract  void 	cancel();

/**
 * @return if this watch key is valid. A watch key is valid if
 * <ul>
 * <li>It has not been canceled</li>
 * <li>The WatchService is not yet closed</li>
 * </ul>
 */
public abstract  boolean 	isValid();

/**
 * Returns the events that have occurred for this WatchKey. Just calling
 * this method will not reset the signalled state of this key; you'll have
 * to call #reset() to indicate to the WatchService that the the client is
 * ready to receive more events and that the key can be re-queued.
 * After the WatchService has determined that events have occurred for a
 * registered Watchable represented by a given WatchKey, it will return that
 * key when the client calls it's WatchService#take() or WatchService#poll()
 * methods.
 * @return	a list of events that have occurred since the last time that
 *	#pollEvents() was called.
 */
public abstract  List<WatchEvent<?>> 	pollEvents();

/**
 * Resets this {@link WatchKey} (marks it as non-signalled) so that it's
 * corresponding {@link WatchService} can report it again via it's
 * {@link WatchService#poll} and {@link WatchService#take} methods.
 *
 * @return <code>true</code> if the key could be reset, <code>false</code>
 * otherwise (typically if the corresponding {@link WatchService} has been closed
 * or if the the key was not signalled).
 */
public abstract  boolean 	reset();
}
