package org.syncany.tests.scenarios.framework;

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
