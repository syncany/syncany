/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.operations.daemon;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.syncany.operations.daemon.messages.GetFileFolderRequest;
import org.syncany.operations.daemon.messages.GetFileFolderResponse;
import org.syncany.operations.daemon.messages.ListWatchesManagementRequest;
import org.syncany.operations.daemon.messages.UpUploadFileSyncExternalEvent;
import org.syncany.operations.daemon.messages.api.Message;
import org.syncany.operations.daemon.messages.api.Request;
import org.syncany.operations.daemon.messages.api.Response;
import org.syncany.operations.daemon.messages.api.XmlMessageFactory;

public class XmlMessageFactoryTest {
	@Test
	public void testXmlToMessageSuccess() throws Exception {
		Message message = XmlMessageFactory.toMessage("<listWatchesManagementRequest><id>123</id></listWatchesManagementRequest>");

		assertEquals(ListWatchesManagementRequest.class, message.getClass());
		assertEquals(123, ((ListWatchesManagementRequest) message).getId());
	}
	
	@Test(expected = Exception.class)
	public void testXmlToMessageFailure() throws Exception {
		XmlMessageFactory.toMessage("This is invalid!");
	}

	@Test
	public void testXmlToRequestSuccess() throws Exception {
		Request request = XmlMessageFactory
				.toRequest("<getFileFolderRequest><id>1234</id><root>/some/path</root><fileHistoryId>beefbeefbeef</fileHistoryId><version>1337</version></getFileFolderRequest>");

		assertEquals(GetFileFolderRequest.class, request.getClass());
		assertEquals(1234, ((GetFileFolderRequest) request).getId());
		assertEquals("/some/path", ((GetFileFolderRequest) request).getRoot());
		assertEquals("beefbeefbeef", ((GetFileFolderRequest) request).getFileHistoryId());
		assertEquals(1337, ((GetFileFolderRequest) request).getVersion());
	}

	@Test(expected = Exception.class)
	public void testXmlToRequestFailure() throws Exception {
		XmlMessageFactory.toRequest("<showMessageExternalEvent><message>Hi there.</message></showMessageExternalEvent>");
	}
	
	@Test
	public void testXmlToResponseSuccess() throws Exception {
		Response response = XmlMessageFactory
				.toResponse("<getFileFolderResponse><code>200</code><requestId>1234</requestId><root>/some/path</root><tempFileToken>beefbeefbeef</tempFileToken></getFileFolderResponse>");

		assertEquals(GetFileFolderResponse.class, response.getClass());
		assertEquals(200, ((GetFileFolderResponse) response).getCode());
		assertEquals((Integer) 1234, ((GetFileFolderResponse) response).getRequestId());
		assertEquals("beefbeefbeef", ((GetFileFolderResponse) response).getTempToken());
	}

	@Test(expected = Exception.class)
	public void testXmlToResponseFailure() throws Exception {
		XmlMessageFactory.toResponse("<watchEndSyncExternalEvent><root>/some/path</root></watchEndSyncExternalEvent>");
	}
	
	@Test
	public void testRequestToXml() throws Exception {
		UpUploadFileSyncExternalEvent event = new UpUploadFileSyncExternalEvent("/some/path", "filename.jpg");		
		String xmlStr = XmlMessageFactory.toXml(event).replaceAll("\\s+", "");
		
		assertEquals("<upUploadFileSyncExternalEvent><root>/some/path</root><filename>filename.jpg</filename></upUploadFileSyncExternalEvent>", xmlStr);
	}
	
	// TODO [low] Missing tests for the converters
}
