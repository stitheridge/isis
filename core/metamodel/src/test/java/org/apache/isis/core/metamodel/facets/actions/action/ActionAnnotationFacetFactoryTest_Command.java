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
package org.apache.isis.core.metamodel.facets.actions.action;

import java.lang.reflect.Method;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.core.config.metamodel.facets.CommandActionsConfiguration;
import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facets.FacetFactory.ProcessMethodContext;
import org.apache.isis.core.metamodel.facets.actions.action.command.CommandFacetForActionAnnotation;
import org.apache.isis.core.metamodel.facets.actions.action.command.CommandFacetForActionAnnotationAsConfigured;
import org.apache.isis.core.metamodel.facets.actions.action.command.CommandFacetFromConfiguration;
import org.apache.isis.core.metamodel.facets.actions.command.CommandFacet;
import org.apache.isis.core.metamodel.facets.actions.publish.PublishedActionFacet;
import org.apache.isis.core.metamodel.facets.actions.semantics.ActionSemanticsFacetAbstract;

import lombok.val;

public class ActionAnnotationFacetFactoryTest_Command extends ActionAnnotationFacetFactoryTest {

    private void processCommand(
            ActionAnnotationFacetFactory facetFactory, ProcessMethodContext processMethodContext) {
        val actionIfAny = processMethodContext.synthesizeOnMethod(Action.class);
        facetFactory.processCommand(processMethodContext, actionIfAny);
    }
    
    @Test
    public void given_HasUniqueId_thenIgnored() {
        // given
        final Method actionMethod = findMethod(SomeHasUniqueId.class, "someAction");

        // when
        processCommand(facetFactory, new ProcessMethodContext(SomeHasUniqueId.class, null, actionMethod, mockMethodRemover, facetedMethod));

        // then
        final Facet facet = facetedMethod.getFacet(CommandFacet.class);
        assertNull(facet);

    }

    @Test
    public void given_noAnnotation_and_configurationSetToIgnoreQueryOnly_andSafeSemantics_thenNone() {

        // given
        allowingCommandConfigurationToReturn(CommandActionsConfiguration.IGNORE_QUERY_ONLY);
        final Method actionMethod = findMethod(ActionAnnotationFacetFactoryTest.Customer.class, "someAction");

        facetedMethod.addFacet(new ActionSemanticsFacetAbstract(SemanticsOf.SAFE, facetedMethod) {});

        // when
        processCommand(facetFactory, new ProcessMethodContext(ActionAnnotationFacetFactoryTest.Customer.class, null,
                actionMethod, mockMethodRemover, facetedMethod));

        // then
        final Facet facet = facetedMethod.getFacet(CommandFacet.class);
        assertNull(facet);
    }

    @Test
    public void given_noAnnotation_and_configurationSetToIgnoreQueryOnly_andNonSafeSemantics_thenAdded() {

        // given
        allowingCommandConfigurationToReturn(CommandActionsConfiguration.IGNORE_QUERY_ONLY);
        final Method actionMethod = findMethod(ActionAnnotationFacetFactoryTest.Customer.class, "someAction");

        facetedMethod.addFacet(new ActionSemanticsFacetAbstract(SemanticsOf.IDEMPOTENT, facetedMethod) {});

        // when
        processCommand(facetFactory, new ProcessMethodContext(ActionAnnotationFacetFactoryTest.Customer.class, null,
                actionMethod, mockMethodRemover, facetedMethod));

        // then
        final Facet facet = facetedMethod.getFacet(CommandFacet.class);
        assertNotNull(facet);
        assertTrue(facet instanceof  CommandFacetFromConfiguration);
        final CommandFacetFromConfiguration facetImpl = (CommandFacetFromConfiguration) facet;
        assertThat(facetImpl.persistence(), is(org.apache.isis.applib.annotation.CommandPersistence.PERSISTED));
        assertThat(facetImpl.executeIn(), is(org.apache.isis.applib.annotation.CommandExecuteIn.FOREGROUND));
    }

