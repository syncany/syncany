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

import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;

import org.syncany.gui.ClientCommandFactory;
import org.syncany.gui.PluginGui;
import org.syncany.gui.util.FileChooserHelper;
import org.syncany.gui.util.ImageResources.Image;
import org.syncany.gui.util.PasswordChecker;
import org.syncany.gui.util.SwingUtils;

/**
 * @author vincent
 *
 */
public class InitOperationFrame extends JFrame implements ActionListener, ItemListener, DocumentListener {
	private static final long serialVersionUID = -8279495882104534328L;
	private static final Logger log = Logger.getLogger(InitOperationFrame.class.getSimpleName());
	
	private final JLabel folderLabel = new JLabel("Folder");
	private final JLabel pluginLabel = new JLabel("Storage");
	private final JLabel lblNewLabel_1 = new JLabel("Password");
	private final JLabel lblNewLabel_2 = new JLabel("Encryption");

	private JPanel commonPanel;
	private JPanel buttonPanel;
	private JPanel pluginPanel;

	private final JTextField folderTF = new JTextField();
	private final JPasswordField passwordField = new JPasswordField();

	private final JButton browseButton = new JButton(Image.FOLDER_SMALL.asIcon());
	private final JButton cancelButton = new JButton("Cancel");
	private final JButton okButton = new JButton("Connect");
	private final JComboBox<PluginGui> comboBox = new JComboBox<>();
	private final JCheckBox chckbxNewCheckBox = new JCheckBox("");
	private final JProgressBar passwordStrengthPG = new JProgressBar();
	
	/**
	 * Create the frame.
	 */
	public InitOperationFrame() {
		cancelButton.addActionListener(this);
		okButton.addActionListener(this);
		browseButton.addActionListener(this);
		
		pluginPanel = new JPanel(new CardLayout());
		
		for (PluginGui plugin : PluginGui.getAvailablePlugins()){
			try{
				PluginPanel pan = (PluginPanel)Class.forName(plugin.getClassName()).getConstructor().newInstance();
				pluginPanelStore.put(plugin.getCode(), pan);
				comboBox.addItem(plugin);
				pluginPanel.add(plugin.getCode(), pan);
			}
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
					| SecurityException | ClassNotFoundException e) {
				log.warning("Error " + e);
			}
		}
		
		comboBox.addItemListener(this);
		chckbxNewCheckBox.addItemListener(this);
		passwordField.getDocument().addDocumentListener(this);
		passwordStrengthPG.setMaximum(100);
		passwordStrengthPG.setValue(0);
		
		initGui();
		togglePassword();
		updatePluginPanel();
		SwingUtils.centerOnScreen(this, true);
	}
	
	public void initGui(){
		getContentPane().setLayout(new MigLayout("", "[grow,fill]", "[fill][grow,fill][30px:n,fill]"));
		
		commonPanel = getCommonPanel();
		buttonPanel = getButtonPanel();
		
		getContentPane().add(commonPanel, "cell 0 0,grow");
		getContentPane().add(pluginPanel, "cell 0 1,grow");
		getContentPane().add(buttonPanel, "cell 0 2,grow");
	}
	
	private JPanel getButtonPanel(){
		JPanel p = new JPanel();
		
		p.setLayout(new MigLayout("ins 0", "[grow,fill][100px:n,fill][100px:n,fill]", "[grow,fill]"));
		p.add(cancelButton, "flowx,cell 1 0");
		p.add(okButton, "cell 2 0");
		
		return p;
	}
	
	private JPanel getCommonPanel(){
		JPanel p = new JPanel();
		p.setBorder(new TitledBorder("Common parameters"));
		p.setLayout(new MigLayout("", "[150px:n][300px:n,grow,fill][]", "[25px:n][25px:n][25px:n]0[][25px:n]"));
		p.add(folderTF, "cell 1 0,growy");
		p.add(browseButton, "cell 2 0");
		
		p.add(lblNewLabel_2, "cell 0 1");
		p.add(chckbxNewCheckBox, "cell 1 1 2 1");
		p.add(lblNewLabel_1, "cell 0 2 1 2");
		p.add(passwordField, "cell 1 2 2 1,growy");
		
		p.add(passwordStrengthPG, "cell 1 3 2 1,grow");
		p.add(pluginLabel, "cell 0 4");
		p.add(folderLabel, "flowx,cell 0 0");
		p.add(comboBox, "cell 1 4 2 1");

		return p;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancelButton){
			this.dispose();
		}
		else if (e.getSource() == okButton){
			final PluginGui pgui = comboBox.getItemAt(comboBox.getSelectedIndex());
			final String folder = folderTF.getText();
			final PluginPanel pluginPanel = pluginPanelStore.get(selectedPlugin);
			
			SwingUtils.dispatchOnSwingThread(new Runnable() {
				@Override
				public void run() {
					handleInitOperation(pgui.getCode(), folder, pluginPanel.getPluginParameters());
				}
			});
		}
		else if (e.getSource() == browseButton){
			final JFrame thizz = this;
			SwingUtils.dispatchOnSwingThread(new Runnable() {
				@Override
				public void run() {
					String folder = FileChooserHelper.show(thizz);
					folderTF.setText(folder);
				}
			});
		}
	}

	private void handleInitOperation(String plugin, String folder, Map<String, String> params) {
		ClientCommandFactory.init(folder, plugin, chckbxNewCheckBox.isSelected() ? passwordField.getText() : null, params);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == chckbxNewCheckBox){
			togglePassword();
		}
		else if (e.getSource() == comboBox){
			updatePluginPanel();
		}
	}
	
	private void updatePluginPanel() {
		PluginGui plugin = comboBox.getItemAt(comboBox.getSelectedIndex());
		CardLayout cl = (CardLayout)(pluginPanel.getLayout());
		selectedPlugin = plugin.getCode();
		cl.show(pluginPanel, selectedPlugin);
	}

	private String selectedPlugin;
	private Map<String, PluginPanel> pluginPanelStore = new HashMap<>();

	private void togglePassword() {
		if (chckbxNewCheckBox.isSelected()){
			passwordField.setEnabled(true);
		}
		else{
			passwordField.setText("");
			passwordField.setEnabled(false);
		}
	}

	@Override
	public void changedUpdate(DocumentEvent arg0) {	
		updatePG();
	}

	@Override
	public void insertUpdate(DocumentEvent arg0) {
		updatePG();
	}

	@Override
	public void removeUpdate(DocumentEvent arg0) {
		updatePG();
	}
	
	public void updatePG(){
		char[] pwd = passwordField.getPassword();
		int score = PasswordChecker.check(pwd);
		passwordStrengthPG.setValue(score);
	}
}