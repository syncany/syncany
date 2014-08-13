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
package org.syncany.plugins.transfer.to;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

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
@Root(name="transaction")
@Namespace(reference="http://syncany.org/transaction/1")
public class TransactionTO {
	private static final Logger logger = Logger.getLogger(TransactionTO.class.getSimpleName()); 
	
	@Element(name="machineName") 
	private String machineName;
	
	@ElementList(entry="action")
	private List<ActionTO> actionTOs;
	
	
	public TransactionTO() {
		// Nothing
	}
	
	public TransactionTO(String machineName) {
		this.machineName = machineName;
		actionTOs = new ArrayList<ActionTO>();
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
}
