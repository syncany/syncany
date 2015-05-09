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
import org.syncany.plugins.transfer.features.ReadAfterWriteConsistent;
import org.syncany.plugins.transfer.features.Feature;
import org.syncany.plugins.transfer.features.FeatureTransferManager;
import org.syncany.plugins.transfer.features.PathAware;
import org.syncany.plugins.transfer.features.Retriable;
import org.syncany.plugins.transfer.features.TransactionAware;
import org.syncany.util.ReflectionUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/**
 * This factory class creates a {@link TransferManager} from a
 * {@link Config} object, and wraps it into the requested {@link Feature}(s).
 *
 * <p>Depending on the {@link Feature}s that the original transfer manager is
 * annotated with, the factory will wrap it into the corresponding feature
 * specific transfer managers.
 *
 * <p>The class uses the builder pattern. It can be used like this:
 *
 * <pre>
 *   TransactionAwareFeatureTransferManager txAwareTM = TransferManagerFactory
 *     .build(config)
 *     .withFeature(Retriable.class)
 *     .withFeature(PathAware.class)
 *     .withFeature(TransactionAware.class)
 *     .as(TransactionAware.class);
 * </pre>
 *
 * @see Feature
 * @see FeatureTransferManager
 * @see TransferManager
 * @author Christian Roth <christian.roth@port17.de>
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class TransferManagerFactory {
	private static final Logger logger = Logger.getLogger(TransferManagerFactory.class.getSimpleName());

	private static final String FEATURE_TRANSFER_MANAGER_FORMAT = Feature.class.getPackage().getName() + ".%s" + FeatureTransferManager.class.getSimpleName();
	private static final List<Class<? extends Annotation>> FEATURE_LIST = ImmutableList.<Class<? extends Annotation>> builder()
			.add(TransactionAware.class)
			.add(Retriable.class)
			.add(PathAware.class)
			.add(ReadAfterWriteConsistent.class)
			.build();

	/**
	 * Creates the transfer manager factory builder from the {@link Config}
	 * using the configured {@link TransferPlugin}. Using this builder, the
	 * feature-wrapped transfer manager can be built.
	 *
	 * @see TransferManagerBuilder
	 * @param config Local folder configuration with transfer plugin settings
	 * @return Transfer manager builder
	 */
	public static TransferManagerBuilder build(Config config) throws StorageException {
		TransferManager transferManager = config.getTransferPlugin().createTransferManager(config.getConnection(), config);
		logger.log(Level.INFO, "Building " + transferManager.getClass().getSimpleName() + " from config '" + config.getLocalDir().getName() + "' ...");

		return new TransferManagerBuilder(config, transferManager);
	}

	/**
	 * The transfer manager builder takes an original {@link TransferManager}, and
	 * wraps it with feature-specific transfer managers, if the original transfer
	 * manager is annotated with a {@link Feature} annotation.
	 *
	 * <p>The class uses the builder pattern. Its usage is described in the
	 * {@link TransferManagerFactory}. The two main methods of this class are
	 * {@link #withFeature(Class)} and {@link #as(Class)}.
	 *
	 * @see Feature
	 * @see TransferManagerFactory
	 */
	public static class TransferManagerBuilder {
		private List<Class<? extends Annotation>> features;
		private Config config;
		private TransferManager originalTransferManager;
		private TransferManager wrappedTransferManager;

		private TransferManagerBuilder(Config config, TransferManager transferManager) {
			this.config = config;
			this.originalTransferManager = transferManager;
			this.wrappedTransferManager = transferManager;
			this.features = new ArrayList<>();
		}

		/**
		 * This method requests the original transfer manager to be wrapped in the corresponding
		 * feature transfer manager.
		 *
		 * <p><b>Note:</b> Calling this method does not automatically wrap the transfer manager.
		 * It will only be wrapped if the original transfer manager is annotated with the feature
		 * annotation.
		 *
		 * <p>If the requested {@link Feature} is required (as per its definition), but the original
		 * transfer manager is not annotated with this feature, the creation of the transfer manager
		 * will fail.
		 *
		 * @param featureAnnotation Annotation representing the feature (see features.* package)
		 * @return Returns this builder class (for more features to be requested)
		 */
		public TransferManagerBuilder withFeature(Class<? extends Annotation> featureAnnotation) {
			logger.log(Level.INFO, "- With feature " + featureAnnotation.getSimpleName());

			features.add(featureAnnotation);
			return this;
		}

		/**
		 * Wraps of the previously requested feature transfer managers and casts the result to the requested class.
		 * If no specific class is requested, {@link #asDefault()} can be used instead.
		 *
		 * @param featureAnnotation Feature annotation corresponding to the requested transfer manager
		 * @return {@link TransferManager} casted to the feature lasted wrapped (and requested by this method)
		 */
		@SuppressWarnings("unchecked")
		public <T extends TransferManager> T as(Class<? extends Annotation> featureAnnotation) {
			Class<T> implementingTransferManagerClass = (Class<T>) getFeatureTransferManagerClass(featureAnnotation);
			return wrap(implementingTransferManagerClass);
		}

		/**
		 * Wraps of the previously requested feature transfer managers and returns a standard transfer manager.
		 * @return {@link TransferManager} wrapped with the requested features
		 */
		public TransferManager asDefault() {
			return wrap(TransferManager.class);
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

		private TransferManager apply(TransferManager underlyingTransferManager, Class<? extends TransferManager> featureTransferManagerClass,
				Class<? extends Annotation> featureAnnotationClass) throws IllegalAccessException, InvocationTargetException, InstantiationException {

			logger.log(Level.FINE,
					"- Wrapping TransferManager " + underlyingTransferManager.getClass().getSimpleName() + " in " + featureTransferManagerClass.getSimpleName());

			Annotation concreteFeatureAnnotation = ReflectionUtil.getAnnotationInHierarchy(originalTransferManager.getClass(), featureAnnotationClass);
			Constructor<?> transferManagerConstructor = ReflectionUtil.getMatchingConstructorForClass(featureTransferManagerClass, TransferManager.class, TransferManager.class, Config.class, featureAnnotationClass);

			if (transferManagerConstructor == null) {
				throw new RuntimeException("Invalid TransferManager class detected: Unable to find constructor.");
			}

			Annotation featureAnnotation = featureAnnotationClass.cast(concreteFeatureAnnotation);
			return (TransferManager) transferManagerConstructor.newInstance(originalTransferManager, underlyingTransferManager, config, featureAnnotation);
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
			// TODO [low] Instead of a feature list, all available @Feature annotations should be listed with reflection

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
