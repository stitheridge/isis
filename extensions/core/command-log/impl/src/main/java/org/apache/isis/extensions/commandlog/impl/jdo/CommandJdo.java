package org.apache.isis.extensions.commandlog.impl.jdo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jdo.annotations.IdentityType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.isis.applib.annotation.CommandExecuteIn;
import org.apache.isis.applib.annotation.CommandPersistence;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.DomainObjectLayout;
import org.apache.isis.applib.annotation.Editing;
import org.apache.isis.applib.annotation.Optionality;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.annotation.Where;
import org.apache.isis.applib.services.DomainChangeAbstract;
import org.apache.isis.applib.services.HasUsername;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.command.CommandWithDto;
import org.apache.isis.applib.services.jaxb.JaxbService;
import org.apache.isis.applib.types.MemberIdentifierType;
import org.apache.isis.applib.types.TargetActionType;
import org.apache.isis.applib.types.TargetClassType;
import org.apache.isis.applib.util.TitleBuffer;
import org.apache.isis.extensions.commandlog.impl.IsisModuleExtCommandLogImpl;
import org.apache.isis.extensions.commandlog.impl.jdo.ReplayState;
import org.apache.isis.schema.cmd.v2.CommandDto;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@javax.jdo.annotations.PersistenceCapable(
        identityType=IdentityType.APPLICATION,
        schema = "isiscoreextcommandlog",
        table = "Command")
