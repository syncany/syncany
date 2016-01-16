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
package org.syncany.tests.integration.scenarios.framework;

import java.io.File;

import org.apache.commons.io.FileUtils;

public class MoveFolderWithinFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File fromFolder = pickFolder(3123);
		File toFolder = new File(fromFolder+"-ren"+fromFolder.hashCode());
		
		log(this, fromFolder+" -> "+toFolder);
		FileUtils.moveDirectory(fromFolder, toFolder);
	}		
}

