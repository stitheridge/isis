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
package org.apache.isis.persistence.jdo.datanucleus5.persistence;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;
import javax.enterprise.inject.Vetoed;
import javax.jdo.FetchGroup;
import javax.jdo.FetchPlan;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.identity.SingleFieldIdentity;
import javax.jdo.listener.InstanceLifecycleListener;
import javax.jdo.listener.StoreLifecycleListener;

import org.datanucleus.enhancement.Persistable;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.identity.DatastoreIdImpl;

import org.apache.isis.applib.query.Query;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.exceprecog.ExceptionRecognizer;
import org.apache.isis.applib.services.iactn.Interaction;
import org.apache.isis.applib.services.repository.EntityState;
import org.apache.isis.applib.services.xactn.TransactionService;
import org.apache.isis.core.commons.collections.Can;
import org.apache.isis.core.commons.exceptions.IsisException;
import org.apache.isis.core.commons.internal.collections._Maps;
import org.apache.isis.core.commons.internal.exceptions._Exceptions;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.oid.ObjectNotFoundException;
import org.apache.isis.core.metamodel.adapter.oid.Oid;
import org.apache.isis.core.metamodel.adapter.oid.PojoRefreshException;
import org.apache.isis.core.metamodel.adapter.oid.RootOid;
import org.apache.isis.core.metamodel.context.MetaModelContext;
import org.apache.isis.core.metamodel.facets.object.callbacks.CallbackFacet;
import org.apache.isis.core.metamodel.facets.object.callbacks.LoadedCallbackFacet;
import org.apache.isis.core.metamodel.facets.object.callbacks.LoadedLifecycleEventFacet;
import org.apache.isis.core.metamodel.facets.object.callbacks.PersistedCallbackFacet;
import org.apache.isis.core.metamodel.facets.object.callbacks.PersistedLifecycleEventFacet;
import org.apache.isis.core.metamodel.facets.object.callbacks.PersistingCallbackFacet;
import org.apache.isis.core.metamodel.facets.object.callbacks.PersistingLifecycleEventFacet;
import org.apache.isis.core.metamodel.facets.object.callbacks.RemovingCallbackFacet;
import org.apache.isis.core.metamodel.facets.object.callbacks.RemovingLifecycleEventFacet;
import org.apache.isis.core.metamodel.facets.object.callbacks.UpdatedCallbackFacet;
import org.apache.isis.core.metamodel.facets.object.callbacks.UpdatedLifecycleEventFacet;
import org.apache.isis.core.metamodel.facets.object.callbacks.UpdatingCallbackFacet;
import org.apache.isis.core.metamodel.facets.object.callbacks.UpdatingLifecycleEventFacet;
import org.apache.isis.core.metamodel.services.container.query.QueryCardinality;
import org.apache.isis.core.metamodel.spec.ManagedObject;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.runtime.persistence.transaction.IsisTransactionObject;
import org.apache.isis.persistence.jdo.applib.exceptions.NotPersistableException;
import org.apache.isis.persistence.jdo.applib.exceptions.UnsupportedFindException;
import org.apache.isis.persistence.jdo.applib.fixturestate.FixturesInstalledStateHolder;
import org.apache.isis.persistence.jdo.datanucleus5.datanucleus.persistence.commands.DataNucleusCreateObjectCommand;
import org.apache.isis.persistence.jdo.datanucleus5.datanucleus.persistence.commands.DataNucleusDeleteObjectCommand;
import org.apache.isis.persistence.jdo.datanucleus5.datanucleus.persistence.queries.PersistenceQueryFindAllInstancesProcessor;
import org.apache.isis.persistence.jdo.datanucleus5.datanucleus.persistence.queries.PersistenceQueryFindUsingApplibQueryProcessor;
import org.apache.isis.persistence.jdo.datanucleus5.datanucleus.persistence.queries.PersistenceQueryProcessor;
import org.apache.isis.persistence.jdo.datanucleus5.datanucleus.persistence.spi.JdoObjectIdSerializer;
import org.apache.isis.persistence.jdo.datanucleus5.objectadapter.ObjectAdapterContext;
import org.apache.isis.persistence.jdo.datanucleus5.persistence.command.CreateObjectCommand;
import org.apache.isis.persistence.jdo.datanucleus5.persistence.command.DestroyObjectCommand;
import org.apache.isis.persistence.jdo.datanucleus5.persistence.command.PersistenceCommand;
import org.apache.isis.persistence.jdo.datanucleus5.persistence.query.PersistenceQuery;
import org.apache.isis.persistence.jdo.datanucleus5.persistence.query.PersistenceQueryFindAllInstances;
import org.apache.isis.persistence.jdo.datanucleus5.persistence.query.PersistenceQueryFindUsingApplibQueryDefault;

