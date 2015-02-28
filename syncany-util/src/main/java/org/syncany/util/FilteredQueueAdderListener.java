/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.util;

import java.util.Queue;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author jesse
 *
 */
public abstract class FilteredQueueAdderListener<T> extends QueueAdderListener<T> implements FilteredListener<T> {

	/**
	 * @param queue
	 */
	public FilteredQueueAdderListener(Queue<T> queue) {
		super(queue);
	}

	@Override
	public void process(T t) {
		if (isValid(t)) {
			super.process(t);
		}
	};

	@Override
	public boolean isValid(T t) {
		throw new NotImplementedException();
	}

}