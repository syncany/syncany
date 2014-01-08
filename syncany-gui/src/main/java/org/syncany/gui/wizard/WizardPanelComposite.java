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

import org.eclipse.swt.widgets.Composite;
import org.syncany.gui.panel.UserParametersChecker;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public abstract class WizardPanelComposite extends Composite implements UserParametersChecker {
	private WizardDialog parentWizardDialog;

	protected WizardPanelComposite(Composite parent, int style) {
		super(parent, style);
	}
	
	public WizardPanelComposite(WizardDialog parentWizardDialog, Composite parent, int style) {
		this(parent, style);
		this.parentWizardDialog = parentWizardDialog;
	}
	
	public WizardDialog getParentWizardDialog() {
		return parentWizardDialog;
	}
	
	public boolean hasCancelButton(){
		return true;
	}

	public abstract boolean hasNextButton();
	public abstract boolean hasPreviousButton();
	public abstract boolean hasFinishButton();
	public abstract void updateData();
}
