/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.isis.extensions.commandreplay.impl.executor;

import java.sql.Timestamp;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import javax.inject.Named;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.jaxb.JavaSqlXMLGregorianCalendarMarshalling;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.command.CommandExecutorService;
import org.apache.isis.extensions.commandreplay.impl.clock.TickingClockService;
import org.apache.isis.schema.cmd.v2.CommandDto;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * Override of {@link CommandExecutorService} that also sets the time (using the {@link TickingClockService}) to that
 * of the {@link Command}'s {@link Command#getTimestamp() timestamp} before executing the command.
 *
 * <p>
 *     It then delegates down to the default implementation.
 * </p>
 */
@DomainService()
@Log4j2
public class CommandExecutorServiceWithTime implements CommandExecutorService {

    final CommandExecutorService delegate;
    final TickingClockService tickingClockService;

    public CommandExecutorServiceWithTime(
            @Named("Default")
            CommandExecutorService delegate
            , TickingClockService tickingClockService) {
        this.delegate = delegate;
        this.tickingClockService = tickingClockService;
    }

    @Override
    public Bookmark executeCommand(Command command) {
        final Supplier<Bookmark> executeCommand = () -> delegate.executeCommand(command);
        return tickingClockService.isInitialized()
                ? tickingClockService.at(command.getTimestamp(), executeCommand)
                : executeCommand.get();
    }

    @Override
    public Bookmark executeCommand(final SudoPolicy sudoPolicy, final Command command) {
        final Supplier<Bookmark> executeCommand = () -> delegate.executeCommand(sudoPolicy, command);
        return tickingClockService.isInitialized()
                ? tickingClockService.at(command.getTimestamp(), executeCommand)
                : executeCommand.get();
    }

    @Override
    public Bookmark executeCommand(final CommandDto commandDto) {
        final Supplier<Bookmark> executeCommand = () -> delegate.executeCommand(commandDto);
        return tickingClockService.isInitialized()
                ? tickingClockService.at(
                        JavaSqlXMLGregorianCalendarMarshalling.toTimestamp(commandDto.getTimestamp()), executeCommand)
                : executeCommand.get();
    }

    @Override
    public Bookmark executeCommand(
            final SudoPolicy sudoPolicy, final CommandDto commandDto) {
        final Supplier<Bookmark> executeCommand = () -> delegate.executeCommand(sudoPolicy, commandDto);
        return tickingClockService.isInitialized()
                ? tickingClockService.at(
                    JavaSqlXMLGregorianCalendarMarshalling.toTimestamp(commandDto.getTimestamp()), executeCommand)
                : executeCommand.get();
    }

}
