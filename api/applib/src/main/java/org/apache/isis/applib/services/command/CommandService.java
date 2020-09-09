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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.annotation.IsisInteractionScope;
import org.apache.isis.applib.annotation.OrderPrecedence;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.command.spi.CommandServiceListener;

import lombok.extern.log4j.Log4j2;

@Service
@Named("isisApplib.CommandService")
@Order(OrderPrecedence.MIDPOINT)
@Primary
@Qualifier("Default")
@Log4j2
// tag::refguide[]
public class CommandService {

    // end::refguide[]
    /**
     * Simply instantiates the appropriate instance of the {@link Command}.
     *
     * <p>
     * Its members will be populated automatically by the framework (the
     * {@link Command}'s {@link Command#getTimestamp()},
     * {@link Command#getUsername()} and {@link Command#getUniqueId()}).
     * </p>
     */
    // tag::refguide[]
    public Command create() {                   // <.>
        return new Command();
    }

    // end::refguide[]

    /**
     * &quot;Complete&quot; the command, providing an opportunity ot persist
     * a memento of the command if the
     * {@link Command#isSystemStateChanged() system state has changed}.
     *
     * <p>
     *     The framework will automatically have set the {@link Command#getCompletedAt()} property.
     * </p>
     */
    // tag::refguide[]
    public void complete(final Command command) {   // <.>
        // ...
    // end::refguide[]

        if(command.getLogicalMemberIdentifier() == null) {
            // eg if seed fixtures
            return;
        }

        log.debug("complete: {}, systemStateChanged {}", command.getLogicalMemberIdentifier(), command.isSystemStateChanged());

    // tag::refguide[]
        commandServiceListeners.forEach(x -> x.onComplete(command));
    }

    @Inject List<CommandServiceListener> commandServiceListeners;

}
// end::refguide[]
