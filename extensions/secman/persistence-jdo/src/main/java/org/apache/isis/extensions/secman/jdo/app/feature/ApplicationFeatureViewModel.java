/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.extensions.secman.jdo.app.feature;

import java.util.List;
import java.util.SortedSet;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.isis.applib.ViewModel;
import org.apache.isis.applib.annotation.Collection;
import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.annotation.Where;
import org.apache.isis.applib.services.factory.FactoryService;
import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.applib.util.ObjectContracts;
import org.apache.isis.commons.internal.base._Casts;
import org.apache.isis.commons.internal.collections._Lists;
import org.apache.isis.extensions.secman.api.SecurityModule;
import org.apache.isis.extensions.secman.jdo.dom.permission.ApplicationPermission;
import org.apache.isis.extensions.secman.jdo.dom.permission.ApplicationPermissionRepository;
import org.apache.isis.metamodel.services.appfeat.ApplicationFeature;
import org.apache.isis.metamodel.services.appfeat.ApplicationFeatureId;
import org.apache.isis.metamodel.services.appfeat.ApplicationFeatureRepositoryDefault;
import org.apache.isis.metamodel.services.appfeat.ApplicationFeatureType;

import lombok.val;

/**
 * View model identified by {@link ApplicationFeatureId} and backed by an {@link ApplicationFeature}.
 */
//@MemberGroupLayout(
//        columnSpans = {6,0,6,12},
//        left = {"Id", "Data Type", "Metadata"},
//        right= {"Parent", "Contributed", "Detail"}
//)
public abstract class ApplicationFeatureViewModel implements ViewModel {

	public static abstract class PropertyDomainEvent<S extends ApplicationFeatureViewModel,T> extends SecurityModule.PropertyDomainEvent<S, T> {
		private static final long serialVersionUID = 1L;}

	public static abstract class CollectionDomainEvent<S extends ApplicationFeatureViewModel,T> extends SecurityModule.CollectionDomainEvent<S, T> {
		private static final long serialVersionUID = 1L;}

	public static abstract class ActionDomainEvent<S extends ApplicationFeatureViewModel> extends SecurityModule.ActionDomainEvent<S> {
		private static final long serialVersionUID = 1L;}



	// -- constructors
	public static ApplicationFeatureViewModel newViewModel(
			final ApplicationFeatureId featureId,
			final ApplicationFeatureRepositoryDefault applicationFeatureRepository,
			final FactoryService factoryService) {
		final Class<? extends ApplicationFeatureViewModel> cls = viewModelClassFor(featureId, applicationFeatureRepository);
		return factoryService.viewModel(cls, featureId.asEncodedString());
	}

	private static Class<? extends ApplicationFeatureViewModel> viewModelClassFor(
			final ApplicationFeatureId featureId,
			final ApplicationFeatureRepositoryDefault applicationFeatureRepository) {
		switch (featureId.getType()) {
		case PACKAGE:
			return ApplicationPackage.class;
		case CLASS:
			return ApplicationClass.class;
		case MEMBER:
			final ApplicationFeature feature = applicationFeatureRepository.findFeature(featureId);

			if(feature == null) {
				// TODO: not sure why, yet...
				return null;
			}
			switch(feature.getMemberType()) {
			case PROPERTY:
				return ApplicationClassProperty.class;
			case COLLECTION:
				return ApplicationClassCollection.class;
			case ACTION:
				return ApplicationClassAction.class;
			}
		}
		throw new IllegalArgumentException("could not determine feature type; featureId = " + featureId);
	}

	public ApplicationFeatureViewModel() {
		this(ApplicationFeatureId.PACKAGE_DEFAULT);
	}

	ApplicationFeatureViewModel(final ApplicationFeatureId featureId) {
		setFeatureId(featureId);
	}


	// -- identification
	/**
	 * having a title() method (rather than using @Title annotation) is necessary as a workaround to be able to use
	 * wrapperFactory#unwrap(...) method, which is otherwise broken in Isis 1.6.0
	 */
	public String title() {
		return getFullyQualifiedName();
	}
	public String iconName() {
		return "applicationFeature";
	}

	// -- ViewModel impl
	@Override
	public String viewModelMemento() {
		return getFeatureId().asEncodedString();
	}

	@Override
	public void viewModelInit(final String encodedMemento) {
		final ApplicationFeatureId applicationFeatureId = ApplicationFeatureId.parseEncoded(encodedMemento);
		setFeatureId(applicationFeatureId);
	}


	// -- featureId (property, programmatic)
	private ApplicationFeatureId featureId;

	@Programmatic
	public ApplicationFeatureId getFeatureId() {
		return featureId;
	}

	public void setFeatureId(final ApplicationFeatureId applicationFeatureId) {
		this.featureId = applicationFeatureId;
	}


	// -- feature (property, programmatic)
	@Programmatic
	ApplicationFeature getFeature() {
		return applicationFeatureRepository.findFeature(getFeatureId());
	}


	// -- fullyQualifiedName (property, programmatic)
	@Programmatic // in the title
	public String getFullyQualifiedName() {
		return getFeatureId().getFullyQualifiedName();
	}


	// -- type (programmatic)
	@Programmatic
	public ApplicationFeatureType getType() {
		return getFeatureId().getType();
	}

	// -- packageName
	public static class PackageNameDomainEvent extends PropertyDomainEvent<ApplicationFeatureViewModel, String> {
		private static final long serialVersionUID = 1L;}

	@Property(
			domainEvent = PackageNameDomainEvent.class
			)
	@PropertyLayout(typicalLength=ApplicationFeature.TYPICAL_LENGTH_PKG_FQN)
	@MemberOrder(name="Id", sequence = "2.2")
	public String getPackageName() {
		return getFeatureId().getPackageName();
	}

