/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.operations.watch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.util.StringUtil;

/**
 * The notification listener implements a client to the fanout, as very
 * lightweight pub/sub server originally written for SparkleShare.
 * 
 * <p>Fanout implements a simple TCP-based plaintext protocol.<br /> 
 * It implements the following <b>commands</b>:
 * <ul>
 *  <li><tt>subcribe &lt;channel&gt;</tt></li>
 *  <li><tt>unsubscribe &lt;channel&gt;</tt></li>
 *  <li><tt>announce &lt;channel&gt; &lt;message&gt;</tt></li>
 * </ul>
 * 
 * <p><b>Notifications</b> have the following format:
 * <ul>
 *  <li><tt>&lt;channel&gt;!&lt;message&gt;</tt></li>
 * </ul>
 * 
 * <p>The notification listener starts a thread and listens for incoming messages.
 * Outgoing messages (subscribe/unsubscribe/announce) are sent directly or (if that
 * fails), put in an outgoing queue. Incoming messages are handed over to a 
 * {@link NotificationListenerListener}.
 * 
 * @see <a href="https://github.com/travisghansen/fanout/">https://github.com/travisghansen/fanout/</a> - Fanout source code by Travis G. Hansen
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class NotificationListener {
	private static final Logger logger = Logger.getLogger(NotificationListener.class.getSimpleName());
	private static final int SOCKET_TIMEOUT = 10000;
	private static final int RECONNECT_WAIT_TIME = 5000;

	private String host;
	private int port;
	private NotificationListenerListener listener;

	private AtomicBoolean connected;
	private AtomicBoolean running;

	private Socket socket;
	private OutputStream socketOut;
	private BufferedReader socketIn;

	private Set<String> subscribedChannels;
	private Queue<String> outgoingMessageQueue;
	private Thread incomingMessageThread;

	public NotificationListener(String host, int port, NotificationListenerListener listener) {
		this.host = host;
		this.port = port;
		this.listener = listener;

		this.subscribedChannels = new HashSet<String>();
		this.incomingMessageThread = null;
		this.outgoingMessageQueue = new LinkedList<String>();
		
		this.connected = new AtomicBoolean(false);
		this.running = new AtomicBoolean(false);
	}

	public void start() {
		logger.log(Level.INFO, "Starting notification listener thread ...");

		stop();

		incomingMessageThread = new SocketThread();
		incomingMessageThread.start();
	}

	public void stop() {
		if (incomingMessageThread != null) {
			logger.log(Level.INFO, "Stopping notification listener thread ...");

			try {
				running.set(false);

				if (socket != null) {
					socket.close();				
				}
				
				if (incomingMessageThread != null) {
					incomingMessageThread.interrupt();
				}
			}
			catch (Exception e) {
				// Don't care
			}
			finally {
				incomingMessageThread = null;
			}
		}
	}

	public void subscribe(String channel) {
		subscribedChannels.add(channel);

		logger.log(Level.INFO, "Subscribing to channel " + channel + "...");
		sendMessageOrAddToOutgoingQueue(String.format("subscribe %s\n", channel));
	}

	public void unsubscribe(String channel) {
		subscribedChannels.remove(channel);

		logger.log(Level.INFO, "Unsubscribing from channel " + channel + "...");
		sendMessageOrAddToOutgoingQueue(String.format("unsubscribe %s\n", channel));
	}

	public void announce(String channel, String message) {
		logger.log(Level.INFO, "Announcing to channel " + channel + ": " + message.trim());
		sendMessageOrAddToOutgoingQueue(String.format("announce %s %s\n", channel, message));
	}

	private void sendMessageOrAddToOutgoingQueue(String message) {
		if (connected.get()) {
			try {
				socketOut.write(StringUtil.toBytesUTF8(message));
				logger.log(Level.INFO, "Sent message: " + message.trim());
			}
			catch (Exception e) {
				queueOutgoingMessage(message);
			}
		}
		else {
			queueOutgoingMessage(message);
		}
	}

	private void queueOutgoingMessage(String message) {
		if (!outgoingMessageQueue.contains(message)) {
			logger.log(Level.INFO, "Sending failed or no connection, queuing message: " + message.trim());
			outgoingMessageQueue.offer(message);
		}
		else {
			logger.log(Level.INFO, "Sending failed and message already in queue: " + message.trim());
		}
	}

	private void connect() {
		try {
			logger.log(Level.INFO, "Connecting socket to " + host + ":" + port + " ...");

			socket = new Socket(host, port);
			socket.setSoTimeout(SOCKET_TIMEOUT);

			socketOut = socket.getOutputStream();
			socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			connected.set(socket.isConnected());
		}
		catch (Exception e) {
			disconnect();
		}
	}

	private void disconnect() {
		try {
			logger.log(Level.INFO, "Disconnecting socket ...");

			if (socket != null) {
				socket.close();
			}

			if (socketOut != null) {
				socketOut.close();
			}

			if (socketIn != null) {
				socketIn.close();
			}
		}
		catch (Exception e) {
			// Don't care!
		}
		finally {
			socket = null;
			socketIn = null;
			socketOut = null;

			connected.set(false);
		}
	}

	private class SocketThread extends Thread {
		public SocketThread() {
			super("NotifyThread");
		}

		@Override
		public void run() {
			running.set(true);
			connect();

			while (running.get()) {
				try {
					if (socket == null || socketIn == null) {
						throw new Exception("Socket closed");
					}

					if (outgoingMessageQueue.size() > 0) {
						logger.log(Level.INFO, "Processing queued outgoing messages ...");
						processOutgoingMessages();
					}

					logger.log(Level.INFO, "Waiting for incoming message (" + SOCKET_TIMEOUT + " ms) ...");
					processIncomingMessage(socketIn.readLine());
				}
				catch (SocketTimeoutException e) {
					// Nothing!
				}
				catch (InterruptedException e) {
					logger.log(Level.INFO, "Notification listener interrupted.");
					running.set(false);
				}
				catch (Exception e) {
					try {
						logger.log(Level.INFO, "Notification connection down: " + e.getMessage() + ", sleeping " + RECONNECT_WAIT_TIME
								+ "ms, then trying a re-connect ...");

						Thread.sleep(RECONNECT_WAIT_TIME);
						connect();

						if (subscribedChannels.size() > 0) {
							logger.log(Level.INFO, "Re-subscribing to channels after broken connection ...");

							for (String channel : subscribedChannels) {
								subscribe(channel);
							}
						}
					}
					catch (InterruptedException e2) {
						logger.log(Level.INFO, "Notification listener interrupted.");
						running.set(false);
					}
				}
			}

			logger.log(Level.INFO, "STOPPED notification listener!");
		}

		private void processOutgoingMessages() throws IOException {
			String nextMessage = null;

			while (null != (nextMessage = outgoingMessageQueue.poll())) {
				socketOut.write(StringUtil.toBytesUTF8(nextMessage));
				logger.log(Level.INFO, "- Sent queued message " + nextMessage);
			}
		}

		private void processIncomingMessage(String messageLine) {
			String[] messageParts = messageLine.split("!");

			if (messageParts.length == 2) {
				String channel = messageParts[0];
				String message = messageParts[1];

				if (!"debug".equals(channel)) {
					logger.log(Level.INFO, "Received message for channel " + channel + ": " + message);
					listener.pushNotificationReceived(channel, message);
				}
			}
		}
	}

	public interface NotificationListenerListener {
		public void pushNotificationReceived(String channel, String message);
	}
}
