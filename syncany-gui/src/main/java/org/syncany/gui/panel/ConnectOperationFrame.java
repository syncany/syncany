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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

import org.syncany.gui.ClientCommandFactory;
import org.syncany.gui.util.FileChooserHelper;
import org.syncany.gui.util.ImageResources.Image;
import org.syncany.gui.util.SwingUtils;

/**
 * @author vincent
 *
 */
public class ConnectOperationFrame extends JFrame implements ActionListener {
	private static final long serialVersionUID = -8279495882104534328L;
	
	private final JButton browseButton = new JButton(Image.FOLDER_SMALL.asIcon());
	private final JButton cancelButton = new JButton("Cancel");
	private final JButton okButton = new JButton("Connect");
	
	private final JTextField folderTF = new JTextField();
	private final JTextField urlTF = new JTextField();
	
	private final JLabel folderLabel = new JLabel("Folder :");
	private final JLabel urlLabel = new JLabel("URL :");
	private final JPanel buttonPanel = new JPanel();
	private final JPanel commonPanel = new JPanel();
	
	/**
	 * Create the frame.
	 */
	public ConnectOperationFrame() {
		cancelButton.addActionListener(this);
		okButton.addActionListener(this);
		browseButton.addActionListener(this);
		initGui();
		
		SwingUtils.centerOnScreen(this, true);
	}
	
	public void initGui(){
		commonPanel.setLayout(new MigLayout("", "[::100px,grow][300px:n,grow,fill][]", "[][][grow]"));
		commonPanel.setBorder(new TitledBorder("Parameters"));
		commonPanel.add(folderLabel, "cell 0 0,alignx trailing");
		commonPanel.add(folderTF, "cell 1 0,growx");
		commonPanel.add(browseButton, "cell 2 0");
		commonPanel.add(urlLabel, "cell 0 1,alignx trailing");
		commonPanel.add(urlTF, "cell 1 1 2 1,growx");

		buttonPanel.setLayout(new MigLayout("ins 0", "[grow][100px:n,fill][100px:n,fill]", "[30px:n,grow,fill]"));
		buttonPanel.add(cancelButton, "cell 1 0");
		buttonPanel.add(okButton, "cell 2 0");

		getContentPane().setLayout(new MigLayout("", "[grow,fill]", "[][30px:n,grow,bottom]"));
		getContentPane().add(commonPanel, "cell 0 0,grow");
		getContentPane().add(buttonPanel, "cell 0 1,growx,aligny bottom");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancelButton){
			this.setVisible(false);
		}
		else if (e.getSource() == okButton){
			String url = urlTF.getText();
			String folder = folderTF.getText();
			ClientCommandFactory.connect(url, folder);
		}
		else if (e.getSource() == browseButton){
			String folder = FileChooserHelper.show(this);
			folderTF.setText(folder);
		}
	}
}
