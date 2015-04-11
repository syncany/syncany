/**
 * Provides classes used by multiple/all core operations, and the abstract classes
 * they inherit from. 
 * 
 * <p>Operations implement the majority of the application's bahavioral logic. All operations
 * follow the same pattern. An {@link org.syncany.operations.Operation Operation} is configured
 * via a corresponding {@link org.syncany.operations.OperationOptions OperationOptions} object
 * and, when executed, returns a {@link org.syncany.operations.OperationResult OperationResult}
 * object. 
 */
package org.syncany.operations;