package org.syncany;

import java.io.File;

import org.syncany.config.ConfigTO;
import org.syncany.config.Profile;

public class Syncany {
	public static void main(String[] args) throws Exception {

		// Parse arguments
		File configFile = new File("config.json");//System.getProperty("user.dir")+File.separator+"config.json");
		
		if (args.length > 0) {
			configFile = new File(args[0]);
		}
				
		// Read config
		Profile profile = new Profile(ConfigTO.load(configFile));

		Application application = new Application(profile);

		application.initProfileDirectories();
		application.syncUp();
	}
}
