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
package org.syncany.plugins.transfer.feature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.util.ReflectionUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public class TransferManagerFeatureSupport {

	private static final Logger logger = Logger.getLogger(TransferManagerFeatureSupport.class.getName());

	private static final String FQCN_SKELETON = TransferManagerFeatureSupport.class.getPackage().getName() + ".%sTransferManager";
	private static final int DEFAULT_PRIORITY = 0;

	public static final List<Class<? extends Annotation>> FEATURES = ImmutableList.<Class<? extends Annotation>>builder()
					.add(TransactionAware.class)
					.add(Retriable.class)
					.add(PathAware.class)
					.build();

	public static Builder build(TransferManager transferManager, Config config) {
		return new Builder(transferManager, config);
	}

	public static class Builder {

		private final TreeMultimap<Integer, Class<? extends Annotation>> features = TreeMultimap.create(Ordering.natural(), Ordering.explicit(FEATURES));
		private final Config config;
		private final Class<? extends TransferManager> transferManagerClass;
		private TransferManager transferManager;

		private Builder(TransferManager transferManager, Config config) {
			this.transferManager = transferManager;
			transferManagerClass = transferManager.getClass();
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

		public Builder withAllFeatures() {
			for (Class<? extends Annotation> featureAnnotation : FEATURES) {
				withFeature(featureAnnotation);
			}
			return this;
		}

		public TransferManager asDefault() {
			return wrap(TransferManager.class);
		}

		@SuppressWarnings("unchecked")
		public <T extends TransferManager> T as(Class<? extends Annotation> featureAnnotation) {
			return wrap((Class<T>) getImplementingTransferManager(featureAnnotation));
		}

		private <T extends TransferManager> T wrap(Class<T> desiredTransferManagerClass) {
			checkFeatureSetForDuplicates();

			logger.log(Level.INFO, "Annotating TransferManager: " + features.size() + "/" + FEATURES.size() + " features selected");

			Class<? extends Annotation> applyLast = null;

			try {
				for (Class<? extends Annotation> featureAnnotation : features.values()) {
					if (ReflectionUtil.isAnnotationPresentSomewhere(transferManagerClass, featureAnnotation)) {
						Class<? extends TransferManager> featureTransferManagerClass = getImplementingTransferManager(featureAnnotation);

						if (featureTransferManagerClass.equals(desiredTransferManagerClass)) {
							logger.log(Level.INFO, "Holding back current feature, request to wrap last");
							applyLast = featureAnnotation;
							continue;
						}

						transferManager = apply(transferManager, featureTransferManagerClass, featureAnnotation);
					}
				}

				if (applyLast != null) {
					transferManager = apply(transferManager, getImplementingTransferManager(applyLast), applyLast);
				}
			}
			catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
				logger.log(Level.SEVERE, "Unable to annotate TransferManager with feature", e);
				throw new RuntimeException("Unable to annotate TransferManager with feature", e);
			}

			try {
				return desiredTransferManagerClass.cast(transferManager);
			}
			catch(ClassCastException e) {
				logger.log(Level.SEVERE, "Unable to wrap TransferManager in " + desiredTransferManagerClass.getSimpleName() + " because the feature does not seem to be supported", e);
				throw new RuntimeException("Unable to wrap TransferManager in " + desiredTransferManagerClass.getSimpleName() + " because the feature does not seem to be supported", e);
			}
		}
		
		private TransferManager apply(TransferManager transferManager, Class<? extends TransferManager> featureTransferManagerClass, Class<? extends Annotation> featureAnnotationClass) throws IllegalAccessException, InvocationTargetException, InstantiationException {
			logger.log(Level.FINE, "Wrapping TransferManager " + transferManager + " in " + featureTransferManagerClass);

			Annotation concreteFeatureAnnotation = ReflectionUtil.getAnnotationSomewhere(transferManagerClass, featureAnnotationClass);
			Constructor<?> constructor;

			constructor = ReflectionUtil.getMatchingConstructorForClass(featureTransferManagerClass, TransferManager.class, Config.class, featureAnnotationClass);

			if (constructor != null) {
				return (TransferManager) constructor.newInstance(transferManager, config, featureAnnotationClass.cast(concreteFeatureAnnotation));
			}

			// legacy support for old TransferManagers
			logger.log(Level.WARNING, "DEPRECATED: TransferManager seems to be config independent and does not use annotation settings, searching simple constructor...");
			constructor = ReflectionUtil.getMatchingConstructorForClass(featureTransferManagerClass, TransferManager.class);

			if (constructor != null) {
				return (TransferManager) constructor.newInstance(transferManager);
			}

			throw new RuntimeException("Invalid TransferManager feature class detected: Unable to find constructor");
		}

		@SuppressWarnings("unchecked")
		private static Class<? extends TransferManager> getImplementingTransferManager(Class<? extends Annotation> featureAnnotation) {
			String FQCN = String.format(FQCN_SKELETON, featureAnnotation.getSimpleName());

			try {
				return (Class<? extends TransferManager>) Class.forName(FQCN);
			}
			catch (Exception e) {
				throw new RuntimeException("Unable to find class for feature " + featureAnnotation.getSimpleName() + ". Tried " + FQCN, e);
			}
		}

		private void checkFeatureSetForDuplicates() {
			int listSize = features.values().size();
			int setSize = Sets.newHashSet(features.values()).size();

			if (listSize != setSize) {
				throw new IllegalArgumentException("There duplicates in the feature set: " + features);
			}
		}

	}

}
