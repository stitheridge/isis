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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.jdo.annotations.IdentityType;

import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.DomainObjectLayout;
import org.apache.isis.applib.annotation.Editing;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.annotation.Where;
import org.apache.isis.applib.jaxb.JavaSqlXMLGregorianCalendarMarshalling;
import org.apache.isis.applib.services.DomainChangeRecord;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.jaxb.JaxbService;
import org.apache.isis.applib.types.MemberIdentifierType;
import org.apache.isis.applib.util.ObjectContracts;
import org.apache.isis.applib.util.TitleBuffer;
import org.apache.isis.core.commons.internal.exceptions._Exceptions;
import org.apache.isis.extensions.commandlog.impl.IsisModuleExtCommandLogImpl;
import org.apache.isis.extensions.commandlog.impl.api.UserDataKeys;
import org.apache.isis.schema.cmd.v2.CommandDto;
import org.apache.isis.schema.cmd.v2.MapDto;

import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.extern.log4j.Log4j2;

/**
 * A persistent representation of a {@link Command}.
 *
 * <p>
 *     Use cases requiring persistence including auditing, and for replay of
 *     commands for regression testing purposes.
 * </p>
 *
 * Note that this class doesn't subclass from {@link Command} ({@link Command}
 * is not an interface).
 */
@javax.jdo.annotations.PersistenceCapable(
        identityType=IdentityType.APPLICATION,
        schema = "isisExtensionsCommandLog",
        table = "Command")
