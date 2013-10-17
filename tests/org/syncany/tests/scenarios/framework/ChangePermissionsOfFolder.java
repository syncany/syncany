package org.syncany.tests.scenarios.framework;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

import org.syncany.util.FileUtil;

public class ChangePermissionsOfFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File file = pickFolder(1922);
		Path filePath = Paths.get(file.getAbsolutePath());
				
		if (FileUtil.isWindows()) {
			Files.setAttribute(filePath, "dos:hidden", true);
			Files.setAttribute(filePath, "dos:system", true);
			Files.setAttribute(filePath, "dos:readonly", true);
			Files.setAttribute(filePath, "dos:archive", true);
		}
		else if (FileUtil.isUnixLikeOperatingSystem()) {
			log(this, "rwxrwxrwx "+file.getAbsolutePath());
			Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwxrwxrwx"));
		}		
	}		
}	
