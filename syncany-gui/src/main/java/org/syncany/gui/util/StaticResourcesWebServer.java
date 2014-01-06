package org.syncany.gui.util;

import java.util.logging.Logger;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.util.resource.Resource;

public class StaticResourcesWebServer {
	private static final Logger log = Logger.getLogger(StaticResourcesWebServer.class.getSimpleName());
	
	private static StaticResourcesWebServer instance;
	public static int port = 8081;
	
	private Server server;

	public static void main(String[] args) {
		new StaticResourcesWebServer().startService();
	}
	
	public void startService() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try{
					server = new Server(port);
				
					ContextHandler context0 = new ContextHandler();
			        context0.setContextPath("/");
			        ResourceHandler rh0 = new ResourceHandler();
			        rh0.setBaseResource( Resource.newResource(ClassLoader.getSystemResource("images")));
			        context0.setHandler(rh0);
			 
			        // Rinse and repeat the previous item, only specifying a different resource base.
			        ContextHandler context1 = new ContextHandler();
			        context1.setContextPath("/");   
			        ResourceHandler rh1 = new ResourceHandler();
			        rh1.setBaseResource( Resource.newResource(ClassLoader.getSystemResource("scripts")));
			        context1.setHandler(rh1);
			        
			        ContextHandlerCollection contexts = new ContextHandlerCollection();
			        contexts.setHandlers(new Handler[]{ context0, context1 });
			        
			        server.setHandler(contexts);
			        
					server.start();
					server.join();
				}
				catch (Exception e){
					log.warning("Exception "+ e.getMessage());
				}
			}
		}, "staticWebServer");
		t.start();
	}

	public void stopService() {
		try {
			server.stop();
		} catch (Exception e) {
			log.warning("Exception " + e.getMessage());
		}
	}

	public boolean isServiceStarted() {
		if (server == null) return false;
		return server.isStarted();
	}

	public boolean isServiceStoped() {
		return !isServiceStarted();
	}

	public static StaticResourcesWebServer getInstance() {
		if (instance == null) instance = new StaticResourcesWebServer();
		return instance;
	}
}
