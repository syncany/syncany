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

import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

/**
 * @author vincent
 *
 */
public class InitFtpOperationPanel extends PluginPanel {
	private static final long serialVersionUID = 562658489716174479L;

	private final JLabel hostNameLabel = new JLabel("Hostname :");
	private final JLabel userNameLabel = new JLabel("Username :");
	private final JLabel passwordLabel = new JLabel("Password :");
	private final JLabel pathLabel = new JLabel("Path :");
	private final JLabel portLabel = new JLabel("Port number :");

	private final JTextField hostNameTF = new JTextField();
	private final JTextField userNameTF = new JTextField();
	private final JPasswordField passwordTF = new JPasswordField();
	private final JTextField pathTF = new JTextField();
	private final JTextField portTF = new JTextField();
	private final JButton testFTPButton = new JButton("Test");

	/**
	 * Create the panel.
	 */
	public InitFtpOperationPanel() {
		initGui();
	}
	
	public void initGui(){
		setBorder(new TitledBorder("FTP Plugin"));
		setLayout(new MigLayout("", "[][grow]", "[][][][][][]"));
		
		add(hostNameLabel, "cell 0 0,alignx trailing");
		add(hostNameTF, "cell 1 0,growx");
		add(userNameLabel, "cell 0 1,alignx trailing");
		add(userNameTF, "cell 1 1,growx");
		add(passwordLabel, "cell 0 2,alignx trailing");
		add(passwordTF, "cell 1 2,growx");
		add(pathLabel, "cell 0 3,alignx trailing");
		add(pathTF, "cell 1 3,growx");
		add(portLabel, "cell 0 4,alignx trailing");
		add(portTF, "cell 1 4,growx");
		add(testFTPButton, "cell 0 5 2 1,alignx right");
	}

	@Override
	public Map<String, String> getPluginParameters() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("hostName", hostNameTF.getText());
		params.put("userName", userNameTF.getText());
		params.put("password", passwordTF.getText());
		params.put("path", pathTF.getText());
		params.put("port", portTF.getText());
		return params;
	}

}
