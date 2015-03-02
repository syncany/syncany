/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.unit.operations.deamon;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.input.Tailer;
import org.junit.Before;
import org.junit.Test;
import org.syncany.config.LocalEventBus;
import org.syncany.operations.daemon.ControlServer;
import org.syncany.operations.daemon.ControlServer.ControlCommand;
import org.syncany.operations.daemon.ServiceAlreadyStartedException;

/**
 * Unit tests for the {@link org.syncany.operations.daemon.ControlServer} class, 
 * using Mockito for mocking its instance variables.
 * 
 * @author Niels Spruit
 *
 */
public class ControlServerTest {

	private ControlServer ctrlServer;
	private File ctrlFile;
	private Tailer ctrlFileTailer;
	private LocalEventBus eventBus;

	@Before
	public void setUp() {
		ctrlFile = mock(File.class);
		ctrlFileTailer = mock(Tailer.class);
		eventBus = mock(LocalEventBus.class);
		ctrlServer = new ControlServer(ctrlFile, ctrlFileTailer, eventBus);
	}

	@Test
	public void testEnterLoop() throws IOException, ServiceAlreadyStartedException {
		// invoke method
		ctrlServer.enterLoop();

		// verify interactions
		verify(ctrlFile).delete();
		verify(ctrlFile).createNewFile();
		verify(ctrlFile).deleteOnExit();
		verify(ctrlFileTailer).run();
	}

	@Test
	public void testHandleShutdown() {
		ctrlServer.handle("SHUTDOWN");

		verifyZeroInteractions(ctrlFile);
		verify(eventBus).post(ControlCommand.SHUTDOWN);
		verifyNoMoreInteractions(eventBus);
		verify(ctrlFileTailer).stop();
		verifyNoMoreInteractions(ctrlFileTailer);
	}

	@Test
	public void testHandleReload() {
		ctrlServer.handle("RELOAD");

		verifyZeroInteractions(ctrlFile);
		verifyZeroInteractions(ctrlFileTailer);
		verify(eventBus).post(ControlCommand.RELOAD);
		verifyNoMoreInteractions(eventBus);
	}

	@Test
	public void testHandleUnknownCommand() {
		ctrlServer.handle("UnknownCommand");

		verifyZeroInteractions(ctrlFile);
		verifyZeroInteractions(ctrlFileTailer);
		verifyZeroInteractions(eventBus);
	}

	@Test(expected = RuntimeException.class)
	public void testFileNotFound() {
		ctrlServer.fileNotFound();
	}

	@Test(expected = RuntimeException.class)
	public void testHandle() {
		ctrlServer.handle(new Exception());
	}

}
