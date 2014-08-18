package org.syncany.gui.config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import org.syncany.gui.Launcher;

/**
 * http://stackoverflow.com/questions/120797/how-do-i-set-the-proxy-to-be-used-by-the-jvm
 * @author Vincent Wiencek
 *
 */
public class ProxyController {
	private static final Logger log = Logger.getLogger(Launcher.class.getSimpleName());
	private static ProxyController instance;

	public enum ProxyType {
		NONE, DEFAULT, PROXY;
	}
	
	public enum ProxyAuthType {
		HTTP, SOCKET4, SOCKET5;
	}

	private ProxyType type;
	private ProxyAuthType authType;
	private String proxyHost;
	private String proxyPort;
	private String proxyUsername;
	private String proxyPassword;
	private Boolean proxyUseAuth;

	public ProxyController initProxy(ApplicationConfiguration ac) {
		log.info("Proxy initialisation");

		setProxyHost(ac.getProxyHost());
		setProxyPort(ac.getProxyPort());
		setProxyUsername(ac.getProxyUsername());
		setProxyPassword(ac.getProxyPassword());
		setProxyUseAuth(ac.isProxyAuth());

		String typeString = ac.getProxyType();
		String authTypeString = ac.getProxyAuthType();

		if (typeString == null) {
			setType(ProxyType.NONE);
		}
		else {
			setType(ProxyType.valueOf(typeString.toUpperCase()));
		}

		if (authTypeString == null) {
			setAuthType(ProxyAuthType.HTTP);
		}
		else {
			setAuthType(ProxyAuthType.valueOf(authTypeString.toUpperCase()));
		}

		if (getProxyHost() != null) {
			System.setProperty("http.proxyHost", getProxyHost());
			System.setProperty("http.proxyPort", getProxyPort());
			System.setProperty("http.proxyUsername", getProxyUsername());
			System.setProperty("http.proxyPassword", getProxyPassword());
		}

		log.info("proxy working : " + testProxy());
		return this;
	}

	public boolean testProxy() {
		try {
			URL url = new URL("http://www.google.com");

			URLConnection uc = url.openConnection();
			uc.connect();

			BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));

			while (in.readLine() != null) {
			}

			log.info("proxy OK");
			return true;
		}
		catch (Exception e) {
			log.info("proxy KO : " + e.toString());
			return false;
		}
	}

	public ProxyType getType() {
		return type;
	}

	public void setType(ProxyType type) {
		this.type = type;
	}

	public Boolean getProxyUseAuth() {
		return proxyUseAuth;
	}

	public void setProxyUseAuth(Boolean proxyUseAuth) {
		this.proxyUseAuth = proxyUseAuth;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public String getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(String proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getProxyUsername() {
		return proxyUsername;
	}

	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}
	
	public ProxyAuthType getAuthType() {
		return authType;
	}

	public void setAuthType(ProxyAuthType authType) {
		this.authType = authType;
	}

	public static ProxyController instance() {
		if (instance == null)
			instance = new ProxyController();
		return instance;
	}
}