    @Test(expected=IllegalStateException.class)
    public void given_noAnnotation_and_configurationSetToIgnoreQueryOnly_andNoSemantics_thenException() {

        // given
        allowingCommandConfigurationToReturn(CommandActionsConfiguration.IGNORE_QUERY_ONLY);
        final Method actionMethod = findMethod(ActionAnnotationFacetFactoryTest.Customer.class, "someAction");

        // when
        processCommand(facetFactory, new ProcessMethodContext(ActionAnnotationFacetFactoryTest.Customer.class, null,
                actionMethod, mockMethodRemover, facetedMethod));
    }

    @Test
    public void given_noAnnotation_and_configurationSetToNone_thenNone() {

        // given
        allowingCommandConfigurationToReturn(CommandActionsConfiguration.NONE);
        final Method actionMethod = findMethod(ActionAnnotationFacetFactoryTest.Customer.class, "someAction");

        // when
        processCommand(facetFactory, new ProcessMethodContext(ActionAnnotationFacetFactoryTest.Customer.class, null,
                actionMethod, mockMethodRemover, facetedMethod));

        // then
        final Facet facet = facetedMethod.getFacet(PublishedActionFacet.class);
        assertNull(facet);
    }

    @Test
    public void given_noAnnotation_and_configurationSetToAll_thenFacetAdded() {

        // given
        final Method actionMethod = findMethod(ActionAnnotationFacetFactoryTest.Customer.class, "someAction");

        allowingCommandConfigurationToReturn(CommandActionsConfiguration.ALL);

        // when
        processCommand(facetFactory, new ProcessMethodContext(ActionAnnotationFacetFactoryTest.Customer.class, null,
                actionMethod, mockMethodRemover, facetedMethod));

        // then
        final Facet facet = facetedMethod.getFacet(CommandFacet.class);
        assertNotNull(facet);
        assert(facet instanceof CommandFacetFromConfiguration);
    }

    @Test
    public void given_asConfigured_and_configurationSetToIgnoreQueryOnly_andSafeSemantics_thenNone() {

        class Customer {
            @Action(command = CommandReification.AS_CONFIGURED)
            public void someAction() {
            }
        }

        allowingCommandConfigurationToReturn(CommandActionsConfiguration.IGNORE_QUERY_ONLY);
        final Method actionMethod = findMethod(Customer.class, "someAction");

        facetedMethod.addFacet(new ActionSemanticsFacetAbstract(SemanticsOf.SAFE, facetedMethod) {});

        processCommand(facetFactory, new ProcessMethodContext(Customer.class, null, actionMethod, mockMethodRemover, facetedMethod));

        final Facet facet = facetedMethod.getFacet(CommandFacet.class);
        assertNull(facet);
    }

    @Test
    public void given_asConfigured_and_configurationSetToIgnoreQueryOnly_andNonSafeSemantics_thenAdded() {

        // given
        class Customer {
            @Action(
                    command = CommandReification.AS_CONFIGURED,
                    commandPersistence = CommandPersistence.IF_HINTED,
                    commandExecuteIn = CommandExecuteIn.BACKGROUND
                    )
            public void someAction() {
            }
        }

        allowingCommandConfigurationToReturn(CommandActionsConfiguration.IGNORE_QUERY_ONLY);
        final Method actionMethod = findMethod(Customer.class, "someAction");

        facetedMethod.addFacet(new ActionSemanticsFacetAbstract(SemanticsOf.IDEMPOTENT, facetedMethod) {});

        // when
        processCommand(facetFactory, new ProcessMethodContext(Customer.class, null, actionMethod, mockMethodRemover, facetedMethod));

        // then
        final Facet facet = facetedMethod.getFacet(CommandFacet.class);
        assertNotNull(facet);
        final CommandFacetForActionAnnotationAsConfigured facetImpl = (CommandFacetForActionAnnotationAsConfigured) facet;
        assertThat(facetImpl.persistence(), is(org.apache.isis.applib.annotation.CommandPersistence.IF_HINTED));
        assertThat(facetImpl.executeIn(), is(org.apache.isis.applib.annotation.CommandExecuteIn.BACKGROUND));
    }

