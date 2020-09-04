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
package org.apache.isis.applib.services.conmap.command;

import java.sql.Timestamp;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.annotation.OrderPrecedence;
import org.apache.isis.applib.jaxb.JavaSqlXMLGregorianCalendarMarshalling;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.command.CommandDtoProcessor;
import org.apache.isis.applib.services.conmap.ContentMappingService;
import org.apache.isis.applib.services.conmap.command.spi.CommandDtoProcessorService;
import org.apache.isis.applib.services.metamodel.MetaModelService;
import org.apache.isis.applib.util.schema.CommandDtoUtils;
import org.apache.isis.schema.cmd.v2.CommandDto;
import org.apache.isis.schema.common.v2.PeriodDto;

@Service
@Named("isisApplib.ContentMappingServiceForCommandDto")
@Order(OrderPrecedence.EARLY)
@Primary
@Qualifier("CommandDto")
public class ContentMappingServiceForCommandDto implements ContentMappingService {

    @Override
    public Object map(Object object, final List<MediaType> acceptableMediaTypes) {
        final boolean supported = Util.isSupported(CommandDto.class, acceptableMediaTypes);
        if(!supported) {
            return null;
        }

        return asProcessedDto(object);
    }

    /**
     * Not part of the {@link ContentMappingService} API.
     */
    public CommandDto map(final Command command) {
        return asProcessedDto(command);
    }

    CommandDto asProcessedDto(final Object object) {
        if (!(object instanceof Command)) {
            return null;
        }
        final Command command = (Command) object;
        return asProcessedDto(command);
    }

    private CommandDto asProcessedDto(final Command command) {
        if(command == null) {
            return null;
        }
        CommandDto commandDto = command.getCommandDto();

        // global processors
        for (final CommandDtoProcessorService commandDtoProcessorService : commandDtoProcessorServices) {
            commandDto = commandDtoProcessorService.process(command, commandDto);
            if(commandDto == null) {
                // any processor could return null, effectively breaking the chain.
                return null;
            }
        }

        // specific processors for this specific member (action or property)
        final CommandDtoProcessor commandDtoProcessor =
                metaModelService.commandDtoProcessorFor(commandDto.getMember().getLogicalMemberIdentifier());
        if (commandDtoProcessor == null) {
            return commandDto;
        }
        return commandDtoProcessor.process(command, commandDto);
    }


    /**
     * Uses the SPI infrastructure to copy over standard properties from {@link Command} to {@link CommandDto}.
     */
    @Service
    @Named("isisApplib.ContentMappingServiceForCommandDto.CopyOverFromCommand")
    // specify quite a high priority since custom processors will probably want to run after this one
    // (but can choose to run before if they wish)
    @Order(OrderPrecedence.EARLY)
    @Qualifier("Command")
    public static class CopyOverFromCommand implements CommandDtoProcessorService {

        @Override
        public CommandDto process(final Command command, CommandDto commandDto) {

            // for some reason this isn't being persisted initially, so patch it in.  TODO: should fix this
            commandDto.setUser(command.getUsername());

            // the timestamp field was only introduced in v1.4 of cmd.xsd, so there's no guarantee
            // it will have been populated.  We therefore copy the value in from CommandWithDto entity.
            if(commandDto.getTimestamp() == null) {
                final Timestamp timestamp = command.getTimestamp();
                commandDto.setTimestamp(JavaSqlXMLGregorianCalendarMarshalling.toXMLGregorianCalendar(timestamp));
            }

            final Bookmark result = command.getResult();
            CommandDtoUtils.setUserData(commandDto,
                    CommandWithDto.USERDATA_KEY_RETURN_VALUE, result != null ? result.toString() : null);
            // knowing whether there was an exception is on the master is used to determine whether to
            // continue when replayed on the slave if an exception occurs there also
            CommandDtoUtils.setUserData(commandDto,
                    CommandWithDto.USERDATA_KEY_EXCEPTION, command.getException());

            PeriodDto timings = CommandDtoUtils.timingsFor(commandDto);
            timings.setStartedAt(JavaSqlXMLGregorianCalendarMarshalling.toXMLGregorianCalendar(command.getStartedAt()));
            timings.setCompletedAt(JavaSqlXMLGregorianCalendarMarshalling.toXMLGregorianCalendar(command.getCompletedAt()));

            return commandDto;
        }
    }


    @Inject
    MetaModelService metaModelService;

    @Inject
    List<CommandDtoProcessorService> commandDtoProcessorServices;

}
