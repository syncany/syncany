/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
 * @see <a href="https://www.syncany.org/">Syncany website</a> at syncany.org
 * @see <a href="https://github.com/syncany/syncany">GitHub code repository</a> at github.com
 * @see <a href="http://www.philippheckel.com/files/syncany-heckel-thesis.pdf">Master thesis describing core concepts</a> at philippheckel.com
 * @see <a href="http://blog.philippheckel.com/tag/syncany/">Blog posts about Syncany</a> at blog.philippheckel.com
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Syncany {
	public static void main(String[] args) throws Exception {
		/**
		String link = "syncany://storage/1/AA-BB";
		Pattern p = Pattern.compile("^syncany://storage/1/(?:(not-encrypted/)(.+)|([^-]+-(.+)))$");
		Matcher linkMatcher = p.matcher(link);
		
		for (int i = 0 ; i < linkMatcher.groupCount() ; i ++){
			System.out.println(linkMatcher.group(i));
		}
		**/
		
		String[] dd= new String[]{"--localdir=/Users/vwiencek/sandbox/client2", "connect", "syncany://storage/1/5sx8ke2sxTU58fmZbKRn7pqxWCdjDbg7qwExvVnSi4muqL9q9onwSj5qBwpgcwrE87JPmZLEQaD2YYXWZziU5oLT/BwuzZQ9n7dyKY4rZhS1dRfpCpkhGh3wBYiuoM6WZapT7vZ3JK8bvMExaqaZ2UAoLC72RanuccNNG878DrbDC7DSYADdQ9hk6qLoK3UiG7dQQaJWPipTTaNDfEE3d7FV3ts8r1UEAXNY8Yd1ax6FsZo9sxpmLSs63voz35qx1z6PKBaNbdMongH6gGcZjGHUaikMWv1atZmfmfyTsbm2FbcAp4eUuDwBfFVanNC3137ut61gsDc5oeeQ59Ey84oPaCxvcs2fxKfLPMveT4tSuNjfevf9XKqKw8mssWnwKmevsszTgfcjsESVZae2aiG9Tr4NyyJnCuXQTEf6twii"};
		System.exit(new CommandLineClient(args).start());
	}		
}
