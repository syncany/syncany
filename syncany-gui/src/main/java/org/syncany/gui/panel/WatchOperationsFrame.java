package org.syncany.gui.panel;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import net.miginfocom.swing.MigLayout;

import org.syncany.gui.ClientCommandFactory;
import org.syncany.gui.util.SwingUtils;

public class WatchOperationsFrame extends JPanel implements ActionListener {
	private static final long serialVersionUID = 6018353272748605091L;
	private static final Logger log = Logger.getLogger(WatchOperationsFrame.class.getSimpleName());
	
	private final JScrollPane scrollPane = new JScrollPane();
	private final JTable table = new JTable();
	private WatchedTableModel wtm;
		
	private static List<String[]> list = new ArrayList<>();
	
	private void startBackgroundUpdate(){
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true){
					ClientCommandFactory.list();
					try {
						Thread.sleep(2000);
					}
					catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		t.start();
	}

	private void uu(String s){
		log.log(Level.INFO, "list="+s);
		if (s != null && s.length()>0){
			String[] a = s.split("@");
			
			list.clear();
			for (String ss : a){
				String[] aaa = ss.split(";");
				list.add(aaa);
			}
			((DefaultTableModel)table.getModel()).fireTableDataChanged();
		}
		
		try {
			Thread.sleep(1000);
		} 
		catch (InterruptedException e) {
			log.log(Level.WARNING, "InterruptedException", e);
		}
	}
	
	/**
	 * Create the application.
	 */
	public WatchOperationsFrame() {
		setBackground(Color.WHITE);
		
		wtm = new WatchedTableModel(list);
		table.setFillsViewportHeight(true);
		table.setBackground(Color.WHITE);
		table.setModel(wtm);
		
		
		final JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem stopWatchMI = new JMenuItem("Stop watch");
        JMenuItem startWatchMI = new JMenuItem("Start watch");
        stopWatchMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	stopWatch();
            }
        });
        startWatchMI.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
            	startWatch();
            }
        });
        popupMenu.add(startWatchMI);
        popupMenu.add(stopWatchMI);
        table.setComponentPopupMenu(popupMenu);
		startBackgroundUpdate();
		
		initGui();
	}
	
	 protected static Image createImage(String path, String description) {
        URL imageURL = WatchOperationsFrame.class.getResource("/"+path);
         
        if (imageURL == null) {
            System.err.println("Resource not found: " + path);
            return null;
        }
        else {
            return (new ImageIcon(imageURL, description)).getImage();
        }
    }

	/**
	 * Initialize the contents of the frame.
	 */
	private void initGui() {
		setBounds(100, 100, 667, 383);
		setLayout(new MigLayout("", "[45.00,grow,right]", "[grow]"));
		add(scrollPane, "cell 0 0,grow");
		scrollPane.setBorder(null);
		scrollPane.setViewportView(table);
		SwingUtils.setVisibleRowCount(table, 6);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	}
	
	public void stopWatch(){
		int idx = table.getSelectedRow();
		int idxModel = table.convertRowIndexToModel(idx);

		final String[] selectedRow = list.get(idxModel);
		
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				ClientCommandFactory.stopWatch(selectedRow[0]);
			}
		});
	}
	
	public void startWatch(){
		int idx = table.getSelectedRow();
		int idxModel = table.convertRowIndexToModel(idx);

		final String[] selectedRow = list.get(idxModel);
		
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				ClientCommandFactory.watch(selectedRow[1]);
			}
		});
	}
}
