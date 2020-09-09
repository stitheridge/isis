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
package org.apache.isis.applib.services.command;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.annotation.IsisInteractionScope;
import org.apache.isis.applib.annotation.OrderPrecedence;
import org.apache.isis.applib.services.TransactionScopeListener;
import org.apache.isis.applib.services.inject.ServiceInjector;
import org.apache.isis.applib.services.metrics.MetricsService;
import org.apache.isis.applib.services.registry.ServiceRegistry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This service (API and implementation) provides access to context information about any {@link Command}.
 *
 * This implementation has no UI and there is only one implementation (this class) in applib, so it is annotated with
 * {@link org.apache.isis.applib.annotation.DomainService}.  This means that it is automatically registered and
 * available for use; no further configuration is required.
 */
// tag::refguide[]
@Service
@Named("isisApplib.CommandContext")
@Order(OrderPrecedence.EARLY - 10) // before ChangedObjectService
@Primary
@Qualifier("Default")
@IsisInteractionScope
@RequiredArgsConstructor(onConstructor_ = {@Inject})
//@Log4j2
public class CommandContext implements TransactionScopeListener, DisposableBean {

    private final ServiceInjector serviceInjector;
    private final MetricsService metricsService;

    @Getter
    private Command command;

    // end::refguide[]
    /**
     * <b>NOT API</b>: intended to be called only by the framework.
     */
    public void setCommand(final Command command) {
        this.command = command;
        if(command!=null) {
            serviceInjector.injectServicesInto(command);
        }
    }

    @Override
    public void destroy() throws Exception {
        setCommand(null);
    }

    @Override
    public void onTransactionEnded() {
        getCommand().internal().setSystemStateChanged(
                getCommand().isSystemStateChanged() ||
                metricsService.numberObjectsDirtied() > 0);
    }

    // tag::refguide[]
}
// end::refguide[]
