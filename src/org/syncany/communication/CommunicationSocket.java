package org.syncany.communication;

import java.io.*;
import java.net.*;
import java.util.logging.Logger;

import org.syncany.watch.remote.RemoteWatcher;

/**
 * Class creating providing socket for communication between GUI and Syncany
 * 
 * @author Paul Steinhilber
 */
public class CommunicationSocket {
	
	private static final Logger logger = Logger.getLogger(CommunicationSocket.class
			.getSimpleName());
	
	private String startDelimiter = "STX";
	private String endDelimiter = "ETX";
	
	private static CommunicationSocket instance;
	private int port = -1;
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	public boolean connected = false;

	public static CommunicationSocket getInstance() {
		if (instance == null) {
			instance = new CommunicationSocket();
		}
		return instance;
	}

	private CommunicationSocket() {

	}

	public void connect() {
		if (connected)
				return;
		
		try {
			socket = new Socket("localhost", port);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader( socket.getInputStream()));
			connected = true;
			
			// Start Listener
			Thread t = new Thread(new Listener());
			t.start();
			
		} catch (Exception e) {
			e.printStackTrace();
			connected = false;
		}
	}

	public void setPort(int newPort){
		port = newPort;
	}

	public int getPort() {
		return port;
	}

	public void send(String msg) {
		if (!connected) {
			logger.info("NOT CONNECTED - can't send message: " + msg);
			return;
		}

		out.print(startDelimiter + msg + endDelimiter);
		out.flush();
		
		logger.info("Message send:" + msg);
	}

	private String receive() throws IOException {
		char[] buffer = new char[200];
		int bufferSize = in.read(buffer, 0, 200);
		String msg = new String(buffer, 0, bufferSize);
		return msg;
	}

	public void disconnect() {
		try {
			out.close();
			in.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	class Listener implements Runnable {
		CommunicationController cc = CommunicationController.getInstance();
		String received = "";

		public void run() {
			while (true) {
				try {
					received = received + receive();

					if (received.contains(startDelimiter)) {
						String[] split_stx = received.split(startDelimiter);

						for (int i=0; i<split_stx.length; i++) {

							// skip first if empty
							if (i == 0 && split_stx[i].isEmpty()) 
								continue;

							// check if last msg is already complete
							if (i == split_stx.length-1) {
								if (split_stx[i].contains(endDelimiter)) {
									String[] split_etx = split_stx[i].split(endDelimiter);
									cc.processMessage(split_etx[0]);
									try {
										received = split_etx[1];
									} catch (Exception e) {
										received = "";
									}
								} else {
									received = split_stx[i];
								}

							} else {
								String[] split_etx = split_stx[i].split(endDelimiter);
								cc.processMessage(split_etx[0]);
							}	
						}
					}
				}
				catch (SocketException e) {
					System.out.println("Socket closed. Connection closed.");
					return;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}
