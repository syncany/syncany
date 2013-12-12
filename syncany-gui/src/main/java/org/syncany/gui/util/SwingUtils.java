package org.syncany.gui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;


public class SwingUtils {
	private static Logger log = Logger.getLogger(SwingUtils.class.getSimpleName());
	
	public static final Color COLOR_INVALID = new Color(253,227,233);
	public static final Color COLOR_VALID = Color.WHITE;
    
	public static void maximize(JFrame jw){
		jw.setExtendedState(jw.getExtendedState() | Frame.MAXIMIZED_BOTH);
	}
	
	/**
	 * Centers the Window on the screen
	 * @param window
	 * @param packFrame
	 */
    public static void centerOnScreen(Window window, boolean packFrame) {
        if (packFrame) {
            window.pack();
        } else {
            window.validate();
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = window.getSize();

        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }

        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }

        
        window.setLocation(
            (screenSize.width - frameSize.width) / 2,
            (screenSize.height - frameSize.height) / 2
        );
    }
    
    public static void centerOnScreen(Window window) {
    	centerOnScreen(window, false);
    }
    

	public static Window getParentWindow(Component c) {
        Container container = c.getParent();
        while( container != null ) {
        	if( container instanceof Frame || container instanceof Dialog) {
        		return( (Window)container );
        	} 
            container = container.getParent();
        } 
        return(null);
	}

	public static void putOnTop(Window w) {
		w.setAlwaysOnTop(true);
		w.setAlwaysOnTop(false);
	}
	
	
    public static void dispatchOnSwingThread(Runnable r) {
	    if (SwingUtilities.isEventDispatchThread()) {
	    	r.run();
	    } else {
	    	try {
	    		SwingUtilities.invokeLater(r);
	    	} 
	    	catch (Exception e) {
	    		log.fine(Utilities.formatStackTrace(e));
	    	}
	    }
    }

    /**
     * Sets enabled property for the given button
     * @param button
     * @param b
     */
	public static void setEnable(final JComponent comp, final boolean b) {
		dispatchOnSwingThread(new Runnable() {
			@Override
			public void run() {
				comp.setEnabled(b);
			}
		});
	}

	public static Dimension getProportionalScreenDimension(double ratio) {
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		return new Dimension((int)(d.getWidth() * ratio), (int)(d.getHeight() * ratio));
	}

	public static void show(final Window frame) {
		dispatchOnSwingThread(new Runnable() {
			@Override
			public void run() {
				frame.setVisible(true);
			}
		});
	}
	
	 public static void setVisibleRowCount(JTable table, int rows) {
	        int height = 0;
	        for (int row = 0; row < rows; row++) {
	            height += table.getRowHeight(row);
	        }

	        table.setPreferredScrollableViewportSize(
				new Dimension(
						table.getPreferredScrollableViewportSize().width, 
					height
				)
			);
	    }
}