import static org.apache.isis.core.commons.internal.base._Casts.uncheckedCast;

import lombok.Getter;
import lombok.val;
import lombok.extern.log4j.Log4j2;

/**
 * A wrapper around the JDO {@link PersistenceManager}.
 */
@Vetoed @Log4j2
public class PersistenceSession5 extends IsisPersistenceSessionJdoBase
implements IsisLifecycleListener.PersistenceSessionLifecycleManagement {

    private ObjectAdapterContext objectAdapterContext;
    @Getter private final TransactionService transactionService;
    private final StoreLifecycleListener storeLifecycleListener;

    /**
     * Initialize the object store so that calls to this object store access
     * persisted objects and persist changes to the object that are saved.
     * @param storeLifecycleListener 
     */
    public PersistenceSession5(
            final MetaModelContext metaModelContext,
            final PersistenceManagerFactory jdoPersistenceManagerFactory,
            final StoreLifecycleListener storeLifecycleListener, 
            final FixturesInstalledStateHolder stateHolder) {

        super(metaModelContext, jdoPersistenceManagerFactory, stateHolder);
        this.transactionService = metaModelContext.getTransactionService();
        this.storeLifecycleListener = storeLifecycleListener;
    }

    // -- open

    /**
     * Injects components, calls open on subcomponents, and then creates service
     * adapters.
     */
    @Override
    public void open() {
        ensureNotOpened();

        if (log.isDebugEnabled()) {
            log.debug("opening {}", this);
        }

        persistenceManager = jdoPersistenceManagerFactory.getPersistenceManager();

        final IsisLifecycleListener.PersistenceSessionLifecycleManagement psLifecycleMgmt = this;
        final IsisLifecycleListener isisLifecycleListener = new IsisLifecycleListener(psLifecycleMgmt);
        persistenceManager.addInstanceLifecycleListener(isisLifecycleListener, (Class[]) null);

        persistenceQueryProcessorByClass.put(
                PersistenceQueryFindAllInstances.class,
                new PersistenceQueryFindAllInstancesProcessor(this));
        persistenceQueryProcessorByClass.put(
                PersistenceQueryFindUsingApplibQueryDefault.class,
                new PersistenceQueryFindUsingApplibQueryProcessor(this));

        objectAdapterContext = ObjectAdapterContext.openContext(super.metaModelContext, this);

        // tell the proxy of all request-scoped services to instantiate the underlying
        // services, store onto the thread-local and inject into them...
        //startRequestOnRequestScopedServices();

        // ... and invoke all @PostConstruct
        //postConstructOnRequestScopedServices();

        val metricsService = metricsServiceProvider.get();
        if(metricsService instanceof InstanceLifecycleListener) {
            persistenceManager
            .addInstanceLifecycleListener((InstanceLifecycleListener)metricsService, (Class[]) null);
        }

        final Command command = commandService.create();
        final Interaction interaction = new Interaction();

        final Timestamp timestamp = clockService.nowAsJavaSqlTimestamp();
        final String userName = userService.getUser().getName();

        command.internal().setTimestamp(timestamp);
        command.internal().setUsername(userName);

        interaction.setUniqueId(command.getUniqueId());

        commandContextProvider.get().setCommand(command);
        interactionContextProvider.get().setInteraction(interaction);
        
        persistenceManager.addInstanceLifecycleListener(storeLifecycleListener, (Class[]) null);

        this.state = State.OPEN;
    }

    // -- close

    /**
     * Closes the subcomponents.
     *
     * <p>
     * Automatically {@link IsisTransactionManagerJdo#commitTransaction(IsisTransactionObject)
     * ends (commits)} the current (Isis) {@link IsisTransactionJdo}. This in turn commits the underlying
     * JDO transaction.
     *
     * <p>
     * The corresponding DataNucleus entity is then closed.
     */
    @Override
    public void close() {

        if (state == State.CLOSED) {
            // nothing to do
            return;
        }

        completeCommandFromInteractionAndClearDomainEvents();

        persistenceManager.removeInstanceLifecycleListener(storeLifecycleListener);
        
        try {
            persistenceManager.close();
        } catch(final Throwable ex) {
            // ignore
            log.error(
                    "close: failed to close JDO persistenceManager; continuing to avoid memory leakage");
        }

        objectAdapterContext.close();

        this.state = State.CLOSED;
    }

    private void completeCommandFromInteractionAndClearDomainEvents() {

        val command = commandContextProvider.get().getCommand();
        val interaction = interactionContextProvider.get().getInteraction();

        if(command.getStartedAt() != null && command.getCompletedAt() == null) {
            // the guard is in case we're here as the result of a redirect following a previous exception;just ignore.

            val priorInteractionExecution = interaction.getPriorExecution();
            final Timestamp completedAt =
                    priorInteractionExecution != null
                    ?
                        // copy over from the most recent (which will be the top-level) interaction
                        priorInteractionExecution.getCompletedAt()
                    :
                        // this could arise as the result of calling SessionManagementService#nextSession within an action
                        // the best we can do is to use the current time

                        // REVIEW: as for the interaction object, it is left somewhat high-n-dry.
                         clockService.nowAsJavaSqlTimestamp();

            command.internal().setCompletedAt(completedAt);
        }

        commandService.complete(command);

        interaction.clear();
    }

    @Override
    public Can<ManagedObject> allMatchingQuery(final Query<?> query) {
        val instances = findInstancesInTransaction(query, QueryCardinality.MULTIPLE);
        return instances;
    }
    
    @Override
    public Optional<ManagedObject> firstMatchingQuery(final Query<?> query) {
        val instances = findInstancesInTransaction(query, QueryCardinality.SINGLE);
        return instances.getFirst();
    }

    /**
     * Finds and returns instances that match the specified query.
     *
     * <p>
     * The {@link QueryCardinality} determines whether all instances or just the
     * first matching instance is returned.
     *
     * @throws org.apache.isis.persistence.jdo.applib.exceptions.UnsupportedFindException
     *             if the criteria is not support by this persistor
     */
    private Can<ManagedObject> findInstancesInTransaction(
            final Query<?> query, 
            final QueryCardinality cardinality) {
        
        if (log.isDebugEnabled()) {
            log.debug("findInstances using (applib) Query: {}", query);
        }

        // TODO: unify PersistenceQuery and PersistenceQueryProcessor
        final PersistenceQuery persistenceQuery = createPersistenceQueryFor(query, cardinality);
        if (log.isDebugEnabled()) {
            log.debug("maps to (core runtime) PersistenceQuery: {}", persistenceQuery);
        }

        final PersistenceQueryProcessor<? extends PersistenceQuery> processor = lookupProcessorFor(persistenceQuery);

        final Can<ManagedObject> instances = transactionService.executeWithinTransaction(
                ()->processPersistenceQuery(processor, persistenceQuery) );
        
        return instances;
        
        //XXX legacy of
        //final ObjectSpecification specification = persistenceQuery.getSpecification();
        //final FreeStandingList results = FreeStandingList.of(specification, instances);
        //return adapterFor(results);
    }

    /**
     * Converts the {@link Query applib representation of a query} into the
     * {@link PersistenceQuery NOF-internal representation}.
     */
    private final PersistenceQuery createPersistenceQueryFor(
            final Query<?> query,
            final QueryCardinality cardinality) {

        final PersistenceQuery persistenceQuery =
                persistenceQueryFactory.createPersistenceQueryFor(query, cardinality);
        if (persistenceQuery == null) {
            throw new IllegalArgumentException("Unknown Query type: " + query.getDescription());
        }

        return persistenceQuery;
    }

    private PersistenceQueryProcessor<? extends PersistenceQuery> lookupProcessorFor(final PersistenceQuery persistenceQuery) {
        final Class<? extends PersistenceQuery> persistenceQueryClass = persistenceQuery.getClass();
        final PersistenceQueryProcessor<? extends PersistenceQuery> processor =
                persistenceQueryProcessorByClass.get(persistenceQueryClass);
        if (processor == null) {
            throw new UnsupportedFindException(MessageFormat.format(
                    "Unsupported PersistenceQuery class: {0}", persistenceQueryClass.getName()));
        }
        return processor;
    }
    @SuppressWarnings("unchecked")
    private <Q extends PersistenceQuery> Can<ManagedObject> processPersistenceQuery(
            final PersistenceQueryProcessor<Q> persistenceQueryProcessor,
            final PersistenceQuery persistenceQuery) {
        return persistenceQueryProcessor.process((Q) persistenceQuery);
    }


    // -- fixture installation

//    @Override
//    public FixturesInstalledState getFixturesInstalledState() {
//        if (fixturesInstalledStateHolder.getFixturesInstalledState() == null) {
//            val initialStateFromConfig = initialStateFromConfig();
//            fixturesInstalledStateHolder.setFixturesInstalledState(initialStateFromConfig);
//        }
//        return fixturesInstalledStateHolder.getFixturesInstalledState();
//    }
//
//    /**
//     * Determine if the object store has been initialized with its set of start
//     * up objects.
//     *
//     * <p>
//     * This method is called only once after the session is opened called. If it returns <code>false</code> then the
//     * framework will run the fixtures to initialise the object store.
//     *
//     * <p>
//     * Implementation looks for the {@link IsisConfiguration.Persistence.JdoDatanucleus#isInstallFixtures()} property
//     * in the injected {@link #configuration configuration}.
//     *
//     * <p>
//     * By default this is not expected to be there, but utilities can add in on
//     * the fly during bootstrapping if required.
//     */
//    private FixturesInstalledState initialStateFromConfig() {
//        val installFixtures = configuration.getPersistence().getJdoDatanucleus().isInstallFixtures();
//        log.info("isFixturesInstalled: {} = {}", "'isis.persistence.jdo-datanucleus.install-fixtures'", installFixtures);
//
//        val objectStoreIsFixturesInstalled = !installFixtures;
//        val initialStateFromConfig = objectStoreIsFixturesInstalled
//                ? FixturesInstalledState.Installed
//                        : FixturesInstalledState.not_Installed;
//
//        return initialStateFromConfig;
//    }

    // -- FETCHING

    @Override
    public Object fetchPersistentPojo(final RootOid rootOid) {
        Objects.requireNonNull(rootOid);
        log.debug("getObject; oid={}", rootOid);

        Object result;
        try {
            val specLoader = super.getSpecificationLoader();
            val cls = clsOf(rootOid);
            val jdoObjectId = JdoObjectIdSerializer.toJdoObjectId(specLoader, rootOid);
            val fetchPlan = persistenceManager.getFetchPlan();
            fetchPlan.addGroup(FetchGroup.DEFAULT);
            result = persistenceManager.getObjectById(cls, jdoObjectId);
        } catch (final RuntimeException e) {

            Class<ExceptionRecognizer> serviceClass = ExceptionRecognizer.class;
            final Iterable<ExceptionRecognizer> exceptionRecognizers = lookupServices(serviceClass);
            for (ExceptionRecognizer exceptionRecognizer : exceptionRecognizers) {
                val recognition = exceptionRecognizer.recognize(e).orElse(null);
                if(recognition != null) {
                    if(recognition.getCategory() == ExceptionRecognizer.Category.NOT_FOUND) {
                        throw new ObjectNotFoundException(rootOid, e);
                    }
                }
            }

            throw e;
        }

        if (result == null) {
            throw new ObjectNotFoundException(rootOid);
        }

        return result;
    }

    @Override
    public Map<RootOid,Object> fetchPersistentPojos(final List<RootOid> rootOids) {

        if(rootOids.isEmpty()) {
            return Collections.emptyMap();
        }

        val specLoader = super.getSpecificationLoader();
        
        final List<Object> dnOids = new ArrayList<>(rootOids.size());
        for (val rootOid : rootOids) {
            final Object id = JdoObjectIdSerializer.toJdoObjectId(specLoader, rootOid);
            if(id instanceof SingleFieldIdentity) {
                dnOids.add(id);
            } else if (id instanceof String && ((String) id).contains("[OID]")) {
                final DatastoreIdImpl datastoreId = new DatastoreIdImpl((String)id);
                dnOids.add(datastoreId);
            } else {
                // application identity
                final DatastoreIdImpl datastoreId = new DatastoreIdImpl(clsOf(rootOid).getName(), id);
                dnOids.add(datastoreId);
            }
        }
        FetchPlan fetchPlan = persistenceManager.getFetchPlan();
        fetchPlan.addGroup(FetchGroup.DEFAULT);
        final List<Object> persistentPojos = new ArrayList<>(rootOids.size());
        try {
            final Collection<Object> pojos = uncheckedCast(persistenceManager.getObjectsById(dnOids, true));
            for (final Object pojo : pojos) {
                try {
                    persistentPojos.add(pojo);
                } catch(Exception ex) {
                    persistentPojos.add(null);
                }
            }
        } catch(NucleusObjectNotFoundException nonfe) {
            // at least one not found; fall back to loading one by one
            for (final Object dnOid : dnOids) {
                try {
                    final Object persistentPojo = persistenceManager.getObjectById(dnOid);
                    persistentPojos.add(persistentPojo);
                } catch(Exception ex) {
                    persistentPojos.add(null);
                }
            }
        }
        Map<RootOid, Object> pojoByOid = zip(rootOids, persistentPojos);
        return pojoByOid;
    }

    private static Map<RootOid, Object> zip(final List<RootOid> rootOids, final Collection<Object> pojos) {
        final Map<RootOid,Object> pojoByOid = _Maps.newLinkedHashMap();
        int i = 0;
        for (final Object pojo : pojos) {
            final RootOid rootOid = rootOids.get(i++);
            pojoByOid.put(rootOid, pojo);
        }
        return pojoByOid;
    }

    private Class<?> clsOf(final RootOid oid) {
        final ObjectSpecification objectSpec = getSpecificationLoader().lookupBySpecIdElseLoad(oid.getObjectSpecId());
        return objectSpec.getCorrespondingClass();
    }


    // -- REFRESH

    @Override
    public void refreshRoot(final Object domainObject) {

        val state = getEntityState(domainObject);
        val isRepresentingPersistent = state.isAttached() || state.isDestroyed();  

        if(!isRepresentingPersistent) {
            debugLogNotPersistentIgnoring(domainObject);
            return; // only resolve object that is representing persistent
        }

        debugLogRefreshImmediately(domainObject);

        try {
            persistenceManager.refresh(domainObject);
        } catch (final RuntimeException e) {
            final Oid oid = oidFor(domainObject);
            throw new PojoRefreshException(oid, e);
        }

        // possibly redundant because also called in the post-load event
        // listener, but (with JPA impl) found it was required if we were ever to
        // get an eager left-outer-join as the result of a refresh (sounds possible).
        initializeEntity((Persistable) domainObject);
    }

    // -- makePersistent


    @Override
    public void makePersistentInTransaction(final ManagedObject adapter) {
        
        val pojo = adapter.getPojo();
        
        if (getEntityState(pojo).isAttached()) {
            throw new NotPersistableException("Object already persistent: " + adapter);
        }
        val spec = adapter.getSpecification();
        if (spec.isManagedBean()) {
            throw new NotPersistableException("Can only persist entity beans: "+ adapter);
        }
        if (spec.getBeanSort().isCollection()) {
            //(FIXME not a perfect match) 
            //legacy of ... 
            //getOid() instanceof ParentedOid;
            //or should we just ignore this?
            throw new NotPersistableException("Cannot persist parented collection: " + adapter);
        }
        
        transactionService.executeWithinTransaction(()->{
            log.debug("persist {}", adapter);               
            val createObjectCommand = newCreateObjectCommand(adapter);
            transactionManager.addCommand(createObjectCommand);
        });
    }
    
    // -- destroyObjectInTransaction


    @Override
    public void destroyObjectInTransaction(final ManagedObject adapter) {
        val spec = adapter.getSpecification();
        if (spec.isParented()) {
            return;
        }
        log.debug("destroyObject {}", adapter);
        transactionService.executeWithinTransaction(()->{
            val destroyObjectCommand = newDestroyObjectCommand(adapter);
            transactionManager.addCommand(destroyObjectCommand);
        });
    }

    // -- newXxxCommand
    /**
     * Makes an {@link ObjectAdapter} persistent. The specified object should be
     * stored away via this object store's persistence mechanism, and have an
     * new and unique OID assigned to it (by calling the object's
     * <code>setOid</code> method). The object, should also be added to the
     * cache as the object is implicitly 'in use'.
     *
     * <p>
     * If the object has any associations then each of these, where they aren't
     * already persistent, should also be made persistent by recursively calling
     * this method.
     * </p>
     *
     * <p>
     * If the object to be persisted is a collection, then each element of that
     * collection, that is not already persistent, should be made persistent by
     * recursively calling this method.
     * </p>
     *
     */
    private CreateObjectCommand newCreateObjectCommand(final ManagedObject adapter) {

        ensureOpened();
        
        val pojo = adapter.getPojo();

        log.debug("create object - creating command for: {}", adapter);
        if (getEntityState(pojo).isAttached()) {
            throw new IllegalArgumentException("Adapter is persistent; adapter: " + adapter);
        }
        return new DataNucleusCreateObjectCommand(adapter, this);
    }

    private DestroyObjectCommand newDestroyObjectCommand(final ManagedObject adapter) {
        
        ensureOpened();
        
        val pojo = adapter.getPojo();

        log.debug("destroy object - creating command for: {}", adapter);
        if (!getEntityState(pojo).isAttached()) {
            throw new IllegalArgumentException("Adapter is not persistent; adapter: " + adapter);
        }
        return new DataNucleusDeleteObjectCommand(adapter, persistenceManager);
    }


    // -- execute
    @Override
    public void execute(final List<PersistenceCommand> commands) {

        // previously we used to check that there were some commands, and skip processing otherwise.
        // we no longer do that; it could be (is quite likely) that DataNucleus has some dirty objects anyway that
        // don't have commands wrapped around them...

        executeCommands(commands);
    }

    private void executeCommands(final List<PersistenceCommand> commands) {

        for (final PersistenceCommand command : commands) {
            command.execute();
        }
        persistenceManager.flush();
    }

    // -- FrameworkSynchronizer delegate methods

    @Override
    public void enlistDeletingAndInvokeIsisRemovingCallbackFacet(final Persistable pojo) {
        ObjectAdapter adapter = adapterFor(pojo);

        changedObjectsServiceProvider.get().enlistDeleting(adapter);

        CallbackFacet.Util.callCallback(adapter, RemovingCallbackFacet.class);
        objectAdapterContext.postLifecycleEventIfRequired(adapter, RemovingLifecycleEventFacet.class);
    }

    @Override
    public ObjectAdapter initializeEntity(final Persistable pojo) {

//        // need to do eagerly, because (if a viewModel then) a
//        // viewModel's #viewModelMemento might need to use services
//        serviceInjector.injectServicesInto(pojo); //redundant

        final RootOid originalOid = objectAdapterContext.createPersistentOrViewModelOid(pojo);
        final ObjectAdapter adapter = objectAdapterContext.recreatePojo(originalOid, pojo);

        CallbackFacet.Util.callCallback(adapter, LoadedCallbackFacet.class);
        objectAdapterContext.postLifecycleEventIfRequired(adapter, LoadedLifecycleEventFacet.class);

        return adapter;
    }

    @Override
    public String identifierFor(final Object pojo) {
        final Object jdoOid = getJdoPersistenceManager().getObjectId(pojo);
        requireNonNull(jdoOid, 
                ()->String.format("Pojo of type '%s' is not recognized by JDO.", 
                        pojo.getClass().getName()));
        return JdoObjectIdSerializer.toOidIdentifier(jdoOid);
    }


    /**
     * Called either when an entity is initially persisted, or when an entity is updated; fires the appropriate
     * lifecycle callback.
     *
     * <p>
     * The implementation therefore uses Isis' {@link Oid#isTransient() oid}
     * to determine which callback to fire.
     */
    @Override
    public void invokeIsisPersistingCallback(final Persistable pojo) {
        if (getEntityState(pojo).isDetached()) {
            val managedObject = ManagedObject.of(specificationLoader::loadSpecification, pojo);

            // persisting
            // previously this was performed in the DataNucleusSimplePersistAlgorithm.
            CallbackFacet.Util.callCallback(managedObject, PersistingCallbackFacet.class);
            objectAdapterContext.postLifecycleEventIfRequired(managedObject, PersistingLifecycleEventFacet.class);

        } else {
            // updating

            // don't call here, already called in preDirty.

            // CallbackFacet.Util.callCallback(adapter, UpdatingCallbackFacet.class);
        }
    }

    /**
     * Called either when an entity is initially persisted, or when an entity is updated;
     * fires the appropriate lifecycle callback
     *
     * <p>
     * The implementation therefore uses Isis' {@link Oid#isTransient() oid}
     * to determine which callback to fire.
     */
    @Override
    public void enlistCreatedAndInvokeIsisPersistedCallback(final Persistable pojo) {

        val adapter = adapterFor(pojo);

        final boolean wasAlreadyEnlisted = changedObjectsServiceProvider.get().isEnlisted(adapter);
        changedObjectsServiceProvider.get().enlistCreated(adapter);

        if(!wasAlreadyEnlisted) {
            CallbackFacet.Util.callCallback(adapter, PersistedCallbackFacet.class);
            objectAdapterContext.postLifecycleEventIfRequired(adapter, PersistedLifecycleEventFacet.class);
        }

    }

    @Override
    public void enlistUpdatingAndInvokeIsisUpdatingCallback(final Persistable pojo) {

        final ObjectAdapter adapter = objectAdapterContext.fetchPersistent(pojo);
        if (adapter == null) {
            throw new RuntimeException(
                    String.format("DN could not find objectId for pojo (unexpected); pojo=[%s]", pojo));
        }

        final boolean wasAlreadyEnlisted = changedObjectsServiceProvider.get().isEnlisted(adapter);

        // we call this come what may;
        // additional properties may now have been changed, and the changeKind for publishing might also be modified
        changedObjectsServiceProvider.get().enlistUpdating(adapter);

        if(!wasAlreadyEnlisted) {
            // prevent an infinite loop... don't call the 'updating()' callback on this object if we have already done so
            CallbackFacet.Util.callCallback(adapter, UpdatingCallbackFacet.class);
            objectAdapterContext.postLifecycleEventIfRequired(adapter, UpdatingLifecycleEventFacet.class);
        }

    }

    @Override
    public void invokeIsisUpdatedCallback(Persistable pojo) {

        final ObjectAdapter adapter = objectAdapterContext.fetchPersistent(pojo);
        if (adapter == null) {
            throw new RuntimeException(
                    String.format("DN could not find objectId for pojo (unexpected); pojo=[%s]", pojo));
        }

        // the callback and transaction.enlist are done in the preStore callback
        // (can't be done here, as the enlist requires to capture the 'before' values)
        CallbackFacet.Util.callCallback(adapter, UpdatedCallbackFacet.class);
        objectAdapterContext.postLifecycleEventIfRequired(adapter, UpdatedLifecycleEventFacet.class);
    }

    /**
     * makes sure the entity is known to Isis and is a root
     * @param pojo
     */
    @Override
    public void ensureRootObject(final Persistable pojo) {
        final Oid oid = adapterFor(pojo).getOid();
        if (!(oid instanceof RootOid)) {
            throw new IsisException(MessageFormat.format("Not a RootOid: oid={0}, for {1}", oid, pojo));
        }
    }

    @Override
    public EntityState getEntityState(@Nullable Object pojo) {

        // guard against misuse
        if(pojo instanceof ObjectAdapter) {
            throw _Exceptions.unexpectedCodeReach();
        }

        if (pojo!=null && pojo instanceof Persistable) {
            val persistable = (Persistable) pojo;
            val isDeleted = persistable.dnIsDeleted();
            if(isDeleted) {
                return EntityState.PERSISTABLE_DESTROYED;
            }
            val isPersistent = persistable.dnIsPersistent();
            if(isPersistent) {
                return EntityState.PERSISTABLE_ATTACHED;
            }
            return EntityState.PERSISTABLE_DETACHED;
        }
        return EntityState.NOT_PERSISTABLE;
    }

    @Override
    public boolean isRecognized(Object pojo) {
        if (pojo!=null && pojo instanceof Persistable) {
            final Object jdoOid = getJdoPersistenceManager().getObjectId(pojo);
            if(jdoOid!=null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ObjectAdapter adapterFor(Object pojo) {
        return objectAdapterContext.getObjectAdapterProvider().adapterFor(pojo);
    }
    
    // -- HELPER
    
    private void debugLogNotPersistentIgnoring(Object domainObject) {
        if (log.isDebugEnabled() && domainObject!=null) {
            val oid = oidFor(domainObject);
            log.debug("; oid={} not persistent - ignoring", oid.enString());
        }     
    }

    private void debugLogRefreshImmediately(Object domainObject) {
        if (log.isDebugEnabled()) {
            val oid = oidFor(domainObject);
            log.debug("refresh immediately; oid={}", oid.enString());
        }
    }

}



