package org.syncany.tests.scenarios.framework;

import java.io.File;

import org.syncany.util.FileUtil;

public class ChangeTypeFileToSymlinkWithTargetFile extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		if (!FileUtil.symlinksSupported()) {
			return; // no symbolic links on Windows
		}

		File file = pickFile(7278);

		log(this, file.getAbsolutePath());

		file.delete();
		FileUtil.createSymlink("/etc/hosts", file);
	}
}
