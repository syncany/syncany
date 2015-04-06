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
package org.syncany;

import org.syncany.cli.CommandLineClient;

/**
 * Syncany is an open-source cloud storage and filesharing application. It allows
 * users to backup and share certain folders of their workstations using any kind
 * of storage, e.g. FTP, Amazon S3 or Google Storage.
 * 
 * While the basic idea is similar to Dropbox and JungleDisk, Syncany is open-source
 * and additionally provides data encryption and more flexibility in terms of storage
 * type and provider:
 * 
 * <ul>
 *   <li><b>Data encryption:</b> Syncany encrypts the files locally, so that any online storage
 *       can be used even for sensitive data.</li>
 *   <li><b>Arbitrary storage:</b> Syncany uses a plug-in based storage system. It can be used
 *       with any type of remote storage.</li>
 * </ul>
 * 
 * <p>The Syncany class is the central entry point for the command line client and
 * the only class to expose a main method. It does not offer any functionality
 * on its own, but rather just instantiates a {@link CommandLineClient} and passes
 * the command line arguments to it.  
 * 
 * <p>Many thanks to all the people who have supported the project over the years and who have
 * continued to listen to my constant never-ending Syncany talk! Thank you all!
 * 
 * @see <a href="https://www.syncany.org/">Syncany website</a>
 * @see <a href="https://github.com/syncany/syncany">GitHub code repository</a> 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Syncany {
	public static void main(String[] args) throws Exception {
		System.exit(new CommandLineClient(args).start());
	}		
}
