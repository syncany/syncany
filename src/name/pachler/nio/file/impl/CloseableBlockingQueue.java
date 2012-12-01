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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import name.pachler.nio.file.ClosedWatchServiceException;

/**
 *
 * @author count
 */
class CloseableBlockingQueue<T>  implements BlockingQueue<T>{
	Queue<T> q = new LinkedList<T>();

	public synchronized boolean add(T e) {
		try {
			put(e);
		} catch (InterruptedException ex) {
			Logger.getLogger(CloseableBlockingQueue.class.getName()).log(Level.SEVERE, null, ex);
		}
		return true;
	}

	public synchronized int drainTo(Collection<? super T> clctn) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized int drainTo(Collection<? super T> clctn, int maxNumElements) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized boolean offer(T e) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized boolean offer(T e, long l, TimeUnit tu) throws InterruptedException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized T poll(long l, TimeUnit tu) throws InterruptedException {
		if(q == null)
			throw new ClosedWatchServiceException();
		T e = q.poll();
		if(e == null)
		{
			if(tu == null)
				wait();
			else
				wait(TimeUnit.MILLISECONDS.convert(l, tu));
			if(q == null)
				throw new ClosedWatchServiceException();
			e = q.poll();
		}
		return e;
	}

	public synchronized void put(T e) throws InterruptedException {
		if(q == null)
			throw new ClosedWatchServiceException();
		boolean success = q.add(e);
		assert(success);
		if(success)
			notify();
	}

	public synchronized int remainingCapacity() {
		return Integer.MAX_VALUE;
	}

	public synchronized T take() throws InterruptedException {
		return poll(-1, null);
	}

	public synchronized void close() {
		q = null;
		notifyAll();
	}

	public synchronized T poll() {
		if(q == null)
			throw new ClosedWatchServiceException();
		return q.poll();
	}

	public synchronized T remove() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized T peek() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized T element() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized int size() {
		return q.size();
	}

	public synchronized boolean isEmpty() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized boolean contains(Object o) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized Iterator<T> iterator() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized Object[] toArray() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized <T> T[] toArray(T[] ts) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized boolean remove(Object o) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized boolean containsAll(Collection<?> clctn) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized boolean addAll(Collection<? extends T> clctn) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized boolean removeAll(Collection<?> clctn) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized boolean retainAll(Collection<?> clctn) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public synchronized void clear() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
