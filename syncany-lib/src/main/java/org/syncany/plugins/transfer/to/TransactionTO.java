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
package org.syncany.plugins.transfer.to;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.chunk.Transformer;

/**
 * The Transaction transfer object exists to serialize a transaction,
 * which is saved on the remote location and deleted once the transaction is
 * completed.
 * 
 * <p>It uses the Simple framework for XML serialization, and its corresponding
 * annotation-based configuration.  
 *  
 * @see <a href="http://simple.sourceforge.net/">Simple framework</a> at simple.sourceforge.net
 * @author Pim Otte
 */
@Root(name = "transaction", strict = false)
public class TransactionTO {
	@Element(name = "machineName")
	private String machineName;

	@ElementList(name = "actions", entry = "action")
	private ArrayList<ActionTO> actionTOs;

	public TransactionTO() {
		// Nothing
	}

	public TransactionTO(String machineName) {
		this.machineName = machineName;
		this.actionTOs = new ArrayList<ActionTO>();
	}

	public String getMachineName() {
		return machineName;
	}

	public List<ActionTO> getActions() {
		return actionTOs;
	}

	public void addAction(ActionTO transactionAction) {
		actionTOs.add(transactionAction);
	}

	public static TransactionTO load(Transformer transformer, File transactionFile) throws Exception {
		InputStream is;

		if (transformer == null) {
			is = new FileInputStream(transactionFile);
		}
		else {
			is = transformer.createInputStream(new FileInputStream(transactionFile));
		}

		return new Persister().read(TransactionTO.class, is);
	}

	public void save(Transformer transformer, File transactionFile) throws Exception {
		PrintWriter out;

		if (transformer == null) {
			out = new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(transactionFile), "UTF-8"));
		}
		else {
			out = new PrintWriter(new OutputStreamWriter(
					transformer.createOutputStream(new FileOutputStream(transactionFile)), "UTF-8"));
		}

		Serializer serializer = new Persister();
		serializer.write(this, out);
		out.flush();
		out.close();
	}
}
