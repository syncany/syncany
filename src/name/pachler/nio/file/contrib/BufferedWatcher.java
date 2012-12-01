/*
 * Copyright 2008-2011 Uwe Pachler
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. This particular file is
 * subject to the "Classpath" exception as provided in the LICENSE file
 * that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package name.pachler.nio.file.contrib;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import name.pachler.nio.file.ClosedWatchServiceException;
import name.pachler.nio.file.FileSystems;
import name.pachler.nio.file.Path;
import name.pachler.nio.file.Paths;
import name.pachler.nio.file.StandardWatchEventKind;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchEvent.Kind;
import name.pachler.nio.file.WatchEvent.Modifier;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.WatchService;
import name.pachler.nio.file.ext.ExtendedWatchEventKind;
import name.pachler.nio.file.impl.LinuxMovePathWatchEvent;

/**
 * Allows to easily add watches to file system folders.
 * 
 * <p>By delaying the events for a few milliseconds, it can match rename
 * from/to events and hence fire artificial an artificial ENTRY_RENAME_FROM_TO
 * event. It furthermore delays ENTRY_CREATE and ENTRY_MODIFY events until
 * the respective files do not change anymore (esp. important for large files).
 * 
 * <p>TODO
 *  - copying a folder does not fire the new child events
 *  - allow flag "fire child events" (on rename, or copy)
 *  - allow own user Kind[] array
 *  - file filter (to ignore files)
 * 
 * @author Philipp C. Heckel
 */
public class BufferedWatcher {
    private static final Logger logger = Logger.getLogger(BufferedWatcher.class.getSimpleName());    
    
    public static final int DEFAULT_DELAY = 500;
    public static final boolean DEFAULT_KILL_SOURCE_EVENTS = true;
    
    private static final Kind[] KINDS = new Kind[] { 
        StandardWatchEventKind.ENTRY_CREATE, 
        StandardWatchEventKind.ENTRY_DELETE, 
        StandardWatchEventKind.ENTRY_MODIFY, 
        ExtendedWatchEventKind.ENTRY_RENAME_FROM,
        ExtendedWatchEventKind.ENTRY_RENAME_TO
    };
    
    private static final Kind[] OSX_KINDS = new Kind[] { 
        StandardWatchEventKind.ENTRY_CREATE, 
        StandardWatchEventKind.ENTRY_DELETE, 
        StandardWatchEventKind.ENTRY_MODIFY, 
    };
    
    /**
     * TODO: Have someone to validate this ostype check to see if this is 
     * acceptable solution or if this should be implemented differently.
     * aammi@villingaholt.nu. Used in addWatch method.
     */
    private static final int OSTYPE_LINUX = 1;
    private static final int OSTYPE_WINDOWS = 2;
    private static final int OSTYPE_BSD = 3;
    private static final int ostype;
    static {
        String osName = System.getProperty("os.name");
        
        if (osName.contains("Windows")) {
            ostype = OSTYPE_WINDOWS;
        }
        else if(osName.equals("Linux")) {
            ostype = OSTYPE_LINUX;
        }
        else if(osName.equals("Mac OS X") || osName.equals("FreeBSD")) {
            ostype = OSTYPE_BSD;
        }
        else {
            throw new RuntimeException("Unsupported operating system: "+osName);
        }
    }
    
    private static final Modifier[] MODIFIERS = new Modifier[] { 
        // ExtendedWatchEventModifier.FILE_TREE // Windows only!
    };
    
    
    // Watch stuff
    private WatchService watchService;    
    private Thread worker;
    
    private Map<File, WatchNode> fileNodeMap;
    private Map<WatchKey, WatchNode> keyNodeMap;

    // Delay stuff
    private final List<TimedWatchEvent> eventQueue;
    private Timer timer;
    private int delay;
    private boolean killSourceEvents;    
        
    
    public BufferedWatcher() {
        this(DEFAULT_DELAY, DEFAULT_KILL_SOURCE_EVENTS);
    }

    public BufferedWatcher(int delay) {
        this(delay, DEFAULT_KILL_SOURCE_EVENTS);
    }

    public BufferedWatcher(int delay, boolean killSourceEvents) {       
        super();

        // Watch stuff
        this.watchService = FileSystems.getDefault().newWatchService();
        this.worker = null; // cp. start()

        this.fileNodeMap = new HashMap<File, WatchNode>();
        this.keyNodeMap = new HashMap<WatchKey, WatchNode>();
        
        // Delay stuff
        this.timer = null; // cp. start()
        this.delay = delay;
        this.killSourceEvents = killSourceEvents;
        this.eventQueue = new ArrayList<TimedWatchEvent>();
    }    

