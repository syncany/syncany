/**
 * Provides interfaces and abstract classes to describe the daemon message API.
 * 
 * <p>Some of the application's functionality is exposed via the HTTP/REST API. All communication
 * to and from the API is done via {@link org.syncany.operations.daemon.messages.api.Message Messages}.
 * 
 * <p>Incoming messages can be either {@link org.syncany.operations.daemon.messages.api.Request Requests} or
 * {@link org.syncany.operations.daemon.messages.api.EventResponse EventResponses}. Outgoing messages can be
 * {@link org.syncany.operations.daemon.messages.api.Response Responses} or 
 * {@link org.syncany.operations.daemon.messages.api.Event Events}.
 */
package org.syncany.operations.daemon.messages.api;