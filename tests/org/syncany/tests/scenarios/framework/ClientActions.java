package org.syncany.tests.scenarios.framework;

import java.util.HashMap;
import java.util.Map;

import org.syncany.tests.util.TestClient;

public class ClientActions {
	public static void runOps(TestClient client, Executable runBefore, AbstractClientAction[] ops, Executable runAfter) throws Exception {
		ops = initOps(client, ops);
		runOps(runBefore, ops, runAfter);
	}
	
	private static void runOps(Executable runBefore, AbstractClientAction[] ops, Executable runAfter) throws Exception {
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
		Map<String, Object> state = new HashMap<String, Object>();
		
		for (AbstractClientAction op : ops) {
			op.client = client;
			op.state = state;
		}
		
		return ops;
	}	
}
