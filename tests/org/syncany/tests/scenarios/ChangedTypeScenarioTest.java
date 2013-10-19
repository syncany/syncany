package org.syncany.tests.scenarios;

import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.scenarios.framework.AbstractClientAction;
import org.syncany.tests.scenarios.framework.ChangeTypeFileToFolder;
import org.syncany.tests.scenarios.framework.ClientActions;
import org.syncany.tests.scenarios.framework.CreateFileTree;
import org.syncany.tests.scenarios.framework.Executable;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class ChangedTypeScenarioTest {
	@Test
	public void testChangeTypeToFolder() throws Exception {		
		final Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		final TestClient clientA = new TestClient("A", testConnection);
		final TestClient clientB = new TestClient("B", testConnection);
		
		ClientActions.run(clientA, null,
			new AbstractClientAction[] {
				new CreateFileTree(),
				new ChangeTypeFileToFolder(),
			},
			new Executable() {
				@Override
				public void execute() throws Exception {
					clientA.upWithForceChecksum();		
					
					clientB.down();
					assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
					assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());					
				}			
			}
		);
		
		clientA.cleanup();
		clientB.cleanup();
	}
}
