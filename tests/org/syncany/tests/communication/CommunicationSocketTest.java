package org.syncany.tests.communication;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.communication.*;
import org.syncany.communication.CommunicationController.SyncanyStatus;

public class CommunicationSocketTest {
	
	static String startDelimiter = "STX";
	static String endDelimiter = "ETX";

	static CommunicationSocket comSock;
	static TestServer server;
	
	@BeforeClass
	public static void init(){}
	
	@Test
	public void socketTest() throws Exception{
		try{
			createNewConnection(4711);
		} catch (Exception e) {
			createNewConnection(4712);
		}
		
		String msg = CommunicationController.getInstance().createMessage("Hello World!");
		
		// send message
		comSock.send(msg);
		Assert.assertEquals("sent and received message are not identical.", startDelimiter + msg + endDelimiter, server.waitForMsg());	
		
		// receive msg
		server.write(msg + endDelimiter + startDelimiter + "loremIpsum" + endDelimiter + startDelimiter + msg);
		Assert.assertEquals("First send message wasn't returned successfully.", startDelimiter + msg + endDelimiter, server.waitForMsg());
		Assert.assertEquals("Second send message wasn't returned successfully.", startDelimiter + msg + endDelimiter, server.waitForMsg());
		
		
		msg = msg.replace("echo", "XXX");
		server.write(msg);
		Assert.assertTrue("No Error when send unknown messageType.", server.waitForMsg().contains("error"));


		server.write("{\"messageType\":\"requestFileStatus\",\"data\":{" +
				"\"files\":[\"file1\", \"file2\", \"file3\"]}}");
		Assert.assertTrue("Response to requestFileStatus has wrong messageType.", server.waitForMsg().contains("fileStatus"));
		
		System.out.println("45");
		// communicationController test
		CommunicationController cc = CommunicationController.getInstance();
		System.out.println("48");
		cc.updateStatus(SyncanyStatus.inSync);
		System.out.println("50");
		Assert.assertTrue("SyncanyStatus update not received.", server.waitForMsg().contains("inSync"));
		System.out.println("52");
		cc.sendAck();
		Assert.assertTrue("ACK not received.", server.waitForMsg().contains("ack"));
		System.out.println("55");
		cc.requestInitialization();
		Assert.assertTrue("Initialization request not received.", server.waitForMsg().contains("initializeMe"));
		
		comSock.disconnect();
	}
	
	public static void createNewConnection(int port) {
		createTestServer(port);
		createSocket(port);
	}
	
	public static void createSocket(int port) {
		comSock = CommunicationSocket.getInstance();
		comSock.setPort(port);
		comSock.connect();
	}
	
	public static void createTestServer(int port) {
		server = new TestServer();
		server.port = port;
		server.startDelimiter = startDelimiter;
		server.endDelimiter = endDelimiter;
		
		Thread t = new Thread( server );
		t.start();
	}
	
}
