package org.syncany.gui.messaging.webserver;

import java.util.logging.Logger;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.eclipse.jetty.util.resource.Resource;

/**
 * Static resources web server
 * Used to access python script and resources to python script
 * @author Vincent Wiencek <vwiencek@gmail.com>
 *
 */
public class StaticResourcesWebServer {
	private static final Logger log = Logger.getLogger(StaticResourcesWebServer.class.getSimpleName());

	private static StaticResourcesWebServer instance;
	public static int WEBSERVER_POR = 51601; 

	private Server server;

	public interface ServerStartedListener {
		public void serverStarted();
	}
	
	public void startService(final ServerStartedListener listener) {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					server = new Server(WEBSERVER_POR);
					server.addLifeCycleListener(new Listener() {
						@Override
						public void lifeCycleStarted(LifeCycle event) {
							listener.serverStarted();
						}

						@Override
						public void lifeCycleStopping(LifeCycle event) {
						}

						@Override
						public void lifeCycleStopped(LifeCycle event) {
						}

						@Override
						public void lifeCycleStarting(LifeCycle event) {
						}

						@Override
						public void lifeCycleFailure(LifeCycle event, Throwable cause) {
						}
					});

					ContextHandler context0 = new ContextHandler();
					context0.setContextPath("/scripts/");
					ResourceHandler rh0 = new ResourceHandler();
					rh0.setBaseResource(Resource.newResource(ClassLoader.getSystemResource("scripts")));
					context0.setHandler(rh0);

					ContextHandler context1 = new ContextHandler();
					context1.setContextPath("/images/");
					ResourceHandler rh1 = new ResourceHandler();
					rh1.setBaseResource(Resource.newResource(ClassLoader.getSystemResource("images")));
					context1.setHandler(rh1);

					ContextHandlerCollection contexts = new ContextHandlerCollection();
					contexts.setHandlers(new Handler[] { context0, context1 });

					server.setHandler(contexts);

					server.start();
					server.join();
				}
				catch (Exception e) {
					log.warning("Exception " + e.getMessage());
				}
			}
		}, "staticWebServer");
		t.start();
	}

	public void stopService() {
		try {
			server.stop();
			server.destroy();
		}
		catch (Exception e) {
			log.warning("Exception " + e.getMessage());
		}
	}

	public boolean isServiceStarted() {
		if (server == null) {
			return false;
		}

		return server.isStarted();
	}

	public boolean isServiceStoped() {
		return !isServiceStarted();
	}

	public static StaticResourcesWebServer getInstance() {
		if (instance == null) {
			instance = new StaticResourcesWebServer();
		}
		
		return instance;
	}
}