    public synchronized void start() {
        if (worker != null) {
            return;
        }
        
        // Worker
        worker = new Thread(new WatchWorker(), "BufWatchWorker");
        worker.start();
        
        
        // The delay for the timer should be less than the actual delay, because
        // if it was, the actual delay could be double the time
        int timerDelay = Math.round((float) delay / 2); 
        
        // Timer        
        timer = new Timer("BufWatchTimer");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { 
                try {
                    fireDelayedEvents();
                } 
                catch (Exception e) { 
                    logger.log(Level.WARNING, "Error while processing delayed events. IGNORING.", e);
                }
            }
        }, 0, timerDelay);        
    }    

    public synchronized void stop() {
        if (worker == null) {
            return;
        }
        
        // Worker
        worker.interrupt();
        worker = null;
        
        // Timer
        timer.cancel();
        timer = null;        
    }
    
    public synchronized boolean isActive() {
        return worker != null && timer != null;
    }

    public int getDelay() {
        return delay;
    }
        
    private void fireDelayedEvents() {
        long now = System.currentTimeMillis();
        
        synchronized (eventQueue) {
            while (eventQueue.size() > 0) {
                TimedWatchEvent e = eventQueue.get(0);

                if (e.getTimestamp()+delay >= now) {
                    break;
                }

                // Take event
                eventQueue.remove(0);    
                //System.out.println("delayed event "+e.getEvent().kind().name()+" "+e.getEvent().context());
                
                // For modify/create events, check if file size has changed
                if (e.getEvent().kind() == StandardWatchEventKind.ENTRY_CREATE
                        || e.getEvent().kind() == StandardWatchEventKind.ENTRY_MODIFY) {
                    
                    File file = getEventFile(e.getEvent(), e.getParentKey());
                    
                    // File might not exist anymore
                    if (file == null || file.length() == 0) {
                        if (logger.isLoggable(Level.WARNING)) {
                            logger.log(Level.WARNING, "Purging delayed event. Watch key invalid. File vanished? Event = {0}, Key ={1}", new Object[]{e.getEvent(), e.getParentKey()});
                        }
                    }
                    
                    // Check if changed; re-add if it did change!
                    /*if (e.getFilesize() != file.length()) {
                        e.setTimestamp(now);
                        e.setFilesize(file.length());
                        
                        System.out.println("re-add");
                        eventQueue.add(e);
                    
                        continue;
                    }*/
                }               

                // If is 'moved from', look for a matching 'moved to' event (via cookie)
                // Note: this assumes that the FROM event always comes before the TO event
                if (e.getEvent().kind() == ExtendedWatchEventKind.ENTRY_RENAME_FROM) {
                    TimedWatchEvent movedFromEvent = e;                    
                    TimedWatchEvent movedToEvent = null;
                    
                    // Look in the queue for files with the same cookie
                    // Take the closest one with the same cookie
                    cookieSearch: for (TimedWatchEvent potentialMovedToEvent : eventQueue) {
                        if (potentialMovedToEvent.getEvent().kind() == ExtendedWatchEventKind.ENTRY_RENAME_TO
                            && movedFromEvent.getCookie() == potentialMovedToEvent.getCookie()) {

                            movedToEvent = potentialMovedToEvent;
                            break cookieSearch;
                        }
                    }

                    // 'moved to'-event found
                    if (movedToEvent != null) {
                        eventQueue.remove(movedToEvent);

                        // Fire source events
                        if (!killSourceEvents) {
                            processEvent(movedFromEvent);
                            processEvent(movedToEvent);
                        }

                        // Fire artificial from/to event
                        processEvent(new ExtendedWatchEvent(e.getParentKey(), new RenameWatchEvent(movedFromEvent, movedToEvent)));                        
                        continue;
                    }
                }

                // Default behavior: fire event!
                processEvent(e);
            }
        }
    }
        
    
    public synchronized WatchKey addWatch(File file, boolean recursive, WatchListener listener) throws IOException {        
        Path watchedPath = Paths.get(file.getAbsolutePath()); 
        
        /** 
         * TODO: Have somone to validate if this is acceptable solution or
         * if this should be implemented differently.sammi@villingaholt.nu
         */
        WatchKey key = null;
        if(ostype == OSTYPE_BSD){
            key = watchedPath.register(watchService, OSX_KINDS, MODIFIERS);
        }else{
            key = watchedPath.register(watchService, KINDS, MODIFIERS);
        }
        
        // Add my own node
        WatchNode node = new WatchNode(key, file);

        node.setListener(listener);
        node.setRecursive(recursive);
        
        // Add my own key mappings
        fileNodeMap.put(file, node);
        keyNodeMap.put(key, node);

        // Add myself to my parent (if it exists)
        File parentFile = file.getParentFile();
        WatchNode parentNode = fileNodeMap.get(parentFile);
                
        node.setParent(parentNode);
            
        if (parentNode != null) {
            parentNode.getChildren().put(key, node);
        }                

        // Recursive?
        if (recursive) {
            for (File child : file.listFiles()) {
                if (!child.isDirectory()) {
                    continue;
                }

                addWatch(child, recursive, listener);            
            }
        }
        
        return key;        
    }
    
    public synchronized void removeWatch(File file) {
        removeWatch(fileNodeMap.get(file));
    }
    
    public synchronized void removeWatch(WatchKey key) {
        removeWatch(keyNodeMap.get(key));
    }    
    
    /**
     * Note: this is designed very carefully since everything could be null.
     */
    private synchronized void removeWatch(WatchNode node) {     
        if (node == null) {
            logger.warning("Cannot remove watch. Node is null.");
            return;
        }
        
        WatchKey key = node.getKey();
        File file = node.getPath();
                
        if (key != null && key.isValid()) {
            key.cancel();
        }
                
        if (key != null) {
            keyNodeMap.remove(key);
        }
                
        if (file != null) {
            fileNodeMap.remove(file);
        }
        
        // Remove from parent
        if (node.getParent() != null) {
            node.getParent().getChildren().remove(key);
        }
        
        // Cancel children
        if (key != null) {
            List<WatchNode> childNodes = new ArrayList<WatchNode>(node.getChildren().values());

            if (childNodes != null) {
                // Note: Don't do a for-loop here [concurrent modification]
                while (childNodes.size() > 0) {
                    removeWatch(childNodes.get(0));
                }
            }
        }
    }
 
    private void processEvent(ExtendedWatchEvent xe) {
        WatchNode parentNode = keyNodeMap.get(xe.getParentKey());
        WatchKey parentKey = xe.getParentKey();
        WatchEvent event = xe.getEvent();        
        
        if (parentNode == null || parentKey == null || event == null) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Cannot process event {0}. Invalid values: parentNode = {1}, parentKey = {2}, event = {3}. IGNORING.", new Object[]{xe, parentNode, parentKey, event});            
            }
            
            return;
        }
        
        if (event.kind() == StandardWatchEventKind.ENTRY_CREATE
            || event.kind() == ExtendedWatchEventKind.ENTRY_RENAME_TO) {
            
            File file = getEventFile(event, xe.getParentKey());
            
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "{0} {1}", new Object[]{event.kind().name(), file});
            }                    
            
            if (file.isDirectory() && parentNode != null && parentNode.isRecursive()) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "This is a directory. Adding new watch ...");
                }

                try {
                    addWatch(file, parentNode.isRecursive(), parentNode.getListener());
                }
                catch (IOException ex) {
                    logger.log(Level.SEVERE, "Could not add log to "+file+". IGNORING.", ex);
                }
            }             
        }

        else if (event.kind() == StandardWatchEventKind.ENTRY_MODIFY) {
            File file = getEventFile(event, xe.getParentKey());

            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "{0} {1}", new Object[]{event.kind().name(), file});
            }   
            
            // Nothing to do.
        }
                
        else if (event.kind() == StandardWatchEventKind.ENTRY_DELETE
            || event.kind() == ExtendedWatchEventKind.ENTRY_RENAME_FROM) {
            
            File file = getEventFile(event, parentKey);

            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "{0} {1}", new Object[]{event.kind().name(), file});
            }
            
            if (fileNodeMap.containsKey(file)) {
                removeWatch(file);
            }
        }

        else if (event.kind() == ExtendedWatchEventKind.KEY_INVALID) {
            removeWatch(parentKey);
        }
        
        else if (event.kind() == RenameWatchEventKind.ENTRY_RENAME_FROM_TO) {
            RenamePathContext renameContext = (RenamePathContext) event.context();
            ExtendedWatchEvent fromEvent = renameContext.getFromEvent();
            ExtendedWatchEvent toEvent = renameContext.getToEvent();            
            File fromFile = getEventFile(fromEvent.getEvent(), fromEvent.getParentKey());
            File toFile = getEventFile(toEvent.getEvent(), toEvent.getParentKey());
            
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "{0} {1} -> {2}", new Object[]{event.kind().name(), fromFile, toFile});
            }            
            
            if (fileNodeMap.containsKey(fromFile)) {
                updateMoveFileMaps(fromFile, toEvent.getParentKey(), toFile);                
            }
            
            if (toFile.isDirectory() && parentNode != null && parentNode.isRecursive()) {
                try {
                    addWatch(toFile, parentNode.isRecursive(), parentNode.getListener());
                }
                catch (IOException ex) {
                    logger.log(Level.SEVERE, "Could not add log to "+toFile+". IGNORING.", ex);
                }
            }
            
        }

        // NOW USER PROCESSING    
        if (parentNode != null && parentNode.getListener() != null) {
            parentNode.getListener().watchEventOccurred(parentKey, event);
        }
    }
    
    private synchronized void updateMoveFileMaps(File fromFile, WatchKey toParentKey, File toFile) { 
        System.err.println("updating file maps : from "+fromFile +" to "+toFile);
        
        // Update given key
        WatchNode moveNode = fileNodeMap.get(fromFile);       
        
        if (moveNode == null) {
            logger.log(Level.WARNING, "WatchKey for file {0} not found. IGNORING.", fromFile);
            return;
        }
        
        // Update 
        moveNode.setPath(toFile);

        // Remove from old parent
        if (moveNode.getParent() != null) {
            moveNode.getParent().getChildren().remove(moveNode.getKey());
        }
                
        // Add to new parent
        WatchNode toParentNode = keyNodeMap.get(toParentKey);
        
        toParentNode.getChildren().put(moveNode.getKey(), moveNode);
        moveNode.setParent(toParentNode);        
        
        // Update maps
        fileNodeMap.remove(fromFile);
        fileNodeMap.put(toFile, moveNode);
        
        // Update children
        // Note: the ArrayList constructor is required to copy the list (since it might be modified 
        //       in the recursive call below.
        Collection<WatchNode> childNodes = new ArrayList<WatchNode>(moveNode.getChildren().values()); 
        
        if (childNodes != null) {
            for (WatchNode childNode : childNodes) {
                File childFromFile = childNode.getPath();//keyFileMap.get(childKey);
                File childToFile = new File(toFile.getAbsolutePath() + childFromFile.getAbsolutePath().substring(fromFile.getAbsolutePath().length()));                
                WatchKey childToParentKey = fileNodeMap.get(childToFile.getParentFile()).getKey();    
                
                updateMoveFileMaps(childFromFile, childToParentKey, childToFile);
            }
        }        
    }
    
    public synchronized File getEventFile(WatchEvent<?> event, WatchKey key) {
        if (key == null || event == null) {
            return null;
        }
        
        WatchNode node = keyNodeMap.get(key);
        
        if (node == null) {
            return null;
        }
        
        File filePath = node.getPath();
        return new File(filePath.getAbsolutePath() + File.separator + ((Path) event.context()));
    }   
    
    public synchronized WatchKey getRootKey(WatchKey key) {
        WatchNode rootNode = keyNodeMap.get(key);
        
        if (rootNode == null) {
            return null;
        }
        
        while (rootNode.getParent() != null) {
            rootNode = rootNode.getParent();
        }
        
        return rootNode.getKey();
    }

    private class WatchWorker implements Runnable {
        @Override
        public void run() {
            while (true) {
                // take() will block until a file has been created/deleted
                WatchKey parentKey;

                try {
                    parentKey = watchService.take();
                }
                catch (InterruptedException ix) {
                    break;
                }
                catch (ClosedWatchServiceException cwse) {
                    // other thread closed watch service
                    System.out.println("watch service closed, terminating.");
                    break;
                }

                // get list of events from key
                List<WatchEvent<?>> events = parentKey.pollEvents();
                parentKey.reset();
                
                for (WatchEvent event : events) {
                    //System.out.println("event : "+event.kind().name() +"  "+event.context());
                    
                    // Make cookie (for rename from/to events)
                    long timestamp = System.currentTimeMillis();
                    int cookie = 0;
                    long filesize = 0;
                    
                    // TODO this only works on Linux so far!!!
                    // cp. https://sourceforge.net/projects/jpathwatch/forums/forum/888207/topic/4538927
                    if (event instanceof LinuxMovePathWatchEvent) {
                        LinuxMovePathWatchEvent moveEvent = (LinuxMovePathWatchEvent) event;
                        cookie = moveEvent.getCookie();
                    }
                    
                    // Record file size (for CLOSE_WRITE checking)
                    if (event.kind() == StandardWatchEventKind.ENTRY_CREATE 
                            || event.kind() == StandardWatchEventKind.ENTRY_MODIFY) {
                        
                        File file = getEventFile(event, parentKey);
                        filesize = file.length();
                        
                        // Remove old modify events with smaller file sizes
                        // so that only one modify event is thrown; bug #805210
                        int i = 0;
        
                        while (i < eventQueue.size()) {
                            TimedWatchEvent potentialModifyEvent = eventQueue.get(i);

                            if (potentialModifyEvent.getEvent().kind() == StandardWatchEventKind.ENTRY_MODIFY
                                    && potentialModifyEvent.getParentKey().equals(parentKey)
                                    && potentialModifyEvent.getEvent().context().equals(event.context())) {
                        
                                //System.out.println("remove "+potentialModifyEvent);
                                eventQueue.remove(i);
                            }
                            else {
                                i++;    
                            }
                        }
                    }
                    
                    eventQueue.add(new TimedWatchEvent(parentKey, event, timestamp, cookie, filesize));
                }
                
                fireDelayedEvents();
                //parentKey.reset();
            }
        }
    }    
    
    public synchronized void printMaps() {
        for (Map.Entry<File, WatchNode> m : fileNodeMap.entrySet())
            logger.severe("fileNodeMap: "+m.getKey()+ " = "+m.getValue());

        for (Map.Entry<WatchKey, WatchNode> m : keyNodeMap.entrySet())
            logger.severe("keyNodeMap: "+m.getKey()+ " = "+m.getValue());

        
        logger.severe("---");
    }      
    
    private class TimedWatchEvent extends ExtendedWatchEvent {
        private long timestamp;
        private int cookie;
        private long filesize;

        public TimedWatchEvent(WatchKey parentKey, WatchEvent event, long timestamp, int cookie, long filesize) {
            super(parentKey, event);
            this.timestamp = timestamp;
            this.cookie = cookie;
            this.filesize = filesize;
        }        

        public long getTimestamp() {
            return timestamp;
        }

        private void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }        

        public int getCookie() {
            return cookie;
        }

        public long getFilesize() {
            return filesize;
        }                
        
        private void setFilesize(long filesize) {
            this.filesize = filesize;
        }        

        @Override
        public boolean equals(Object obj) {
            TimedWatchEvent o = (TimedWatchEvent) obj;
            
            return o.getParentKey().equals(getParentKey())
                && o.getEvent().equals(getEvent())
                && o.getTimestamp() == getTimestamp()
                && o.getCookie() == getCookie()                   
                && o.getFilesize() == getFilesize();
        }
    }    
    
    private class WatchNode {
        private WatchKey key;
        private File path;
        private WatchListener listener;
        private boolean recursive;

        private WatchNode parent;
        private Map<WatchKey, WatchNode> children;

        public WatchNode(WatchKey key, File path) {
            this.key = key;
            this.path = path;
            this.parent = null;
            this.children = new HashMap<WatchKey, WatchNode>();
        }

        public boolean isRecursive() {
            return recursive;
        }

        public void setRecursive(boolean recursive) {
            this.recursive = recursive;
        }

        public WatchListener getListener() {
            return listener;
        }

        public void setListener(WatchListener listener) {
            this.listener = listener;
        }       
        
        public void setParent(WatchNode parent) {
            this.parent = parent;
        }

        public File getPath() {
            return path;
        }

        public void setPath(File path) {
            this.path = path;
        }        
        
        public Map<WatchKey, WatchNode> getChildren() {
            return children;
        }

        public WatchKey getKey() {
            return key;
        }

        public WatchNode getParent() {
            return parent;
        }

        @Override
        public String toString() {
            return "WatchKeyNode{" + "key=" + key + ", children=" + children + '}';
        }                
    }

}