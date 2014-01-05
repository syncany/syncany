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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.gui.ApplicationResourcesManager;
import org.syncany.gui.panel.PluginPanel;
import org.syncany.util.I18n;
import org.syncany.util.StringUtil;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class CreateRepositorySelectionPanel extends WizardPanelComposite {
	private static final Logger log = Logger.getLogger(CreateRepositorySelectionPanel.class.getSimpleName());
	
	private Combo pluginSelectionCombo;
	private List<Plugin> pluginList;
	private Label lblNewLabel;
	private Label lblNewLabel_1;
	private Composite pluginStackComposite;
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public CreateRepositorySelectionPanel(Composite parent, int style) {
		super(parent, style);
		
		this.pluginList = Plugins.list();
		
		initComposite();
	}
	
	private void initComposite(){
		Font fontNormal = ApplicationResourcesManager.FONT_NORMAL;
		Font fontBold = ApplicationResourcesManager.FONT_BOLD;
		
		setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout gl_composite = new GridLayout(2, false);
		setLayout(gl_composite);
		
		lblNewLabel = new Label(this, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		lblNewLabel.setFont(fontBold);
		lblNewLabel.setText("New Label");
		
		lblNewLabel_1 = new Label(this, SWT.NONE);
		lblNewLabel_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		lblNewLabel_1.setFont(fontNormal);
		lblNewLabel_1.setText("New Label");
		
		lblNewLabel_2 = new Label(this, SWT.NONE);
		lblNewLabel_2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel_2.setFont(fontNormal);
		lblNewLabel_2.setText(I18n.getString("NewDialog.dialog.option.otherPlugin"));
		
		
		pluginSelectionCombo = new Combo(this, SWT.NONE);
		pluginSelectionCombo.setFont(fontNormal);
		pluginSelectionCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		pluginSelectionCombo.select(0);
		
		pluginSelectionCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String id = (String)pluginSelectionCombo.getData(pluginSelectionCombo.getItem(pluginSelectionCombo.getSelectionIndex()));
				showPLuginPanel(id);;
			}
		});
		
		for (Plugin p : pluginList){
			pluginSelectionCombo.add(p.getName());
			pluginSelectionCombo.setData(p.getName(), p.getId());
		}
		
		pluginStackComposite = new Composite(this, SWT.NONE);
		GridData gd_pluginStackComposite = new GridData(SWT.LEFT, SWT.FILL, false, true, 2, 1);
		gd_pluginStackComposite.verticalIndent = 30;
		gd_pluginStackComposite.horizontalIndent = 30;
		pluginStackComposite.setLayoutData(gd_pluginStackComposite);
		stackLayout = new StackLayout();
		pluginStackComposite.setLayout(stackLayout);
		
		for (Plugin p : pluginList){
			String pluginPanelClassName = String.format("org.syncany.gui.plugin.%sPluginPanel", StringUtil.toCamelCase(p.getId()));
			
			try {
				Class<?>[] type = { Composite.class, int.class };
				Class<?> classDefinition = Class.forName(pluginPanelClassName);
				Constructor<?> cons = classDefinition.getConstructor(type);
				Object[] obj = { pluginStackComposite, SWT.NONE };
				
				PluginPanel pluginPanel = (PluginPanel) cons.newInstance(obj);
				panels.put(p.getId(), pluginPanel);
			}
			catch (Exception e) {
				log.warning("Unable to instanciate plugin gui panel " + pluginPanelClassName);
			}
		}
	}
	
	private void showPLuginPanel(String id){
		PluginPanel ppanel = panels.get(id);
		stackLayout.topControl = ppanel;
		pluginStackComposite.layout();
	}

	private Map<String, PluginPanel> panels = new HashMap<>();
	private StackLayout stackLayout;
	private Label lblNewLabel_2;
	
	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public Map<String, String> getUserSelection() {
		String id = (String)pluginSelectionCombo.getData(pluginSelectionCombo.getItem(pluginSelectionCombo.getSelectionIndex()));Map<String, String> userInput = new HashMap<>();
		userInput.put("repositoryType", id);
		Map<String, String> pluginParameters = panels.get(id).getParameters();
		userInput.putAll(pluginParameters);
		return userInput;
	}
}