@javax.jdo.annotations.Queries( {
    @javax.jdo.annotations.Query(
            name="findByUniqueId",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE uniqueId == :uniqueId "),
    @javax.jdo.annotations.Query(
            name="findByParent",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE parent == :parent "),
    @javax.jdo.annotations.Query(
            name="findCurrent",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE completedAt == null "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findCompleted",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE completedAt != null "
                    + "&& executeIn == 'FOREGROUND' "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findRecentByTarget",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE targetStr == :targetStr "
                    + "ORDER BY this.timestamp DESC, uniqueId DESC "
                    + "RANGE 0,30"),
    @javax.jdo.annotations.Query(
            name="findByTargetAndTimestampBetween",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE targetStr == :targetStr " 
                    + "&& timestamp >= :from " 
                    + "&& timestamp <= :to "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTargetAndTimestampAfter",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE targetStr == :targetStr " 
                    + "&& timestamp >= :from "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTargetAndTimestampBefore",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE targetStr == :targetStr " 
                    + "&& timestamp <= :to "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTarget",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE targetStr == :targetStr " 
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTimestampBetween",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE timestamp >= :from " 
                    + "&&    timestamp <= :to "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTimestampAfter",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE timestamp >= :from "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTimestampBefore",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE timestamp <= :to "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="find",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findRecentByUser",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE user == :user "
                    + "ORDER BY this.timestamp DESC "
                    + "RANGE 0,30"),
    @javax.jdo.annotations.Query(
            name="findFirst",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE timestamp   != null "
                    + "   && startedAt   != null "
                    + "   && completedAt != null "
                    + "ORDER BY this.timestamp ASC "
                    + "RANGE 0,2"),
        // this should be RANGE 0,1 but results in DataNucleus submitting "FETCH NEXT ROW ONLY"
        // which SQL Server doesn't understand.  However, as workaround, SQL Server *does* understand FETCH NEXT 2 ROWS ONLY
    @javax.jdo.annotations.Query(
            name="findSince",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE timestamp > :timestamp "
                    + "   && startedAt != null "
                    + "   && completedAt != null "
                    + "ORDER BY this.timestamp ASC"),
    @javax.jdo.annotations.Query(
            name="findReplayableHwm",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE executeIn == 'REPLAYABLE' "
                    + "ORDER BY this.timestamp DESC "
                    + "RANGE 0,2"),
        // this should be RANGE 0,1 but results in DataNucleus submitting "FETCH NEXT ROW ONLY"
        // which SQL Server doesn't understand.  However, as workaround, SQL Server *does* understand FETCH NEXT 2 ROWS ONLY
    @javax.jdo.annotations.Query(
            name="findForegroundHwm",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE executeIn == 'FOREGROUND' "
                    + "   && startedAt   != null "
                    + "   && completedAt != null "
                    + "ORDER BY this.timestamp DESC "
                    + "RANGE 0,2"),
        // this should be RANGE 0,1 but results in DataNucleus submitting "FETCH NEXT ROW ONLY"
        // which SQL Server doesn't understand.  However, as workaround, SQL Server *does* understand FETCH NEXT 2 ROWS ONLY
    @javax.jdo.annotations.Query(
            name="findBackgroundCommandsNotYetStarted",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE executeIn == 'BACKGROUND' "
                    + "   && startedAt == null "
                    + "ORDER BY this.timestamp ASC "),
        @javax.jdo.annotations.Query(
                name="findReplayableInErrorMostRecent",
                value="SELECT "
                        + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                        + "WHERE executeIn   == 'REPLAYABLE' "
                        + "  && (replayState != 'PENDING' || "
                        + "      replayState != 'OK'      || "
                        + "      replayState != 'EXCLUDED'   ) "
                        + "ORDER BY this.timestamp DESC "
                        + "RANGE 0,2"),
    @javax.jdo.annotations.Query(
            name="findReplayableMostRecentStarted",
            value="SELECT "
                    + "FROM org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo "
                    + "WHERE executeIn == 'REPLAYABLE' "
                    + "   && startedAt != null "
                    + "ORDER BY this.timestamp DESC "
                    + "RANGE 0,20"),
})
@javax.jdo.annotations.Indices({
        @javax.jdo.annotations.Index(name = "CommandJdo_timestamp_e_s_IDX", members = {"timestamp", "executeIn", "startedAt"}),
        @javax.jdo.annotations.Index(name = "CommandJdo_startedAt_e_c_IDX", members = {"startedAt", "executeIn", "completedAt"}),
})
@DomainObject(
        objectType = "isisExtensionsCommandLog.Command",
        editing = Editing.DISABLED
)
@DomainObjectLayout(named = "Command")
@Log4j2
public class CommandJdo
        implements DomainChangeRecord, Comparable<CommandJdo> {


    public static abstract class PropertyDomainEvent<T> extends IsisModuleExtCommandLogImpl.PropertyDomainEvent<CommandJdo, T> { }
    public static abstract class CollectionDomainEvent<T> extends IsisModuleExtCommandLogImpl.CollectionDomainEvent<CommandJdo, T> { }
    public static abstract class ActionDomainEvent extends IsisModuleExtCommandLogImpl.ActionDomainEvent<CommandJdo> { }

    public CommandJdo() {
        this(UUID.randomUUID());
    }

    private CommandJdo(final UUID uniqueId) {
        super();
        this.uniqueId = uniqueId;
    }

    /**
     * Intended for use on primary system.
     *
     * @param command
     * @param commandJdoRepository
     */
    public CommandJdo(
            final Command command
            , final CommandJdoRepository commandJdoRepository) {
        this();

        setUniqueId(command.getUniqueId());
        setUsername(command.getUsername());
        setTimestamp(command.getTimestamp());

        setCommandDto(command.getCommandDto());
        setTarget(command.getTarget());
        setLogicalMemberIdentifier(command.getLogicalMemberIdentifier());

        val parent = command.getParent();
        if(parent != null) {
            setParent(commandJdoRepository.findByUniqueId(parent.getUniqueId()).orElse(null));
        }

        setStartedAt(command.getStartedAt());
        setCompletedAt(command.getCompletedAt());

        setResult(command.getResult());

        setException(command.getException());

        setReplayState(ReplayState.UNDEFINED);
    }


    /**
     * Intended for use on secondary (replay) system.
     *
     * @param commandDto - obtained from the primary system as a representation of a command invocation
     * @param replayState - controls whether this is to be replayed
     * @param targetIndex - if the command represents a bulk action, then it is flattened out when replayed; this indicates which target to execute against.
     */
    public CommandJdo(final CommandDto commandDto, final ReplayState replayState, final int targetIndex) {
        this();

        setUniqueId(UUID.fromString(commandDto.getTransactionId()));
        setUsername(commandDto.getUser());
        setTimestamp(JavaSqlXMLGregorianCalendarMarshalling.toTimestamp(commandDto.getTimestamp()));

        setCommandDto(commandDto);
        setTarget(Bookmark.from(commandDto.getTargets().getOid().get(targetIndex)));
        setLogicalMemberIdentifier(commandDto.getMember().getLogicalMemberIdentifier());

        // the hierarchy of commands calling other commands is only available on the primary system, and is
        setParent(null);

        setStartedAt(JavaSqlXMLGregorianCalendarMarshalling.toTimestamp(commandDto.getTimings().getStartedAt()));
        setCompletedAt(JavaSqlXMLGregorianCalendarMarshalling.toTimestamp(commandDto.getTimings().getCompletedAt()));

        set(commandDto, UserDataKeys.RESULT, value -> setResult(Bookmark.parse(value).orElse(null)));
        set(commandDto, UserDataKeys.EXCEPTION, this::setException);

        setReplayState(replayState);
    }

    private void set(CommandDto commandDto, String key, Consumer<String> consumer) {
        commandDto.getUserData().getEntry()
                .stream()
                .filter(x -> Objects.equals(x.getKey(), UserDataKeys.RESULT))
                .map(MapDto.Entry::getValue)
                .findFirst()
                .ifPresent(consumer::accept);
    }

    public String title() {
        // nb: not thread-safe
        // formats defined in https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
        val format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        val buf = new TitleBuffer();
        buf.append(format.format(getTimestamp()));
        buf.append(" ").append(getLogicalMemberIdentifier());
        return buf.toString();
    }


    public static class UniqueIdDomainEvent extends PropertyDomainEvent<UUID> { }
    @javax.jdo.annotations.Persistent
    @javax.jdo.annotations.Column(allowsNull="false", length = 36)
    @Property(domainEvent = UniqueIdDomainEvent.class)
    @Getter @Setter
    private UUID uniqueId;


    public static class UsernameDomainEvent extends PropertyDomainEvent<String> { }
    @javax.jdo.annotations.Column(allowsNull="false", length = 50)
    @Property(domainEvent = UsernameDomainEvent.class)
    @Getter @Setter
    private String username;


    public static class TimestampDomainEvent extends PropertyDomainEvent<Timestamp> { }
    @javax.jdo.annotations.Persistent
    @javax.jdo.annotations.Column(allowsNull="false")
    @Property(domainEvent = TimestampDomainEvent.class)
    @Getter @Setter
    private Timestamp timestamp;



    @Override
    public ChangeType getType() {
        return ChangeType.COMMAND;
    }


    public static class ReplayStateDomainEvent extends PropertyDomainEvent<ReplayState> { }
    /**
     * For a replayed command, what the outcome was.
     */
    @javax.jdo.annotations.Column(allowsNull="true", length=10)
    @Property(domainEvent = ReplayStateDomainEvent.class)
    @Getter @Setter
    private ReplayState replayState;


    public static class ReplayStateFailureReasonDomainEvent extends PropertyDomainEvent<ReplayState> { }
    /**
     * For a {@link ReplayState#FAILED failed} replayed command, what the reason was for the failure.
     */
    @javax.jdo.annotations.Column(allowsNull="true", length=255)
    @Property(domainEvent = ReplayStateFailureReasonDomainEvent.class)
    @PropertyLayout(hidden = Where.ALL_TABLES, multiLine = 5)
    @Getter @Setter
    private String replayStateFailureReason;
    public boolean hideReplayStateFailureReason() {
        return getReplayState() == null || !getReplayState().isFailed();
    }


    public static class ParentDomainEvent extends PropertyDomainEvent<Command> { }
    @javax.jdo.annotations.Persistent
    @javax.jdo.annotations.Column(name="parentId", allowsNull="true")
    @Property(domainEvent = ParentDomainEvent.class)
    @PropertyLayout(hidden = Where.ALL_TABLES)
    @Getter @Setter
    private CommandJdo parent;


    public static class TargetDomainEvent extends PropertyDomainEvent<String> { }
    @javax.jdo.annotations.Persistent
    @javax.jdo.annotations.Column(allowsNull="true", length = 2000, name="target")
    @Property(domainEvent = TargetDomainEvent.class)
    @PropertyLayout(hidden = Where.REFERENCES_PARENT, named = "Object")
    @Getter @Setter
    private Bookmark target;

    @Override
    public String getTargetMember() {
        return getCommandDto().getMember().getLogicalMemberIdentifier();
    }

    public static class LogicalMemberIdentifierDomainEvent extends PropertyDomainEvent<String> { }
    @Property(domainEvent = LogicalMemberIdentifierDomainEvent.class)
    @PropertyLayout(hidden = Where.ALL_TABLES)
    @javax.jdo.annotations.Column(allowsNull="false", length = MemberIdentifierType.Meta.MAX_LEN)
    @Getter @Setter
    private String logicalMemberIdentifier;


    public static class CommandDtoDomainEvent extends PropertyDomainEvent<CommandDto> { }
    @javax.jdo.annotations.Persistent
    @javax.jdo.annotations.Column(allowsNull="true", jdbcType="CLOB")
    @Property(domainEvent = CommandDtoDomainEvent.class)
    @PropertyLayout(multiLine = 9)
    @Getter @Setter
    private CommandDto commandDto;


    public static class StartedAtDomainEvent extends PropertyDomainEvent<Timestamp> { }
    @javax.jdo.annotations.Persistent
    @javax.jdo.annotations.Column(allowsNull="true")
    @Property(domainEvent = StartedAtDomainEvent.class)
    @Getter @Setter
    private Timestamp startedAt;


    public static class CompletedAtDomainEvent extends PropertyDomainEvent<Timestamp> { }
    @javax.jdo.annotations.Persistent
    @javax.jdo.annotations.Column(allowsNull="true")
    @Property(domainEvent = CompletedAtDomainEvent.class)
    @Getter @Setter
    private Timestamp completedAt;


    public static class DurationDomainEvent extends PropertyDomainEvent<BigDecimal> { }
    /**
     * The number of seconds (to 3 decimal places) that this interaction lasted.
     * 
     * <p>
     * Populated only if it has {@link #getCompletedAt() completed}.
     */
    @javax.jdo.annotations.NotPersistent
    @javax.validation.constraints.Digits(integer=5, fraction=3)
    @Property(domainEvent = DurationDomainEvent.class)
    public BigDecimal getDuration() {
        return durationBetween(getStartedAt(), getCompletedAt());
    }


    public static class IsCompleteDomainEvent extends PropertyDomainEvent<Boolean> { }
    @javax.jdo.annotations.NotPersistent
    @Property(domainEvent = IsCompleteDomainEvent.class)
    @PropertyLayout(hidden = Where.OBJECT_FORMS)
    public boolean isComplete() {
        return getCompletedAt() != null;
    }


    public static class ResultSummaryDomainEvent extends PropertyDomainEvent<String> { }
    @javax.jdo.annotations.NotPersistent
    @Property(domainEvent = ResultSummaryDomainEvent.class)
    @PropertyLayout(hidden = Where.OBJECT_FORMS, named = "Result")
    public String getResultSummary() {
        if(getCompletedAt() == null) {
            return "";
        }
        if(getException() != null) {
            return "EXCEPTION";
        }
        if(getResult() != null) {
            return "OK";
        } else {
            return "OK (VOID)";
        }
    }


    public static class ResultDomainEvent extends PropertyDomainEvent<String> { }
    @javax.jdo.annotations.Persistent
    @javax.jdo.annotations.Column(allowsNull="true", length = 2000, name="result")
    @Property(domainEvent = ResultDomainEvent.class)
    @PropertyLayout(hidden = Where.ALL_TABLES, named = "Result Bookmark")
    @Getter @Setter
    private Bookmark result;


    public static class ExceptionDomainEvent extends PropertyDomainEvent<String> { }
    /**
     * Stack trace of any exception that might have occurred if this interaction/transaction aborted.
     *
     * <p>
     * Not part of the applib API, because the default implementation is not persistent
     * and so there's no object that can be accessed to be annotated.
     */
    @javax.jdo.annotations.Column(allowsNull="true", jdbcType="CLOB")
    @Property(domainEvent = ExceptionDomainEvent.class)
    @PropertyLayout(hidden = Where.ALL_TABLES, multiLine = 5, named = "Exception (if any)")
    @Getter
    private String exception;
    public void setException(String exception) {
        this.exception = exception;
    }
    public void setException(final Throwable exception) {
        val stackTraceStr =
                _Exceptions.streamStacktraceLines(exception, 1000)
                .collect(Collectors.joining("\n"));
        setException(stackTraceStr);
    }

    public static class IsCausedExceptionDomainEvent extends PropertyDomainEvent<Boolean> { }
    @javax.jdo.annotations.NotPersistent
    @Property(domainEvent = IsCausedExceptionDomainEvent.class)
    @PropertyLayout(hidden = Where.OBJECT_FORMS)
    public boolean isCausedException() {
        return getException() != null;
    }


    @Override
    public String toString() {
        return ObjectContracts
                .toString("uniqueId", CommandJdo::getUniqueId)
                .thenToString("username", CommandJdo::getUsername)
                .thenToString("timestamp", CommandJdo::getTimestamp)
                .thenToString("target", CommandJdo::getTarget)
                .thenToString("logicalMemberIdentifier", CommandJdo::getLogicalMemberIdentifier)
                .thenToStringOmitIfAbsent("startedAt", CommandJdo::getStartedAt)
                .thenToStringOmitIfAbsent("completedAt", CommandJdo::getCompletedAt)
                .toString(this);
    }

    @Override
    public int compareTo(final CommandJdo other) {
        return this.getTimestamp().compareTo(other.getTimestamp());
    }


    /**
     * @return in seconds, to 3 decimal places.
     */
    private static BigDecimal durationBetween(Timestamp startedAt, Timestamp completedAt) {
        if (completedAt == null) {
            return null;
        } else {
            long millis = completedAt.getTime() - startedAt.getTime();
            return toSeconds(millis);
        }
    }

    private static final BigDecimal DIVISOR = new BigDecimal(1000);

    private static BigDecimal toSeconds(long millis) {
        return new BigDecimal(millis)
                    .divide(DIVISOR, RoundingMode.HALF_EVEN)
                    .setScale(3, RoundingMode.HALF_EVEN);
    }


    @Override
    public String getPreValue() {
        return null;
    }

    @Override
    public String getPostValue() {
        return null;
    }

    @Inject JaxbService jaxbService;

}
