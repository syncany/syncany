/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.gui.panel;

import java.util.List;

import javax.swing.table.DefaultTableModel;

/**
 * @author vincent
 *
 */
public class WatchedTableModel extends DefaultTableModel {
	private static final long serialVersionUID = -4311408284130605976L;

	private List<String[]> data;
	
	public WatchedTableModel(List<String[]> data){
		this.data = data;
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch (columnIndex){
			case 0:
				return data.get(rowIndex)[0];
			case 1:
				return data.get(rowIndex)[1];
			case 2:
				return data.get(rowIndex)[2];
		}
		return data.get(rowIndex);
	}
	
	@Override
	public int getRowCount() {
		return (data == null ? 0 : data.size());
	}
	
	@Override
	public String getColumnName(int columnIndex) {
		return "Folder watched";
	}
	
	@Override
	public int getColumnCount() {
		return 3;
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}
}
