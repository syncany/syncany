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

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

/**
 * @author vincent
 *
 */
public class InitAmazonS3OperationPanel extends PluginPanel {
	private static final long serialVersionUID = 4154765239652744563L;

	private JTextField accessKeyTF = new JTextField();
	private JTextField secretKeyTF = new JTextField();
	private JTextField bucketTF = new JTextField();
	private JTextField locationTF = new JTextField();

	private final JLabel accessKeyLabel = new JLabel("Access Key");
	private final JLabel secretKeyLabel = new JLabel("Secret Key");
	private final JLabel bucketLabel = new JLabel("Bucket");
	private final JLabel locationLabel = new JLabel("Location");

	/**
	 * Create the panel.
	 */
	public InitAmazonS3OperationPanel() {
		initGui();
	}
	
	public void initGui(){
		setBorder(new TitledBorder("Local Plugin"));
		setLayout(new MigLayout("", "[][grow]", "[][][][]"));
		
		add(accessKeyLabel, "cell 0 0,alignx trailing");
		add(accessKeyTF, "cell 1 0,growx");
		add(secretKeyLabel, "cell 0 1,alignx trailing");
		add(secretKeyTF, "cell 1 1,growx");
		add(bucketLabel, "cell 0 2,alignx trailing");
		add(bucketTF, "cell 1 2,growx");
		add(locationLabel, "cell 0 3,alignx trailing");
		add(locationTF, "cell 1 3,growx");
	}

	@Override
	public Map<String, String> getPluginParameters() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("location", locationTF.getText());
		params.put("accessKey", accessKeyTF.getText());
		params.put("secretKey", secretKeyTF.getText());
		params.put("bucket", bucketTF.getText());
		return params;
	}
}
