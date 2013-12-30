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
package org.syncany.gui.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.syncany.util.I18n;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class NewLocalFolders extends Composite {
	private List list;
	
	public NewLocalFolders(Composite parent, int style) {
		super(parent, style);
		initComposite();
	}
	
	private void initComposite(){
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(1, false);
		gl_composite.verticalSpacing = 15;
		setLayout(gl_composite);
		
		list = new List(this, SWT.BORDER | SWT.V_SCROLL);
		list.setItems(I18n.getString(NewLocalFolders.class.getSimpleName()+".dialog.foldersList").split(","));
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Composite composite_1 = new Composite(this, SWT.NONE);
		composite_1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		composite_1.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		Button removeButton = new Button(composite_1, SWT.NONE);
		removeButton.setText(I18n.getString(NewLocalFolders.class.getSimpleName()+".dialog.removeFolder"));
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int idx = list.getSelectionIndex();
				list.remove(idx);
			}
		});
		
		Button addButton = new Button(composite_1, SWT.NONE);
		addButton.setText(I18n.getString(NewLocalFolders.class.getSimpleName()+".dialog.addFolder"));
	}
}