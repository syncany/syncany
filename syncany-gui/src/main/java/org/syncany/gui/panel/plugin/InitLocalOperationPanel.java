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
package org.syncany.gui.panel.plugin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

import org.syncany.gui.panel.PluginPanel;
import org.syncany.gui.util.FileChooserHelper;
import org.syncany.gui.util.ImageResources.Image;

/**
 * @author vincent
 *
 */
public class InitLocalOperationPanel extends PluginPanel implements ActionListener {
	private static final long serialVersionUID = 4154765239652744563L;

	private final JTextField localRepoTextfield = new JTextField();
	private final JLabel localRepoLabel = new JLabel("Local repository :");
	private final JButton chooseFolderButton = new JButton(Image.FOLDER_SMALL.asIcon());
	
	
	/**
	 * Create the panel.
	 */
	public InitLocalOperationPanel() {
		chooseFolderButton.addActionListener(this);;
		
		initGui();
	}
	
	public void initGui(){
		setBorder(new TitledBorder("Local Plugin"));
		setLayout(new MigLayout("", "[][grow,fill][]", "[]"));
		
		add(localRepoLabel, "cell 0 0,alignx trailing");
		add(localRepoTextfield, "cell 1 0,growx");
		
		add(chooseFolderButton, "cell 2 0");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == chooseFolderButton){
			String folder = FileChooserHelper.show(this);
			localRepoTextfield.setText(folder);
		}
	}

	@Override
	public Map<String, String> getPluginParameters() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("path", localRepoTextfield.getText());
		return params;
	}
}
