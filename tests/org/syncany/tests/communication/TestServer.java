package org.syncany.tests.communication;

import java.io.*;
import java.net.*;

public class TestServer implements Runnable {
	
			public String startDelimiter = "STX";
			public String endDelimiter = "ETX";
			public int port;
			
			Socket socket;
	
			public TestServer() {}
			
			public void run() {
				ServerSocket serverSocket;
				try {
					serverSocket = new ServerSocket(port);
					socket = waitForConnection(serverSocket);
					
					System.out.println("Client Connected");
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
   		    
		    private Socket waitForConnection(ServerSocket serverSocket) throws IOException {
		        return serverSocket.accept();
		    }
		    
		    public String waitForMsg() throws IOException {
		    	char[] buffer = new char[200];
		    	BufferedReader in = new BufferedReader(new InputStreamReader( socket.getInputStream()));
				int bufferSize = in.read(buffer, 0, 200);
				String msg = new String(buffer, 0, bufferSize);
				return msg;
		    }
		    
		    public void write(String msg) throws IOException {
		        PrintWriter printWriter =
		            new PrintWriter(
		                new OutputStreamWriter(
		                    socket.getOutputStream()));
		        printWriter.print(startDelimiter + msg + endDelimiter);
		        printWriter.flush();
		    }		    
}
