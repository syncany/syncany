package org.syncany.tests.scenario;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.syncany.config.ConfigTO;
import org.syncany.tests.TestUtil;

import com.google.gson.JsonSyntaxException;

/**
 * Scenario test helper for cleaning up directories and setting up logging 
 */
public class ScenarioTestHelper {

	public static void cleanupDirectories(String configPath, boolean cleanRepo) throws JsonSyntaxException, IOException {
		// read without initializing objects
		ConfigTO d = new ConfigTO(new File(configPath));
		TestUtil.emptyDirectory(new File(d.getAppDir()));
		TestUtil.emptyDirectory(new File(d.getCacheDir()));

		if (cleanRepo && d.getConnection().getType().equals("local"))
			TestUtil.emptyDirectory(new File(d.getConnection()
					.getSettings().get("path")));

		TestUtil.emptyDirectory(new File(d.getLocalDir()));
	}

	public static Logger setupLogging(String fileName) throws SecurityException,
			IOException {
		Logger l = Logger.getLogger("");
		l.setLevel(Level.ALL);
		FileHandler fileHandler = new FileHandler(fileName);
		fileHandler.setFormatter(new Formatter() {
			@Override 
			public String format(LogRecord record) {
				return record.getLevel() + ": "
						+ record.getSourceClassName() + "."
						+ record.getSourceMethodName() + " -> "
						+ formatMessage(record) + "\n";
			}

		});
		l.addHandler(fileHandler);
		for (Handler h : l.getHandlers()) {
			if (h instanceof ConsoleHandler)
				l.removeHandler(h);
		}
		return l;
	}
}
