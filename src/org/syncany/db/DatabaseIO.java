/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import org.syncany.config.Settings;

public class DatabaseIO {

	public static synchronized void writeCompleteCloneFileTree(
			CloneFileTree tree) {
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(Settings.getInstance().getCloneFileDbFile());
			out = new ObjectOutputStream(fos);
			out.writeObject(tree);
			out.close();
		} catch (FileNotFoundException e) {
			// TODO: Implement Error Reporting from DAEMON to HOSTING APP
			e.printStackTrace();
		} catch (IOException e) {
			// TODO: Implement Error Reporting from DAEMON to HOSTING APP
			e.printStackTrace();
		}
	}

	public static synchronized CloneFileTree readCompleteCloneFileTree() {
		File f = new File(Settings.getInstance().getCloneFileDbFile());
		if(!f.exists())
			return new CloneFileTree();
		
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try {
			fis = new FileInputStream(Settings.getInstance().getCloneFileDbFile());
			in = new ObjectInputStream(fis);
			CloneFileTree tree = (CloneFileTree) in.readObject();
			in.close();
			return tree;
		} catch (FileNotFoundException e) {
			// TODO: Implement Error Reporting from DAEMON to HOSTING APP
			e.printStackTrace();
		} catch (IOException e) {
			// TODO: Implement Error Reporting from DAEMON to HOSTING APP
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO: Implement Error Reporting from DAEMON to HOSTING APP
			e.printStackTrace();
		}

		return null;
	}

	public static synchronized void writeCloneClients(List<CloneClient> list) {
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			// TODO: Change when using more than one daemon, maybe folder by
			// commandline arguments
			fos = new FileOutputStream(Settings.getInstance().getCloneClientDbFile());
			out = new ObjectOutputStream(fos);
			out.writeObject(list);
			out.close();
		} catch (FileNotFoundException e) {
			// TODO: Implement Error Reporting from DAEMON to HOSTING APP
			e.printStackTrace();
		} catch (IOException e) {
			// TODO: Implement Error Reporting from DAEMON to HOSTING APP
			e.printStackTrace();
		}
	}

	public static synchronized List<CloneClient> readCloneClients() {
		File f = new File(Settings.getInstance().getCloneClientDbFile());
		if(!f.exists())
			return new LinkedList<CloneClient>();
		
		
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try {
			fis = new FileInputStream(Settings.getInstance().getCloneClientDbFile());
			in = new ObjectInputStream(fis);
			@SuppressWarnings("unchecked")
			List<CloneClient> list = (List<CloneClient>) in.readObject();
			in.close();
			return list;
		} catch (FileNotFoundException e) {
			// TODO: Implement Error Reporting from DAEMON to HOSTING APP
			e.printStackTrace();
		} catch (IOException e) {
			// TODO: Implement Error Reporting from DAEMON to HOSTING APP
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO: Implement Error Reporting from DAEMON to HOSTING APP
			e.printStackTrace();
		}

		return null;
	}
}