    @Test(expected=IllegalStateException.class)
    public void given_asConfigured_and_configurationSetToIgnoreQueryOnly_andNoSemantics_thenException() {

        class Customer {
            @Action(command = CommandReification.AS_CONFIGURED)
            public void someAction() {
            }
        }

        allowingCommandConfigurationToReturn(CommandActionsConfiguration.IGNORE_QUERY_ONLY);
        final Method actionMethod = findMethod(Customer.class, "someAction");

        processCommand(facetFactory, new ProcessMethodContext(Customer.class, null, actionMethod, mockMethodRemover, facetedMethod));
    }

    @Test
    public void given_asConfigured_and_configurationSetToNone_thenNone() {

        class Customer {
            @Action(command = CommandReification.AS_CONFIGURED)
            public void someAction() {
            }
        }

        allowingCommandConfigurationToReturn(CommandActionsConfiguration.NONE);
        final Method actionMethod = findMethod(Customer.class, "someAction");

        processCommand(facetFactory, new ProcessMethodContext(Customer.class, null, actionMethod, mockMethodRemover, facetedMethod));

        final Facet facet = facetedMethod.getFacet(CommandFacet.class);
        assertNull(facet);
    }

    @Test
    public void given_asConfigured_and_configurationSetToAll_thenFacetAdded() {

        // given
        class Customer {
            @Action(
                    command = CommandReification.AS_CONFIGURED,
                    commandPersistence = CommandPersistence.IF_HINTED,
                    commandExecuteIn = CommandExecuteIn.BACKGROUND
                    )
            public void someAction() {
            }
        }
        final Method actionMethod = findMethod(Customer.class, "someAction");

        allowingCommandConfigurationToReturn(CommandActionsConfiguration.ALL);

        // when
        processCommand(facetFactory, new ProcessMethodContext(Customer.class, null, actionMethod, mockMethodRemover, facetedMethod));

        // then
        final Facet facet = facetedMethod.getFacet(CommandFacet.class);
        assertNotNull(facet);
        final CommandFacetForActionAnnotationAsConfigured facetImpl = (CommandFacetForActionAnnotationAsConfigured) facet;
        assertThat(facetImpl.persistence(), is(org.apache.isis.applib.annotation.CommandPersistence.IF_HINTED));
        assertThat(facetImpl.executeIn(), is(org.apache.isis.applib.annotation.CommandExecuteIn.BACKGROUND));
    }

    @Test
    public void given_enabled_irrespectiveOfConfiguration_thenFacetAdded() {

        // given
        class Customer {
            @Action(command = CommandReification.ENABLED)
            public void someAction() {
            }
        }
        final Method actionMethod = findMethod(Customer.class, "someAction");

        // even though configuration is disabled
        allowingCommandConfigurationToReturn(CommandActionsConfiguration.NONE);

        // when
        processCommand(facetFactory, new ProcessMethodContext(Customer.class, null, actionMethod, mockMethodRemover, facetedMethod));

        // then
        final Facet facet = facetedMethod.getFacet(CommandFacet.class);
        assertNotNull(facet);
        assertTrue(facet instanceof CommandFacetForActionAnnotation);
    }

    @Test
    public void given_disabled_irrespectiveOfConfiguration_thenNone() {

        // given
        class Customer {
            @Action(command = CommandReification.DISABLED)
            public void someAction() {
            }
        }
        final Method actionMethod = findMethod(Customer.class, "someAction");

        // even though configuration is disabled
        allowingCommandConfigurationToReturn(CommandActionsConfiguration.NONE);

        // when
        processCommand(facetFactory, new ProcessMethodContext(Customer.class, null, actionMethod, mockMethodRemover, facetedMethod));

        // then
        final Facet facet = facetedMethod.getFacet(CommandFacet.class);
        assertNull(facet);
    }


}