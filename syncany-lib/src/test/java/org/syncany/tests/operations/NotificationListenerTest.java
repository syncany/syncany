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
package org.syncany.tests.operations;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.config.Logging;
import org.syncany.operations.watch.NotificationListener;
import org.syncany.operations.watch.NotificationListener.NotificationListenerListener;

public class NotificationListenerTest {
	private static final Logger logger = Logger.getLogger(NotificationListenerTest.class.getName());
	
	static {
		Logging.init();
	}
	
	@Test
	public void testNotificationListener() throws IOException, InterruptedException {
		String randomChannelName = NotificationListenerTest.class.getName() + new Random().nextInt();

		final AtomicInteger messagesReceivedBy1 = new AtomicInteger(0);
		final AtomicInteger messagesReceivedBy2 = new AtomicInteger(0);
		
		final NotificationListener notificationListener1 = new NotificationListener("notify.syncany.org", 8080, new NotificationListenerListener() {			
			@Override
			public void pushNotificationReceived(String channel, String message) {
				logger.log(Level.INFO, "Client 1: pushNotificationReceived(channel = "+channel+", message = "+message+")");
				messagesReceivedBy1.addAndGet(1);
			}
		});
		
		final NotificationListener notificationListener2 = new NotificationListener("notify.syncany.org", 8080, new NotificationListenerListener() {			
			@Override
			public void pushNotificationReceived(String channel, String message) {
				logger.log(Level.INFO, "Client 2: pushNotificationReceived(channel = "+channel+", message = "+message+")");				
				messagesReceivedBy2.addAndGet(1);		
			}
		});		
		
		// Start
		notificationListener1.start();
		notificationListener2.start();
		
		notificationListener1.subscribe(randomChannelName);
		notificationListener2.subscribe(randomChannelName);
				
		Thread.sleep(1500); // Let them settle
		
		notificationListener1.announce(randomChannelName, "Message from 1");
		notificationListener2.announce(randomChannelName, "Message from 2");
		
		for (int i = 0; i < 100; i++) {
			Thread.sleep(100); // Wait for messages
			if (messagesReceivedBy1.get() == 2 && messagesReceivedBy2.get() == 2) {
				break;
			}
		}
		assertEquals("Different amount of messages received by 1", 2, messagesReceivedBy1.get());
		assertEquals("Different amount of messages received by 2", 2, messagesReceivedBy2.get());

		notificationListener1.stop();
		notificationListener2.stop();
	}
}
