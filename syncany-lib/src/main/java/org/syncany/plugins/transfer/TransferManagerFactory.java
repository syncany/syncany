/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.plugins.transfer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.plugins.transfer.features.Feature;
import org.syncany.plugins.transfer.features.PathAware;
import org.syncany.plugins.transfer.features.Retriable;
import org.syncany.plugins.transfer.features.TransactionAware;
import org.syncany.util.ReflectionUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/**
 * @author Christian Roth <christian.roth@port17.de>
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class TransferManagerFactory {
	private static final Logger logger = Logger.getLogger(TransferManagerFactory.class.getSimpleName());

	private static final String FEATURE_TRANSFER_MANAGER_FORMAT = Feature.class.getPackage().getName() + ".%s" + TransferManager.class.getSimpleName();
	private static final List<Class<? extends Annotation>> FEATURE_LIST = ImmutableList.<Class<? extends Annotation>> builder()
			.add(TransactionAware.class)
			.add(Retriable.class)
			.add(PathAware.class)
			.build();

	public static Builder build(Config config) throws StorageException {		
		TransferManager transferManager = config.getTransferPlugin().createTransferManager(config.getConnection(), config);		
		logger.log(Level.INFO, "Building " + transferManager.getClass().getSimpleName() + " from config '" + config.getLocalDir().getName() + "' ...");

		return new Builder(transferManager, config);
	}

	public static class Builder {
		private List<Class<? extends Annotation>> features;
		private Config config;
		private TransferManager originalTransferManager;
		private TransferManager wrappedTransferManager;

		private Builder(TransferManager transferManager, Config config) {
			this.features = new ArrayList<>();
			this.wrappedTransferManager = transferManager;
			this.originalTransferManager = transferManager;
			this.config = config;
		}

		public Builder withFeature(Class<? extends Annotation> featureAnnotation) {
			logger.log(Level.INFO, "- With feature " + featureAnnotation.getSimpleName());

			features.add(featureAnnotation);
			return this;
		}

		public TransferManager asDefault() {
			return wrap(TransferManager.class);
		}

		@SuppressWarnings("unchecked")
		public <T extends TransferManager> T as(Class<? extends Annotation> featureAnnotation) {
			Class<T> implementingTransferManager = (Class<T>) getFeatureTransferManagerClass(featureAnnotation);
			return wrap(implementingTransferManager);
		}

		private <T extends TransferManager> T wrap(Class<T> desiredTransferManagerClass) {
			checkIfAllFeaturesSupported();
			checkDuplicateFeatures();
			checkRequiredFeatures();
			
			applyFeatures();
			
			return castToDesiredTransferManager(desiredTransferManagerClass);			
		}
		
		private void applyFeatures() {
			try {
				for (Class<? extends Annotation> featureAnnotation : features) {
					boolean isFeatureSupported = ReflectionUtil.isAnnotationPresentInHierarchy(originalTransferManager.getClass(), featureAnnotation);
					
					if (isFeatureSupported) {
						Class<? extends TransferManager> featureTransferManagerClass = getFeatureTransferManagerClass(featureAnnotation);
						wrappedTransferManager = apply(wrappedTransferManager, featureTransferManagerClass, featureAnnotation);
					}
					else {
						logger.log(Level.INFO, "- SKIPPING unsupported optional feature " + featureAnnotation.getSimpleName());
					}
				}
			}
			catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
				throw new RuntimeException("Unable to annotate TransferManager with feature.", e);
			}
		}
		
		private <T extends TransferManager> T castToDesiredTransferManager(Class<T> desiredTransferManagerClass) {			
			try {
				return desiredTransferManagerClass.cast(wrappedTransferManager);
			}
			catch (ClassCastException e) {
				throw new RuntimeException("Unable to wrap TransferManager in " + desiredTransferManagerClass.getSimpleName()
						+ " because feature does not seem to be supported", e);
			}
		}

		private TransferManager apply(TransferManager transferManager, Class<? extends TransferManager> featureTransferManagerClass,
				Class<? extends Annotation> featureAnnotationClass) throws IllegalAccessException, InvocationTargetException, InstantiationException {
			
			Annotation concreteFeatureAnnotation = ReflectionUtil.getAnnotationInHierarchy(originalTransferManager.getClass(), featureAnnotationClass);
			TransferManager wrappedTransferManager;
			
			// Try the most common constructor
			wrappedTransferManager = createDefaultTransferManager(transferManager, featureTransferManagerClass, featureAnnotationClass, concreteFeatureAnnotation);
			
			if (wrappedTransferManager != null) {
				return wrappedTransferManager;
			}
			
			// Try feature constructor
			wrappedTransferManager = createFeatureTransferManager(transferManager, featureTransferManagerClass, featureAnnotationClass, concreteFeatureAnnotation);
			
			if (wrappedTransferManager != null) {
				return wrappedTransferManager;
			}
			
			// Try legacy/classic constructor
			wrappedTransferManager = createClassicTransferManager(transferManager, featureTransferManagerClass);
			
			if (wrappedTransferManager != null) {
				return wrappedTransferManager;
			}						

			throw new RuntimeException("Invalid TransferManager class detected: Unable to find constructor.");
		}

		// Try the most default constructor		
		private TransferManager createDefaultTransferManager(TransferManager transferManager,
				Class<? extends TransferManager> featureTransferManagerClass, Class<? extends Annotation> featureAnnotationClass,
				Annotation concreteFeatureAnnotation) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			
			Constructor<?> transferManagerConstructor = ReflectionUtil.getMatchingConstructorForClass(featureTransferManagerClass, TransferManager.class, Config.class, featureAnnotationClass);

			if (transferManagerConstructor != null) {
				logger.log(Level.FINE,
						"- Wrapping TransferManager " + transferManager.getClass().getSimpleName() + " in " + featureTransferManagerClass.getSimpleName() + ", using DEFAULT contructor");

				return (TransferManager) transferManagerConstructor.newInstance(transferManager, config, featureAnnotationClass.cast(concreteFeatureAnnotation));
			}
			else {
				return null;
			}
		}

		private TransferManager createFeatureTransferManager(TransferManager transferManager,
				Class<? extends TransferManager> featureTransferManagerClass, Class<? extends Annotation> featureAnnotationClass,
				Annotation concreteFeatureAnnotation) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
			// Some features require the original TM instance because they use a feature extension
			Constructor<?> transferManagerConstructor = ReflectionUtil.getMatchingConstructorForClass(featureTransferManagerClass, TransferManager.class, Config.class, featureAnnotationClass, TransferManager.class);

			if (transferManagerConstructor != null) {
				logger.log(Level.FINE,
						"- Wrapping TransferManager " + transferManager.getClass().getSimpleName() + " in " + featureTransferManagerClass.getSimpleName() + ", using FEATURE contructor");

				Annotation featureAnnotation = featureAnnotationClass.cast(concreteFeatureAnnotation);
				return (TransferManager) transferManagerConstructor.newInstance(transferManager, config, featureAnnotation, originalTransferManager);
			}
			else {
				return null;
			}
		}
		
		private TransferManager createClassicTransferManager(TransferManager transferManager,
				Class<? extends TransferManager> featureTransferManagerClass) throws InstantiationException, IllegalAccessException,
				IllegalArgumentException, InvocationTargetException {

			// legacy support for old TransferManagers
			Constructor<?> transferManagerConstructor = ReflectionUtil.getMatchingConstructorForClass(featureTransferManagerClass, TransferManager.class);

			if (transferManagerConstructor != null) {
				logger.log(Level.FINE,
						"- Wrapping TransferManager " + transferManager.getClass().getSimpleName() + " in " + featureTransferManagerClass.getSimpleName() + ", using CLASSIC contructor");

				return (TransferManager) transferManagerConstructor.newInstance(transferManager);
			}
			else {
				return null;
			}
		}

		private static Class<? extends TransferManager> getFeatureTransferManagerClass(Class<? extends Annotation> featureAnnotation) {
			String featureTransferManagerClassName = String.format(FEATURE_TRANSFER_MANAGER_FORMAT, featureAnnotation.getSimpleName());

			try {
				return (Class<? extends TransferManager>) Class.forName(featureTransferManagerClassName).asSubclass(TransferManager.class);
			}
			catch (Exception e) {
				throw new RuntimeException("Unable to find class with feature " + featureAnnotation.getSimpleName() + ". Tried " + featureTransferManagerClassName, e);
			}
		}		

		private void checkRequiredFeatures() {
			for (Class<? extends Annotation> concreteFeatureAnnotation : FEATURE_LIST) {
				Feature featureAnnotation = ReflectionUtil.getAnnotationInHierarchy(concreteFeatureAnnotation, Feature.class);
				
				if (featureAnnotation.required()) {
					logger.log(Level.FINE, "- Checking required feature " + concreteFeatureAnnotation.getSimpleName() + " in " + originalTransferManager.getClass().getSimpleName() + " ...");					
					boolean requiredFeaturePresent = ReflectionUtil.isAnnotationPresentInHierarchy(originalTransferManager.getClass(), concreteFeatureAnnotation);
					
					if (!requiredFeaturePresent) {
						throw new RuntimeException("Required feature " + concreteFeatureAnnotation.getSimpleName() + " is not present in " + originalTransferManager.getClass().getSimpleName());
					}
				}
			}
		}
		
		private void checkDuplicateFeatures() {
			logger.log(Level.FINE, "- Checking for duplicate features ...");					

			int listSize = features.size();
			int setSize = Sets.newHashSet(features).size();

			if (listSize != setSize) {
				throw new IllegalArgumentException("There are duplicates in feature set: " + features);
			}
		}

		private void checkIfAllFeaturesSupported() {
			logger.log(Level.FINE, "- Checking if selected features supported ...");					

			for (Class<? extends Annotation> featureAnnotation : features) {
				if (!featureAnnotation.isAnnotationPresent(Feature.class)) {
					throw new IllegalArgumentException("Feature " + featureAnnotation + " is unknown");
				}
			}
		}
	}
}
