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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.annotation.OrderPrecedence;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.jaxb.JavaSqlXMLGregorianCalendarMarshalling;
import org.apache.isis.applib.query.Query;
import org.apache.isis.applib.query.QueryDefault;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.command.CommandContext;
import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.applib.util.schema.CommandDtoUtils;
import org.apache.isis.persistence.jdo.applib.services.IsisJdoSupport_v3_2;
import org.apache.isis.schema.cmd.v2.CommandDto;
import org.apache.isis.schema.cmd.v2.CommandsDto;
import org.apache.isis.schema.cmd.v2.MapDto;
import org.apache.isis.schema.common.v2.OidDto;

import lombok.val;
import lombok.var;
import lombok.extern.log4j.Log4j2;

/**
 * Provides supporting functionality for querying and persisting
 * {@link CommandJdo command} entities.
 */
@Service
@Named("isisExtensionsCommandLog.CommandJdoRepository")
@Order(OrderPrecedence.MIDPOINT)
@Qualifier("Jdo")
@Log4j2
public class CommandJdoRepository {

    public List<CommandJdo> findByFromAndTo(
            final LocalDate from, final LocalDate to) {
        final Timestamp fromTs = toTimestampStartOfDayWithOffset(from, 0);
        final Timestamp toTs = toTimestampStartOfDayWithOffset(to, 1);
        
        final Query<CommandJdo> query;
        if(from != null) {
            if(to != null) {
                query = new QueryDefault<>(CommandJdo.class,
                        "findByTimestampBetween", 
                        "from", fromTs,
                        "to", toTs);
            } else {
                query = new QueryDefault<>(CommandJdo.class,
                        "findByTimestampAfter", 
                        "from", fromTs);
            }
        } else {
            if(to != null) {
                query = new QueryDefault<>(CommandJdo.class,
                        "findByTimestampBefore", 
                        "to", toTs);
            } else {
                query = new QueryDefault<>(CommandJdo.class,
                        "find");
            }
        }
        return repositoryService.allMatches(query);
    }


    public Optional<CommandJdo> findByUniqueId(final UUID transactionId) {
        persistCurrentCommandIfRequired();
        return repositoryService.firstMatch(
                new QueryDefault<>(CommandJdo.class,
                        "findByUniqueId",
                        "uniqueId", transactionId));
    }

    public List<CommandJdo> findByParent(final CommandJdo parent) {
        return repositoryService.allMatches(
                new QueryDefault<>(CommandJdo.class,
                        "findByParent",
                        "parent", parent));
    }


    public List<CommandJdo> findCurrent() {
        persistCurrentCommandIfRequired();
        return repositoryService.allMatches(
                new QueryDefault<>(CommandJdo.class, "findCurrent"));
    }


    public List<CommandJdo> findCompleted() {
        persistCurrentCommandIfRequired();
        return repositoryService.allMatches(
                new QueryDefault<>(CommandJdo.class, "findCompleted"));
    }


    private void persistCurrentCommandIfRequired() {
        if(commandContextProvider == null) {
            return;
        } 
        final Command command = commandContextProvider.get().getCommand();
        final CommandJdo commandJdo = new CommandJdo(command, this);
        repositoryService.persist(commandJdo);
    }


    public List<CommandJdo> findByTargetAndFromAndTo(
            final Bookmark target
            , final LocalDate from
            , final LocalDate to) {
        final String targetStr = target.toString();
        final Timestamp fromTs = toTimestampStartOfDayWithOffset(from, 0);
        final Timestamp toTs = toTimestampStartOfDayWithOffset(to, 1);
        
        final Query<CommandJdo> query;
        if(from != null) {
            if(to != null) {
                query = new QueryDefault<>(CommandJdo.class,
                        "findByTargetAndTimestampBetween", 
                        "targetStr", targetStr,
                        "from", fromTs,
                        "to", toTs);
            } else {
                query = new QueryDefault<>(CommandJdo.class,
                        "findByTargetAndTimestampAfter", 
                        "targetStr", targetStr,
                        "from", fromTs);
            }
        } else {
            if(to != null) {
                query = new QueryDefault<>(CommandJdo.class,
                        "findByTargetAndTimestampBefore", 
                        "targetStr", targetStr,
                        "to", toTs);
            } else {
                query = new QueryDefault<>(CommandJdo.class,
                        "findByTarget", 
                        "targetStr", targetStr);
            }
        }
        return repositoryService.allMatches(query);
    }

    private static Timestamp toTimestampStartOfDayWithOffset(final LocalDate dt, int daysOffset) {
        return dt!=null
                ?new java.sql.Timestamp(dt.toDateTimeAtStartOfDay().plusDays(daysOffset).getMillis())
                :null;
    }


    public List<CommandJdo> findRecentByUser(final String user) {
        return repositoryService.allMatches(
                new QueryDefault<>(CommandJdo.class, "findRecentByUser", "user", user));
    }


    public List<CommandJdo> findRecentByTarget(final Bookmark target) {
        final String targetStr = target.toString();
        return repositoryService.allMatches(
                new QueryDefault<>(CommandJdo.class, "findRecentByTarget", "targetStr", targetStr));
    }


