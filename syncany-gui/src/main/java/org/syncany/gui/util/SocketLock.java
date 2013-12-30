package org.syncany.gui.util;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class SocketLock {
	private static final Logger log = Logger.getLogger(SocketLock.class.getSimpleName());
    private int port;
    private Socket socket;

    public SocketLock() {
        this(49162); // create an interlock using a socket
    }

    public SocketLock(int port) {// use lowest private port
        this.port = port;
    }

    public synchronized void lock() throws Exception {
        if (socket == null) {
            socket = new Socket();
            socket.bind(new InetSocketAddress("127.0.0.1", port));
        }
        else{
        	if (!socket.isClosed())
        		throw new Exception("socket not closed");
        }
    }

    public synchronized void free() {
        try {
            socket.close();
            socket = null;
        } 
        catch (Exception e) {
        	log.log(Level.WARNING,"Socket, exceptions" + e);
        }
    }

    @Override
    public String toString() {
        return "Interlock on port " + port + (socket.isBound() ? " ours" : " theirs");
    }
}