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

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;

import org.syncany.gui.ClientCommandFactory;

/**
 * @author vincent
 *
 */
public class MainFrame extends JFrame implements WindowFocusListener, ActionListener {
	private static final long serialVersionUID = -7239614745212305595L;
	
	private JPanel contentPane;
	private final JButton quitButton = new JButton("Quit");
	private final JButton initButton = new JButton("Init");
	private final JButton connectButton = new JButton("Connect");
			

	/**
	 * Create the frame.
	 */
	public MainFrame() {
		initGui();
		setUndecorated(true);
		
		addWindowFocusListener(this);
		quitButton.addActionListener(this);
		initButton.addActionListener(this);
		connectButton.addActionListener(this);
	}
	
	private void initGui(){
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBackground(Color.WHITE);
		contentPane.setBorder(new LineBorder(new Color(192, 192, 192), 3));
		setContentPane(contentPane);
		contentPane.setLayout(new MigLayout("", "[grow]", "[][grow,fill][fill]"));
		
		JPanel panel_1 = new JPanel();
		panel_1.setBackground(Color.WHITE);
		contentPane.add(panel_1, "cell 0 0,grow");
		panel_1.setLayout(new MigLayout("ins 0", "[grow][100px:n][100px:n]", "[30px:n]"));
		
		panel_1.add(connectButton, "cell 1 0,grow");
		
		panel_1.add(initButton, "cell 2 0,grow");
		
		JPanel watchPanel = new WatchOperationsFrame();
		watchPanel.setBackground(Color.WHITE);
		contentPane.add(watchPanel, "cell 0 1,grow");
		watchPanel.setLayout(new MigLayout("", "[]", "[]"));
		
		JPanel panel_2 = new JPanel();
		panel_2.setBackground(Color.WHITE);
		contentPane.add(panel_2, "cell 0 2,grow");
		panel_2.setLayout(new MigLayout("ins 0", "[grow][100px:n,fill]", "[30px:n,fill]"));
		
		panel_2.add(quitButton, "cell 1 0");
	}


	@Override
	public void windowGainedFocus(WindowEvent e) {
		
	}

	@Override
	public void windowLostFocus(WindowEvent e) {
		setVisible(false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == quitButton){
			try{
				ClientCommandFactory.stopDaemon();
			}
			catch (Exception ex){
				
			}
			System.exit(0);
		}
		else if (e.getSource() == connectButton){
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {
						ConnectOperationFrame frame = new ConnectOperationFrame();
						frame.setVisible(true);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
		else if (e.getSource() == initButton){
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {
						InitOperationFrame panel = new InitOperationFrame();
						panel.setVisible(true);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
}
