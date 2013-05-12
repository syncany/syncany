package org.syncany.config;

public class ConfigException extends Exception {
	private static final long serialVersionUID = 4414807565457521855L;

	public ConfigException(Throwable cause) {
        super(cause);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException() {
    }    
}
