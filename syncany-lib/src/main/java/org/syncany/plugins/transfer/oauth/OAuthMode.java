package org.syncany.plugins.transfer.oauth;

/**
 * OAuth basically provides two different types of implicit grant. One is used by clients to directly acquire an Access token (often with
 * a very short lifetime) to perform different API actions. The second mode is used by server and requires a separate step to transform an
 * Authorization token into an actual Access token. Syncany supports both modes out of the box.
 *
 * @author Christian Roth <christian.roth@port17.de>
 */

public enum OAuthMode {
	BROWSER,
	SERVER
}
