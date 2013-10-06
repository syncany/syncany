package org.syncany.tests.scenarios.framework;

import java.io.File;
import java.io.FileFilter;

public class MoveFileToOtherFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		final File fromFile = pickFile(hashCode());
		final File toFile = pickFileOrFolder(hashCode()*hashCode(), new FileFilter() {				
			@Override
			public boolean accept(File file) {
				return file.isFile() && !fromFile.getParentFile().getAbsolutePath().equals(file.getParentFile().getAbsolutePath());
			}
		});
		
		log(this, fromFile+" -> "+toFile);
		fromFile.renameTo(toFile);
	}		
}
