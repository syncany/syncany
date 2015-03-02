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
package org.syncany.tests.integration.scenarios.framework;

import java.util.HashMap;
import java.util.Map;

import org.syncany.tests.util.TestClient;

public class ClientActions {
	public static final Map<String, Object> state = new HashMap<String, Object>();
	
	public static void run(TestClient client, Executable runBefore, AbstractClientAction op, Executable runAfter) throws Exception {
		AbstractClientAction[] ops = new AbstractClientAction[] { op };		
		run(client, runBefore, ops, runAfter);
	}
	
	public static void run(TestClient client, Executable runBefore, AbstractClientAction[] ops, Executable runAfter) throws Exception {
		ops = initOps(client, ops);
		run(runBefore, ops, runAfter);
	}
	
	private static void run(Executable runBefore, AbstractClientAction[] ops, Executable runAfter) throws Exception {
		for (AbstractClientAction op : ops) {
			if (runBefore != null) {
				runBefore.execute();
			}
			
			op.execute();
			
			if (runAfter != null) {
				runAfter.execute();
			}
		}
	}
	
	private static AbstractClientAction[] initOps(TestClient client, AbstractClientAction[] ops) {
		for (AbstractClientAction op : ops) {
			op.client = client;
			op.state = state;
		}
		
		return ops;
	}	
}
