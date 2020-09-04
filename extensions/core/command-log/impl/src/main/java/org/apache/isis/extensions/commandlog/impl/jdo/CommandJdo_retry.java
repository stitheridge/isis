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
package org.apache.isis.extensions.commandlog.impl.jdo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.command.CommandContext;
import org.apache.isis.applib.services.command.CommandExecutorService;
import org.apache.isis.applib.services.jaxb.JaxbService;
import org.apache.isis.applib.util.schema.CommandDtoUtils;
import org.apache.isis.extensions.commandlog.impl.IsisModuleExtCommandLogImpl;
import org.apache.isis.schema.cmd.v2.CommandDto;

import lombok.val;

@Action(
    semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE
    , domainEvent = CommandJdo_retry.ActionDomainEvent.class
)
public class CommandJdo_retry {

    private final CommandJdo commandJdo;
    public CommandJdo_retry(CommandJdo commandJdo) {
        this.commandJdo = commandJdo;
    }

    public static class ActionDomainEvent extends IsisModuleExtCommandLogImpl.ActionDomainEvent<CommandJdo_retry> { }
    @MemberOrder(name = "executeIn", sequence = "1")
    public CommandJdo act() {

        val dto = commandJdo.getCommandDto();
        commandExecutorService.executeCommand(dto);

        return commandJdo;
    }

    @Inject CommandExecutorService commandExecutorService;
}
