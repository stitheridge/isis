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
package org.apache.isis.core.runtimeservices.background;

import java.util.List;

import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.command.CommandExecutorService;
import org.apache.isis.core.commons.internal.collections._Lists;

import lombok.val;
import lombok.extern.log4j.Log4j2;

/**
 * Intended to be used as a base class for executing queued up {@link Command background action}s.
 *
 * <p>
 * This implementation uses the {@link #findBackgroundCommandsToExecute() hook method} so that it is
 * independent of the location where the actions have actually been persisted to.
 */
@Log4j2
public abstract class BackgroundCommandExecution extends CommandExecutionAbstract {

    /**
     * Defaults to the historical defaults * for running background commands.
     */
    public BackgroundCommandExecution() {
        this(CommandExecutorService.SudoPolicy.NO_SWITCH);
    }

    public BackgroundCommandExecution(final CommandExecutorService.SudoPolicy sudoPolicy) {
        super(sudoPolicy);
    }

    // //////////////////////////////////////

    @Override
    protected void doExecute(Object context) {

        val commands = _Lists.<Command>newArrayList();
        transactionService.executeWithinTransaction(() -> {
            commands.addAll(findBackgroundCommandsToExecute());
        });

        log.debug("Found {} to execute", commands.size());

        for (val command : commands) {
            execute((CommandWithDto) command, transactionService);
        }
    }

    /**
     * Mandatory hook method
     */
    protected abstract List<? extends Command> findBackgroundCommandsToExecute();

}
