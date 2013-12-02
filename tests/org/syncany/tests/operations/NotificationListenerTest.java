/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.syncany.config.Logging;
import org.syncany.operations.NotificationListener;
import org.syncany.operations.NotificationListener.NotificationListenerListener;

public class NotificationListenerTest {
	static {
		Logging.init();
	}
	
	@Test
	public void testDeduperWithDatabase() throws IOException, InterruptedException {
		final NotificationListener notificationListener1 = new NotificationListener("localhost", 1986, new NotificationListenerListener() {			
			@Override
			public void pushNotificationReceived(String channel, String message) {
				System.out.println("1 NEW channel = "+channel+", message = "+message);
			}
		});
		
		final NotificationListener notificationListener2 = new NotificationListener("localhost", 1986, new NotificationListenerListener() {			
			@Override
			public void pushNotificationReceived(String channel, String message) {
				System.out.println("2 NEW channel = "+channel+", message = "+message);
			}
		});
		
		notificationListener1.start();
		notificationListener2.start();
		
		notificationListener1.subscribe("syncanychannel");
		notificationListener2.subscribe("syncanychannel");

		
		new Thread(new Runnable() {			
			@Override
			public void run() {
				while (true) {
					notificationListener1.announce("syncanychannel", ""+System.currentTimeMillis());
					
					try { Thread.sleep(2000); }
					catch (Exception e) { }
				}
			}
		}).start();		
		
		Thread.sleep(60000);
		
		fail("Test me!!!");
	}

}
