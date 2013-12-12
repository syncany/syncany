package org.syncany.gui.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Class used to handle <code>Image</code> 
 * in the application
 * @author Vincent Wiencek
 *
 */
public class ImageResources {
	private static Logger log = Logger.getLogger(ImageResources.class.getSimpleName());

	public static String fileSep = System.getProperty("file.separator");
	
	//Map used to store already load
	public static Map<String, Icon> iconCache = new HashMap<String, Icon>();
    public static Map<String, BufferedImage> bufferedImageCache = new HashMap<String, BufferedImage>();
	
	public enum Image{
		//Language Flags
		TRAY("sync.png", 32),
        FOLDER_SMALL("folder_16.png", 16),
        FOLDER_MEDIUM("folder_32.png", 32),
        FOLDER_LARGE("folder.png", 512);
		
		private String fileName;
		private int width;
		private int height;
		
		private Image(String fileName, int size){
			this(fileName, size, size);
		}
		
		private Image(String fileName, int width, int height){
			this.fileName = fileName;
			this.height = height;
			this.width = width;
		}

		public String getValue(){return fileName;}
		
		public String path(){
			return "images/" + getValue(); 
		}
		
		public Icon asIcon(){
			return getIcon(this);
		}
		public BufferedImage asBufferedImage(){
			return getBufferedImage(this);
		}
		
		public String key() {return name();}
		public int getWidth() {return width;}
		public int getHeight() {return height;}
	}
	
	public static Icon getIcon(Image image) {
        if (!iconCache.containsKey(image.name())){
        	log.fine("trying to load icon in path " + image.path());
        	
        	URL url  = Utilities.getUrl(image.path());
        	
        	if (url != null){
        		iconCache.put(image.name(), new ImageIcon(url));
        	}
        }
        
        return iconCache.get(image.name());
    }
	
	public static BufferedImage getImage(String fileName){
		BufferedImage image = null;
		try {
			URL u = Utilities.getUrl(fileName);
			if (u != null){
				image = ImageIO.read(u.openStream());
			}
		} 
		catch (IOException e) {
			log.fine(""+e);
		} 
	    return image;
	}
    /** 
     * @param image
     * @return a <code>BufferedImage</code> corresponding
     * to the <code>Image</code> Object
     */
    public static BufferedImage getBufferedImage(Image image) {
    	if (!bufferedImageCache.containsKey(image.name())){
    		log.fine("trying to load image in path " + image.path());
        	
    		try {
    			URL url  = Utilities.getUrl(image.path());
    			
    			if (url != null){
    				bufferedImageCache.put(image.name(), ImageIO.read(url));
    			}
	    	} catch (IOException e) {
	    		log.fine(""+e);
	    	}
    	}
    	return bufferedImageCache.get(image.name());
    }
    
    public static BufferedImage getBufferedImage(Image _image, double ratio) {
    	BufferedImage image = getBufferedImage(_image);
    	int w = (int)(ratio * image.getWidth());
    	int h = (int)(ratio * image.getHeight());
    	return getBufferedImage(_image, w, h);
    }

    public static BufferedImage getBufferedImage(String imageName, int p_width, int p_height) {
    	BufferedImage image = getImage(imageName);
    	return getBufferedImage(image, p_width, p_height);
    }
    
    public static BufferedImage getBufferedImage(BufferedImage image, int p_width, int p_height) {
    	int thumbWidth = p_width;
		int thumbHeight = p_height;

		// Make sure the aspect ratio is maintained, so the image is not skewed
		double thumbRatio = (double) thumbWidth / (double) thumbHeight;
		int imageWidth = image.getWidth(null);
		int imageHeight = image.getHeight(null);
		double imageRatio = (double) imageWidth / (double) imageHeight;
		
		if (thumbRatio < imageRatio) {
			thumbHeight = (int) (thumbWidth / imageRatio);
		} else {
			thumbWidth = (int) (thumbHeight * imageRatio);
		}

		java.awt.Image im =  image.getScaledInstance(thumbWidth, thumbHeight, java.awt.Image.SCALE_SMOOTH);
		BufferedImage bi = new BufferedImage(im.getWidth(null),im.getHeight(null),BufferedImage.TYPE_INT_RGB);
		Graphics bg = bi.getGraphics();
		bg.setColor(Color.WHITE);
		bg.fillRect(0, 0, im.getWidth(null),im.getHeight(null));
		bg.drawImage(im, 0, 0, null);
		bg.dispose();
		     
		return bi;
    }
    
    private static BufferedImage getBufferedImage(Image _image, int p_width, int p_height) {
    	BufferedImage image = getBufferedImage(_image);
    	
		return getBufferedImage(image, p_width, p_height);
	}
}