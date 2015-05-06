/**
 * Provides classes to extend the default {@link org.syncany.plugins.transfer.TransferManager TransferManager}
 * functionality with {@link org.syncany.plugins.transfer.features.Feature Features}.
 * 
 * <p>Features are extensions for transfer managers, such as path awareness,
 * transaction awareness or retriability. 
 * 
 * <p>The central classes in this package are the {@link org.syncany.plugins.transfer.features.Feature Feature}
 * to mark feature annotations, {@link org.syncany.plugins.transfer.features.FeatureExtension FeatureExtension}
 * to provide additional methods to a feature, and {@link org.syncany.plugins.transfer.features.FeatureTransferManager FeatureTransferManager}
 * to wrap the normal transfer manager with the feature's functionality. 
 */
package org.syncany.plugins.transfer.features;