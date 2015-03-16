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
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */
public class TransferManagerFactory {
	private static final Logger logger = Logger.getLogger(TransferManagerFactory.class.getSimpleName());

	private static final String FEATURE_TRANSFER_MANAGER_FORMAT = TransferManagerFactory.class.getPackage().getName() + ".features.%sTransferManager";
	private static final int DEFAULT_PRIORITY = 0;

	private static final List<Class<? extends Annotation>> ALL_FEATURES = ImmutableList.<Class<? extends Annotation>> builder()
			.add(TransactionAware.class)
			.add(Retriable.class)
			.add(PathAware.class)
			.build();

	public static Builder buildFromConfig(Config config) throws StorageException {
		TransferManager transferManager = config.getTransferPlugin().createTransferManager(config.getConnection(), config);
		return new Builder(transferManager, config);
	}

	public static class Builder {
		private TreeMultimap<Integer, Class<? extends Annotation>> features = TreeMultimap.create(Ordering.natural(), Ordering.explicit(ALL_FEATURES));
		private Config config;
		private TransferManager originalTransferManager;
		private TransferManager wrappedTransferManager;

		private Builder(TransferManager transferManager, Config config) {
			this.wrappedTransferManager = transferManager;
			this.originalTransferManager = transferManager;
			this.config = config;
		}

		public Builder withFeature(Class<? extends Annotation> featureAnnotation) {
			features.put(DEFAULT_PRIORITY, featureAnnotation);
			return this;
		}

		public Builder withFeature(Class<? extends Annotation> featureAnnotation, int priority) {
			features.put(priority, featureAnnotation);
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
			checkFeatureSetForDuplicates();
			checkIfAllFeaturesSupported();

			logger.log(Level.INFO, "Annotating TransferManager: " + features.size() + "/" + ALL_FEATURES.size() + " features selected");

			Class<? extends Annotation> applyLastFeatureAnnotation = null;

			try {
				for (Class<? extends Annotation> featureAnnotation : features.values()) {
					if (ReflectionUtil.isAnnotationPresentInHierarchy(originalTransferManager.getClass(), featureAnnotation)) {
						Class<? extends TransferManager> featureTransferManagerClass = getFeatureTransferManagerClass(featureAnnotation);

						if (featureTransferManagerClass.equals(desiredTransferManagerClass)) {
							logger.log(Level.INFO, "Holding back feature " + featureTransferManagerClass.getSimpleName() + ", request to wrap last.");
							applyLastFeatureAnnotation = featureAnnotation;
							continue;
						}

						wrappedTransferManager = apply(wrappedTransferManager, featureTransferManagerClass, featureAnnotation);
					}
				}

				if (applyLastFeatureAnnotation != null) {
					Class<? extends TransferManager> featureTransferManagerClass = getFeatureTransferManagerClass(applyLastFeatureAnnotation);
					wrappedTransferManager = apply(wrappedTransferManager, featureTransferManagerClass, applyLastFeatureAnnotation);
				}
			}
			catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
				throw new RuntimeException("Unable to annotate TransferManager with feature.", e);
			}

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
			
			logger.log(Level.FINE,
					"Wrapping TransferManager " + transferManager.getClass().getSimpleName() + " in " + featureTransferManagerClass.getSimpleName());

			Annotation concreteFeatureAnnotation = ReflectionUtil.getAnnotationInHierarchy(originalTransferManager.getClass(), featureAnnotationClass);
			Constructor<?> transferManagerConstructor;

			// Try the most common constructor
			transferManagerConstructor = ReflectionUtil.getMatchingConstructorForClass(featureTransferManagerClass, TransferManager.class, Config.class, featureAnnotationClass);

			if (transferManagerConstructor != null) {
				return (TransferManager) transferManagerConstructor.newInstance(transferManager, config, featureAnnotationClass.cast(concreteFeatureAnnotation));
			}

			// Some features require the original TM instance because they use a feature extension
			transferManagerConstructor = ReflectionUtil.getMatchingConstructorForClass(featureTransferManagerClass, TransferManager.class, Config.class, featureAnnotationClass, TransferManager.class);

			if (transferManagerConstructor != null) {
				logger.log(Level.INFO, "Feature uses an extension");
				Annotation featureAnnotation = featureAnnotationClass.cast(concreteFeatureAnnotation);
				return (TransferManager) transferManagerConstructor.newInstance(transferManager, config, featureAnnotation, originalTransferManager);
			}

			// legacy support for old TransferManagers
			logger.log(Level.WARNING, "DEPRECATED: TransferManager seems to be config independent and does not use annotation settings, searching simple constructor...");
			transferManagerConstructor = ReflectionUtil.getMatchingConstructorForClass(featureTransferManagerClass, TransferManager.class);

			if (transferManagerConstructor != null) {
				return (TransferManager) transferManagerConstructor.newInstance(transferManager);
			}

			throw new RuntimeException("Invalid TransferManager class detected: Unable to find constructor");
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

		private void checkFeatureSetForDuplicates() {
			int listSize = features.values().size();
			int setSize = Sets.newHashSet(features.values()).size();

			if (listSize != setSize) {
				throw new IllegalArgumentException("There are duplicates in tfeaturesure set: " + features);
			}
		}

		private void checkIfAllFeaturesSupported() {
			for (Class<? extends Annotation> featureAnnotation : features.values()) {
				if (!featureAnnotation.isAnnotationPresent(Feature.class)) {
					throw new IllegalArgumentException("Feature " + featureAnnotation + " is unknown");
				}
			}
		}
	}
}