@javax.jdo.annotations.Queries( {
    @javax.jdo.annotations.Query(
            name="findByTransactionId",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE transactionId == :transactionId "),
    @javax.jdo.annotations.Query(
            name="findBackgroundCommandsByParent",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE parent == :parent "
                    + "&& executeIn == 'BACKGROUND'"),
    @javax.jdo.annotations.Query(
            name="findCurrent",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE completedAt == null "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findCompleted",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE completedAt != null "
                    + "&& executeIn == 'FOREGROUND' "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findRecentBackgroundByTarget",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE targetStr == :targetStr "
                    + "&& executeIn == 'BACKGROUND' "
                    + "ORDER BY this.timestamp DESC, transactionId DESC "
                    + "RANGE 0,30"),
    @javax.jdo.annotations.Query(
            name="findByTargetAndTimestampBetween",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE targetStr == :targetStr " 
                    + "&& timestamp >= :from " 
                    + "&& timestamp <= :to "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTargetAndTimestampAfter",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE targetStr == :targetStr " 
                    + "&& timestamp >= :from "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTargetAndTimestampBefore",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE targetStr == :targetStr " 
                    + "&& timestamp <= :to "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTarget",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE targetStr == :targetStr " 
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTimestampBetween",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE timestamp >= :from " 
                    + "&&    timestamp <= :to "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTimestampAfter",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE timestamp >= :from "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findByTimestampBefore",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE timestamp <= :to "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="find",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "ORDER BY this.timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findRecentByUser",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE user == :user "
                    + "ORDER BY this.timestamp DESC "
                    + "RANGE 0,30"),
    @javax.jdo.annotations.Query(
            name="findRecentByTarget",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE targetStr == :targetStr "
                    + "ORDER BY this.timestamp DESC, transactionId DESC "
                    + "RANGE 0,30"),
    @javax.jdo.annotations.Query(
            name="findForegroundFirst",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE executeIn == 'FOREGROUND' "
                    + "   && timestamp   != null "
                    + "   && startedAt   != null "
                    + "   && completedAt != null "
                    + "ORDER BY this.timestamp ASC "
                    + "RANGE 0,2"),
        // this should be RANGE 0,1 but results in DataNucleus submitting "FETCH NEXT ROW ONLY"
        // which SQL Server doesn't understand.  However, as workaround, SQL Server *does* understand FETCH NEXT 2 ROWS ONLY
    @javax.jdo.annotations.Query(
            name="findForegroundSince",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE executeIn == 'FOREGROUND' "
                    + "   && timestamp > :timestamp "
                    + "   && startedAt != null "
                    + "   && completedAt != null "
                    + "ORDER BY this.timestamp ASC"),
    @javax.jdo.annotations.Query(
            name="findReplayableHwm",
            value="SELECT "
                    + "FROM CommandJdo "
                    + "WHERE executeIn == 'REPLAYABLE' "
                    + "ORDER BY this.timestamp DESC "
                    + "RANGE 0,2"),
        // this should be RANGE 0,1 but results in DataNucleus submitting "FETCH NEXT ROW ONLY"
        // which SQL Server doesn't understand.  However, as workaround, SQL Server *does* understand FETCH NEXT 2 ROWS ONLY
    @javax.jdo.annotations.Query(
            name="findForegroundHwm",
            value="SELECT "
                    + "FROM CommandJdo "
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
                    + "FROM CommandJdo "
                    + "WHERE executeIn == 'BACKGROUND' "
                    + "   && startedAt == null "
                    + "ORDER BY this.timestamp ASC "),
        @javax.jdo.annotations.Query(
                name="findReplayableInErrorMostRecent",
                value="SELECT "
                        + "FROM CommandJdo "
                        + "WHERE executeIn   == 'REPLAYABLE' "
                        + "  && (replayState != 'PENDING' || "
                        + "      replayState != 'OK'      || "
                        + "      replayState != 'EXCLUDED'   ) "
                        + "ORDER BY this.timestamp DESC "
                        + "RANGE 0,2"),
    @javax.jdo.annotations.Query(
            name="findReplayableMostRecentStarted",
            value="SELECT "
                    + "FROM CommandJdo "
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
        objectType = "isisextcommandlog.Command",
        editing = Editing.DISABLED
)
@DomainObjectLayout(named = "Command")
@Log4j2
public class CommandJdo extends DomainChangeAbstract
        implements Command, CommandWithDto, HasUsername, Comparable<CommandJdo> {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(CommandJdo.class);


    public static abstract class PropertyDomainEvent<T> extends IsisModuleExtCommandLogImpl.PropertyDomainEvent<CommandJdo, T> { }
    public static abstract class CollectionDomainEvent<T> extends IsisModuleExtCommandLogImpl.CollectionDomainEvent<CommandJdo, T> { }
    public static abstract class ActionDomainEvent extends IsisModuleExtCommandLogImpl.ActionDomainEvent<CommandJdo> { }

    public CommandJdo() {
        super(DomainChangeAbstract.ChangeType.COMMAND);
        this.uniqueId = UUID.randomUUID();
    }


    public String title() {
        // nb: not thread-safe
        // formats defined in https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        final TitleBuffer buf = new TitleBuffer();
        buf.append(format.format(getTimestamp()));
        buf.append(" ").append(getMemberIdentifier());
        return buf.toString();
    }


    public static class UniqueIdDomainEvent extends PropertyDomainEvent<UUID> { }
    @javax.jdo.annotations.Persistent
    @javax.jdo.annotations.Column(allowsNull="false")
    private UUID uniqueId;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = UniqueIdDomainEvent.class)
    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }


    public static class UserDomainEvent extends PropertyDomainEvent<String> { }
    @javax.jdo.annotations.Column(allowsNull="false", length = 50)
    private String username;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = UserDomainEvent.class)
    @Override
    public String getUsername() {
        return username;
    }


    public static class TimestampDomainEvent extends PropertyDomainEvent<Timestamp> { }
    @javax.jdo.annotations.Persistent
    @javax.jdo.annotations.Column(allowsNull="false")
    private Timestamp timestamp;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = TimestampDomainEvent.class)
    @Override
    public Timestamp getTimestamp() {
        return timestamp;
    }


    public static class ExecutorDomainEvent extends PropertyDomainEvent<Executor> { }
    @javax.jdo.annotations.NotPersistent
    private Executor executor;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = ExecutorDomainEvent.class)
    @Override
    public Executor getExecutor() {
        return executor;
    }


    public static class ExecuteInDomainEvent extends PropertyDomainEvent<CommandExecuteIn> { }
    @javax.jdo.annotations.Column(allowsNull="false", length = CommandExecuteIn.Type.Meta.MAX_LEN)
    private CommandExecuteIn executeIn;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = ExecuteInDomainEvent.class)
    @Override
    public CommandExecuteIn getExecuteIn() {
        return executeIn;
    }


    public static class ReplayStateDomainEvent extends PropertyDomainEvent<ReplayState> { }
    /**
     * For a replayed command, what the outcome was.
     *
     * NOT API.
     */
    @javax.jdo.annotations.Column(allowsNull="true", length=10)
    @Property(domainEvent = ReplayStateDomainEvent.class)
    @Getter @Setter
    private ReplayState replayState;


    public static class ReplayStateFailureReasonDomainEvent extends PropertyDomainEvent<ReplayState> { }
    /**
     * For a {@link ReplayState#FAILED failed} replayed command, what the reason was for the failure.
     *
     * <b>NOT API</b>.
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
    @javax.jdo.annotations.Column(name="parentTransactionId", allowsNull="true")
    private Command parent;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = ParentDomainEvent.class)
    @PropertyLayout(hidden = Where.ALL_TABLES)
    @Override
    public Command getParent() {
        return parent;
    }


    public static class TransactionIdDomainEvent extends PropertyDomainEvent<UUID> { }
    @javax.jdo.annotations.PrimaryKey
    @javax.jdo.annotations.Column(allowsNull="false", length = 36)
    @Setter
    private UUID transactionId;
    /**
     * {@inheritDoc}
     *
     * <p>
     * Implementation notes: copied over from the Isis transaction when the command is persisted.
     */
    @Property(domainEvent = TransactionIdDomainEvent.class)
    @Override
    public UUID getTransactionId() {
        return transactionId;
    }


    public static class TargetClassDomainEvent extends PropertyDomainEvent<String> { }
    @javax.jdo.annotations.Column(allowsNull="false", length = TargetClassType.Meta.MAX_LEN)
    private String targetClass;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = TargetClassDomainEvent.class)
    @PropertyLayout(named="Class")
    @Override
    public String getTargetClass() {
        return targetClass;
    }
    public void setTargetClass(final String targetClass) {
        this.targetClass = abbreviated(targetClass, TargetClassType.Meta.MAX_LEN);
    }


    public static class TargetActionDomainEvent extends PropertyDomainEvent<String> { }
    @javax.jdo.annotations.Column(allowsNull="false", length = TargetActionType.Meta.MAX_LEN)
    private String targetAction;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = TargetActionDomainEvent.class, optionality = Optionality.MANDATORY)
    @PropertyLayout(hidden = Where.NOWHERE, named = "Action")
    @Override
    public String getTargetAction() {
        return targetAction;
    }
    public void setTargetAction(final String targetAction) {
        this.targetAction = abbreviated(targetAction, TargetActionType.Meta.MAX_LEN);
    }


    public static class TargetStrDomainEvent extends PropertyDomainEvent<String> { }
    @javax.jdo.annotations.Column(allowsNull="true", length = 2000, name="target")
    private String targetStr;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = TargetStrDomainEvent.class)
    @PropertyLayout(hidden = Where.REFERENCES_PARENT, named = "Object")
    @Override
    public String getTargetStr() {
        return targetStr;
    }
    @Override
    public void setTargetStr(String targetStr) {
        this.targetStr = targetStr;
    }

    public static class ArgumentsDomainEvent extends PropertyDomainEvent<String> { }
    @javax.jdo.annotations.Column(allowsNull="true", jdbcType="CLOB", sqlType="LONGVARCHAR")
    private String arguments;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = ArgumentsDomainEvent.class)
    @PropertyLayout(multiLine = 7, hidden = Where.ALL_TABLES)
    @Override
    public String getArguments() {
        return arguments;
    }


    public static class MemberIdentifierDomainEvent extends PropertyDomainEvent<String> { }
    @javax.jdo.annotations.Column(allowsNull="false", length = MemberIdentifierType.Meta.MAX_LEN)
    private String memberIdentifier;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = MemberIdentifierDomainEvent.class)
    @PropertyLayout(hidden = Where.ALL_TABLES)
    @Override
    public String getMemberIdentifier() {
        return memberIdentifier;
    }
    public void setMemberIdentifier(final String memberIdentifier) {
        this.memberIdentifier = abbreviated(memberIdentifier, MemberIdentifierType.Meta.MAX_LEN);
    }


    public static class MementoDomainEvent extends PropertyDomainEvent<String> { }
    @javax.jdo.annotations.Column(allowsNull="true", jdbcType="CLOB")
    private String memento;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = MementoDomainEvent.class)
    @PropertyLayout(multiLine = 9, hidden = Where.ALL_TABLES)
    @Override
    public String getMemento() {
        return memento;
    }


    // locally cached
    private transient CommandDto commandDto;

    @Override
    public CommandDto asDto() {
        if(commandDto == null) {
            this.commandDto = buildCommandDto();
        }
        return this.commandDto;
    }

    private CommandDto buildCommandDto() {
        if(getMemento() == null) {
            return null;
        }

        return jaxbService.fromXml(CommandDto.class, getMemento());
    }


    public static class StartedAtDomainEvent extends PropertyDomainEvent<Timestamp> { }
    @javax.jdo.annotations.Persistent
    @javax.jdo.annotations.Column(allowsNull="true")
    private Timestamp startedAt;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = StartedAtDomainEvent.class)
    @Override
    public Timestamp getStartedAt() {
        return startedAt;
    }


    public static class CompletedAtDomainEvent extends PropertyDomainEvent<Timestamp> { }
    @javax.jdo.annotations.Persistent
    @javax.jdo.annotations.Column(allowsNull="true")
    private Timestamp completedAt;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = CompletedAtDomainEvent.class)
    @Override
    public Timestamp getCompletedAt() {
        return completedAt;
    }


    public static class DurationDomainEvent extends PropertyDomainEvent<BigDecimal> { }
    /**
     * The number of seconds (to 3 decimal places) that this interaction lasted.
     * 
     * <p>
     * Populated only if it has {@link #getCompletedAt() completed}.
     */
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
        if(getResultStr() != null) {
            return "OK";
        } else {
            return "OK (VOID)";
        }
    }


    @Programmatic
    @Override
    public Bookmark getResult() {
        return bookmarkFor(getResultStr());
    }
    @Programmatic
    public void setResult(final Bookmark result) {
        setResultStr(asString(result));
    }


    public static class ResultStrDomainEvent extends PropertyDomainEvent<String> { }
    @javax.jdo.annotations.Column(allowsNull="true", length = 2000, name="result")
    @Property(domainEvent = ResultStrDomainEvent.class)
    @PropertyLayout(hidden = Where.ALL_TABLES, named = "Result Bookmark")
    @Getter @Setter
    private String resultStr;


    public static class ExceptionDomainEvent extends PropertyDomainEvent<String> { }
    /**
     * Stack trace of any exception that might have occurred if this interaction/transaction aborted.
     * 
     * <p>
     * Not part of the applib API, because the default implementation is not persistent
     * and so there's no object that can be accessed to be annotated.
     */
    @javax.jdo.annotations.Column(allowsNull="true", jdbcType="CLOB")
    private String exception;
    /**
     * {@inheritDoc}
     */
    @Property(domainEvent = ExceptionDomainEvent.class)
    @PropertyLayout(hidden = Where.ALL_TABLES, multiLine = 5, named = "Exception (if any)")
    @Override
    public String getException() {
        return exception;
    }


    public static class IsCausedExceptionDomainEvent extends PropertyDomainEvent<Boolean> { }
    @javax.jdo.annotations.NotPersistent
    @Property(domainEvent = IsCausedExceptionDomainEvent.class)
    @PropertyLayout(hidden = Where.OBJECT_FORMS)
    public boolean isCausedException() {
        return getException() != null;
    }



    private final LinkedList<org.apache.isis.applib.events.domain.ActionDomainEvent<?>> actionDomainEvents = new LinkedList<>();
    @Programmatic
    public org.apache.isis.applib.events.domain.ActionDomainEvent<?> peekActionDomainEvent() {
        return actionDomainEvents.isEmpty()? null: actionDomainEvents.getLast();
    }
    @Programmatic
    public void pushActionDomainEvent(final org.apache.isis.applib.events.domain.ActionDomainEvent<?> event) {
        if(peekActionDomainEvent() == event) {
            return;
        }
        this.actionDomainEvents.add(event);
    }
    @Programmatic
    public org.apache.isis.applib.events.domain.ActionDomainEvent<?> popActionDomainEvent() {
        return !actionDomainEvents.isEmpty()
                ? actionDomainEvents.removeLast() : null;
    }
    @Programmatic
    public List<org.apache.isis.applib.events.domain.ActionDomainEvent<?>> flushActionDomainEvents() {
        final List<org.apache.isis.applib.events.domain.ActionDomainEvent<?>> events =
                Collections.unmodifiableList(new ArrayList<>(actionDomainEvents));
        actionDomainEvents.clear();
        return events;
    }


    private final Map<String, AtomicInteger> sequenceByName = new HashMap<>();
    @Programmatic
    public int next(final String sequenceAbbr) {
        AtomicInteger next = sequenceByName.get(sequenceAbbr);
        if(next == null) {
            next = new AtomicInteger(0);
            sequenceByName.put(sequenceAbbr, next);
        } else {
            next.incrementAndGet();
        }
        return next.get();
    }



    @javax.jdo.annotations.NotPersistent
    @Programmatic
    private CommandPersistence persistence;
    /**
     * {@inheritDoc}
     */
    @Override
    public CommandPersistence getPersistence() {
        return persistence;
    }


    @javax.jdo.annotations.NotPersistent
    @Programmatic
    private boolean persistHint;
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPersistHint() {
        return persistHint;
    }


    boolean shouldPersist() {
        switch (getPersistence()) {
            case PERSISTED:
                return true;
            case IF_HINTED:
                return isPersistHint();
            default:
                return false;
        }
    }


    private final Command.Internal INTERNAL = new Command.Internal() {
        @Override
        public void setMemberIdentifier(String actionIdentifier) {
            CommandJdo.this.memberIdentifier = actionIdentifier;
        }
        @Override
        public void setTargetClass(String targetClass) {
            CommandJdo.this.targetClass = targetClass;
        }
        @Override
        public void setTargetAction(String targetAction) {
            CommandJdo.this.targetAction = targetAction;
        }
        @Override
        public void setArguments(String arguments) {
            CommandJdo.this.arguments = arguments;
        }
        @Override
        public void setMemento(String memento) {
            CommandJdo.this.memento = memento;
        }
        @Override
        public void setTarget(Bookmark target) {
            CommandJdo.this.setTarget(target);
        }
        @Override
        public void setTimestamp(Timestamp timestamp) {
            CommandJdo.this.timestamp = timestamp;
        }
        @Override
        public void setStartedAt(Timestamp startedAt) {
            CommandJdo.this.startedAt = startedAt;
        }
        @Override
        public void setCompletedAt(final Timestamp completed) {
            CommandJdo.this.completedAt = completed;
        }
        @Override
        public void setUser(String user) {
            CommandJdo.this.username = user;
        }
        @Override
        public void setParent(Command parent) {
            CommandJdo.this.parent = parent;
        }
        @Override
        public void setResult(final Bookmark result) {
            CommandJdo.this.setResult(result);
        }
        @Override
        public void setException(final String exceptionStackTrace) {
            CommandJdo.this.exception = exceptionStackTrace;
        }
        @Override
        public void setPersistence(CommandPersistence persistence) {
            CommandJdo.this.persistence = persistence;
        }
        @Override
        public void setPersistHint(boolean persistHint) {
            CommandJdo.this.persistHint = persistHint;
        }
        @Override
        public void setExecutor(Executor executor) {
            CommandJdo.this.executor = executor;
        }
        @Override
        public void setExecuteIn(CommandExecuteIn executeIn) {
            CommandJdo.this.executeIn = executeIn;
        }
    };

    @Override
    public Command.Internal internal() {
        return INTERNAL;
    }


    @Override
    public String toString() {
        return "CommandJdo{" +
                "targetStr='" + targetStr + '\'' +
                ", memberIdentifier='" + memberIdentifier + '\'' +
                ", username='" + username + '\'' +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                ", transactionId=" + transactionId +
                '}';
    }

    @Override
    public int compareTo(final CommandJdo other) {
        return this.getTimestamp().compareTo(other.getTimestamp());
    }


    private static String abbreviated(String str, int maxLength) {
        return str != null
                ? (str.length() < maxLength ? str : str.substring(0, maxLength - 3) + "...")
                : null;
    }

    private static Bookmark bookmarkFor(String str) {
        return Bookmark.parse(str).orElse(null);
    }

    private static String asString(Bookmark bookmark) {
        return bookmark != null ? bookmark.toString() : null;
    }

    private static BigDecimal durationBetween(Timestamp startedAt, Timestamp completedAt) {
        if (completedAt == null) {
            return null;
        } else {
            long millis = completedAt.getTime() - startedAt.getTime();
            return (new BigDecimal(millis)).divide(new BigDecimal(1000)).setScale(3, RoundingMode.HALF_EVEN);
        }
    }


    @javax.inject.Inject
    JaxbService jaxbService;

}
