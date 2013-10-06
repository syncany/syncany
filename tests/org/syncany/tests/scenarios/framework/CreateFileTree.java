package org.syncany.tests.scenarios.framework;

public class CreateFileTree extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		log(this, "Creating file tree");

		client.createNewFiles();
		
		for (int i=0; i<10; i++) {
			client.createNewFolder("folder"+i);
			client.createNewFiles("folder"+i);
			
			client.createNewFolder("folder"+i+"/sub1");
			client.createNewFiles("folder"+i+"/sub1");
			
			client.createNewFolder("folder"+i+"/sub2");
			client.createNewFiles("folder"+i+"/sub2");
		}
	}		
}