    /**
     * Intended to support the replay of commands on a slave instance of the application.
     *
     * This finder returns all (completed) {@link CommandJdo}s started after the command with the specified
     * transaction Id.  The number of commands returned can be limited so that they can be applied in batches.
     *
     * If the provided transactionId is null, then only a single {@link CommandJdo command} is returned.  This is
     * intended to support the case when the slave does not yet have any {@link CommandJdo command}s replicated.
     * In practice this is unlikely; typically we expect that the slave will be set up to run against a copy of the
     * master instance's DB (restored from a backup), in which case there will already be a {@link CommandJdo command}
     * representing the current high water mark on the slave.
     *
     * If the transaction id is not null but the corresponding {@link CommandJdo command} is not found, then
     * <tt>null</tt> is returned. In the replay scenario the caller will probably interpret this as an error because
     * it means that the high water mark on the slave is inaccurate, referring to a non-existent
     * {@link CommandJdo command} on the master.
     *
     * @param transactionId - the identifier of the {@link CommandJdo command} being the replay hwm (using {@link #findReplayHwm()} on the slave), or null if no HWM was found there.
     * @param batchSize - to restrict the number returned (so that replay commands can be batched).
     *
     * @return
     */
    public List<CommandJdo> findSince(final UUID transactionId, final Integer batchSize) {
        if(transactionId == null) {
            return findFirst();
        }
        final CommandJdo from = findByTransactionIdElseNull(transactionId);
        if(from == null) {
            return null;
        }
        return findSince(from.getTimestamp(), batchSize);
    }

    private List<CommandJdo> findFirst() {
        Optional<CommandJdo> firstCommandIfAny = repositoryService.firstMatch(
                new QueryDefault<>(CommandJdo.class, "findFirst"));
        return firstCommandIfAny
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
    }


    private CommandJdo findByTransactionIdElseNull(final UUID transactionId) {
        var q = isisJdoSupport.newTypesafeQuery(CommandJdo.class);
        val cand = QCommandJdo.candidate();
        q = q.filter(
                cand.transactionId.eq(q.parameter("transactionId", UUID.class))
        );
        q.setParameter("transactionId", transactionId);
        return q.executeUnique();
    }

    private List<CommandJdo> findSince(final Timestamp timestamp, final Integer batchSize) {
        val q = new QueryDefault<>(
                CommandJdo.class,
                "findSince",
                "timestamp", timestamp);

        // DN generates incorrect SQL for SQL Server if count set to 1; so we set to 2 and then trim
        if(batchSize != null) {
            q.withCount(batchSize == 1 ? 2 : batchSize);
        }
        final List<CommandJdo> commandJdos = repositoryService.allMatches(q);
        return batchSize != null && batchSize == 1 && commandJdos.size() > 1
                    ? commandJdos.subList(0,1)
                    : commandJdos;
    }


    public CommandJdo findReplayHwm() {

        // most recent replayable command, replicated from master to slave
        // this may or may not
        Optional<CommandJdo> replayableHwm = repositoryService.firstMatch(
                new QueryDefault<>(CommandJdo.class, "findReplayableHwm"));

        return replayableHwm
                .orElseGet(() -> {
            // otherwise, the most recent completed command, run on the slave,
            // this corresponds to a command restored from a copy of
            // the production database
            Optional<CommandJdo> restoredFromDbHwm = repositoryService.firstMatch(
                    new QueryDefault<>(CommandJdo.class, "findForegroundHwm"));

            return restoredFromDbHwm.orElse(null);
        });

    }


    public List<CommandJdo> findBackgroundCommandsNotYetStarted() {
        return repositoryService.allMatches(
                new QueryDefault<>(CommandJdo.class,
                        "findBackgroundCommandsNotYetStarted"));
    }



    public List<CommandJdo> findReplayedOnSlave() {
        return repositoryService.allMatches(
                new QueryDefault<>(CommandJdo.class, "findReplayableMostRecentStarted"));
    }


    public List<CommandJdo> saveForReplay(final CommandsDto commandsDto) {
        List<CommandDto> commandDto = commandsDto.getCommandDto();
        List<CommandJdo> commands = new ArrayList<>();
        for (final CommandDto dto : commandDto) {
            commands.add(saveForReplay(dto));
        }
        return commands;
    }

    @Programmatic
    public CommandJdo saveForReplay(final CommandDto dto) {

        final MapDto userData = dto.getUserData();
        if (userData == null ) {
            throw new IllegalStateException(String.format(
                    "Can only persist DTOs with additional userData; got: \n%s",
                    CommandDtoUtils.toXml(dto)));
        }

        final CommandJdo commandJdo = new CommandJdo();

        commandJdo.setUniqueId(UUID.fromString(dto.getTransactionId()));
        commandJdo.setTimestamp(JavaSqlXMLGregorianCalendarMarshalling.toTimestamp(dto.getTimestamp()));
        commandJdo.setUsername(dto.getUser());

        commandJdo.setReplayState(ReplayState.PENDING);

        final OidDto firstTarget = dto.getTargets().getOid().get(0);
        commandJdo.setTarget(Bookmark.from(firstTarget));
        commandJdo.setCommandDto(dto);
        commandJdo.setLogicalMemberIdentifier(dto.getMember().getLogicalMemberIdentifier());

        persist(commandJdo);

        return commandJdo;
    }

    public void persist(final CommandJdo commandJdo) {
        repositoryService.persist(commandJdo);
    }



    @Inject Provider<CommandContext> commandContextProvider;
    @Inject RepositoryService repositoryService;
    @Inject IsisJdoSupport_v3_2 isisJdoSupport;

}
