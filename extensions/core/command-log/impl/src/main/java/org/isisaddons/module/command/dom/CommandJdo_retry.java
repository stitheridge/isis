package org.isisaddons.module.command.dom;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.CommandExecuteIn;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.command.CommandContext;
import org.apache.isis.applib.services.jaxb.JaxbService;
import org.apache.isis.schema.cmd.v2.CommandDto;

import org.isisaddons.module.command.IsisModuleExtCommandLogImpl;

@Action(
    semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE
    , domainEvent = CommandJdo_retry.ActionDomainEvent.class
)
public class CommandJdo_retry {

    public static enum Mode {
        SCHEDULE_NEW,
        REUSE
    }

    private final CommandJdo commandJdo;
    public CommandJdo_retry(CommandJdo commandJdo) {
        this.commandJdo = commandJdo;
    }


    public static class ActionDomainEvent extends IsisModuleExtCommandLogImpl.ActionDomainEvent<CommandJdo_retry> { }
    @MemberOrder(name = "executeIn", sequence = "1")
    public CommandJdo act(final Mode mode) {

        switch (mode) {
        case SCHEDULE_NEW:
            final String memento = commandJdo.getMemento();
            final CommandDto dto = jaxbService.fromXml(CommandDto.class, memento);
            backgroundCommandServiceJdo.schedule(
                    dto, commandContext.getCommand(), commandJdo.getTargetClass(), commandJdo.getTargetAction(), commandJdo.getArguments());
            break;
        case REUSE:
            // will cause it to be picked up next time around
            commandJdo.internal().setStartedAt(null);
            commandJdo.internal().setException(null);
            commandJdo.internal().setCompletedAt(null);
            commandJdo.setResult(null);
            commandJdo.setReplayState(null);
            break;
        default:
            // shouldn't occur
            throw new IllegalStateException(String.format("Probable framework error, unknown mode: %s", mode));
        }
        return commandJdo;
    }

    public List<Mode> choices0Act() {
        CommandExecuteIn executeIn = commandJdo.getExecuteIn();
        switch (executeIn){
            case FOREGROUND:
            case BACKGROUND:
                return Arrays.asList(Mode.SCHEDULE_NEW, Mode.REUSE);
            case REPLAYABLE:
                return Collections.singletonList(Mode.REUSE);
            default:
                // shouldn't occur
                throw new IllegalStateException(String.format("Probable framework error, unknown executeIn: %s", executeIn));
        }
    }

    public Mode default0Act() {
        return choices0Act().get(0);
    }
    public String disableAct() {
        if (!commandJdo.isComplete()) {
            return "Not yet completed";
        }
        return null;
    }


    @Inject
    CommandContext commandContext;
    @Inject
    BackgroundCommandServiceJdo backgroundCommandServiceJdo;
    @Inject
    JaxbService jaxbService;

}
