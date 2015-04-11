/**
 * Provides classes to configure the application, and to read/write the
 * configuration.
 * 
 * <p>The central classes in this package are the repository-specific 
 * configuration in {@link org.syncany.config.Config Config}, the
 * user-specific config in {@link org.syncany.config.UserConfig UserConfig},
 * as well as the daemon configuration.
 * 
 * <p>The configuration is usally stored in XML files. All configuration is 
 * read/written via SimpleXML and its corresponding transfer objects. 
 */
package org.syncany.config;