	// -- className

	public static class ClassNameDomainEvent extends PropertyDomainEvent<ApplicationFeatureViewModel, String> {
		private static final long serialVersionUID = 1L;}

	/**
	 * For packages, will be null. Is in this class (rather than subclasses) so is shown in
	 * {@link ApplicationPackage#getContents() package contents}.
	 */
	@Property(
			domainEvent = ClassNameDomainEvent.class
			)
	@PropertyLayout(typicalLength=ApplicationFeature.TYPICAL_LENGTH_CLS_NAME)
	@MemberOrder(name="Id", sequence = "2.3")
	public String getClassName() {
		return getFeatureId().getClassName();
	}
	public boolean hideClassName() {
		return getType().hideClassName();
	}

	// -- memberName

	public static class MemberNameDomainEvent extends PropertyDomainEvent<ApplicationFeatureViewModel, String> {
		private static final long serialVersionUID = 1L;}

	/**
	 * For packages and class names, will be null.
	 */
	@Property(
			domainEvent = MemberNameDomainEvent.class
			)
	@PropertyLayout(typicalLength=ApplicationFeature.TYPICAL_LENGTH_MEMBER_NAME)
	@MemberOrder(name="Id", sequence = "2.4")
	public String getMemberName() {
		return getFeatureId().getMemberName();
	}

	public boolean hideMemberName() {
		return getType().hideMember();
	}


	// -- parent (property)

	public static class ParentDomainEvent extends PropertyDomainEvent<ApplicationFeatureViewModel, ApplicationFeatureViewModel> {
		private static final long serialVersionUID = 1L;}

	@Property(
			domainEvent = ParentDomainEvent.class
			)
	@PropertyLayout(hidden=Where.ALL_TABLES)
	@MemberOrder(name = "Parent", sequence = "2.6")
	public ApplicationFeatureViewModel getParent() {
		final ApplicationFeatureId parentId;
		parentId = getType() == ApplicationFeatureType.MEMBER
				? getFeatureId().getParentClassId()
						: getFeatureId().getParentPackageId();
				if(parentId == null) {
					return null;
				}
				final ApplicationFeature feature = applicationFeatureRepository.findFeature(parentId);
				if (feature == null) {
					return null;
				}
				final Class<? extends ApplicationFeatureViewModel> cls = 
						viewModelClassFor(parentId, applicationFeatureRepository);
				return factory.viewModel(cls, parentId.asEncodedString());
	}




	// -- contributed (property)

	public static class ContributedDomainEvent extends PropertyDomainEvent<ApplicationFeatureViewModel, Boolean> {
		private static final long serialVersionUID = 1L;}

	/**
	 * For packages and class names, will be null.
	 */
	@Property(
			domainEvent = ContributedDomainEvent.class
			)
	@MemberOrder(name="Contributed", sequence = "2.5.5")
	public boolean isContributed() {
		return getFeature().isContributed();
	}

	public boolean hideContributed() {
		return getType().hideMember();
	}


	// -- permissions (collection)
	public static class PermissionsDomainEvent extends CollectionDomainEvent<ApplicationFeatureViewModel, ApplicationPermission> {
		private static final long serialVersionUID = 1L;}

	@Collection(
			domainEvent = PermissionsDomainEvent.class
			)
	@CollectionLayout(
			defaultView="table"
			)
	@MemberOrder(sequence = "10")
	public List<ApplicationPermission> getPermissions() {
		return applicationPermissionRepository.findByFeatureCached(getFeatureId());
	}


	// -- parentPackage (property, programmatic, for packages & classes only)

	/**
	 * The parent package feature of this class or package.
	 */
	@Programmatic
	public ApplicationFeatureViewModel getParentPackage() {
		return Functions.asViewModelForId(applicationFeatureRepository, factory)
				.apply(getFeatureId().getParentPackageId());
	}

	
	// -- equals, hashCode, toString

	private final static String propertyNames = "featureId";

	@Override
	public boolean equals(final Object obj) {
		return ObjectContracts.equals(this, obj, propertyNames);
	}

	@Override
	public int hashCode() {
		return ObjectContracts.hashCode(this, propertyNames);
	}

	@Override
	public String toString() {
		return ObjectContracts.toString(this, propertyNames);
	}


	// -- helpers
	<T extends ApplicationFeatureViewModel> List<T> asViewModels(final SortedSet<ApplicationFeatureId> members) {
		val viewModelForId = Functions.<T>asViewModelForId(applicationFeatureRepository, factory);
		return _Lists.map(members, viewModelForId);
	}


	// -- Functions

	public static final class Functions {
		private Functions(){}

		public static <T extends ApplicationFeatureViewModel> Function<ApplicationFeatureId, T> asViewModelForId(
				final ApplicationFeatureRepositoryDefault applicationFeatureRepository, 
				final FactoryService factoryService) {
			
			return (ApplicationFeatureId input) -> 
				_Casts.uncheckedCast(ApplicationFeatureViewModel
						.newViewModel(input, applicationFeatureRepository, factoryService));

		}
		public static <T extends ApplicationFeatureViewModel> Function<ApplicationFeature, T> asViewModel(
				final ApplicationFeatureRepositoryDefault applicationFeatureRepository, 
				final FactoryService factoryService) {

			return (ApplicationFeature input) ->
				_Casts.uncheckedCast(ApplicationFeatureViewModel
						.newViewModel(input.getFeatureId(), applicationFeatureRepository, factoryService));
		}
	}

	// -- DEPENDENCIES
	
	@Inject RepositoryService repository;
	@Inject FactoryService factory;
	@Inject ApplicationFeatureRepositoryDefault applicationFeatureRepository;
	@Inject ApplicationPermissionRepository applicationPermissionRepository;


}