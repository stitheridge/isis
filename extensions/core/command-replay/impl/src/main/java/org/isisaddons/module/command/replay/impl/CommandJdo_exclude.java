package org.isisaddons.module.command.replay.impl;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.CommandPersistence;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.Mixin;
import org.apache.isis.applib.annotation.SemanticsOf;

import org.isisaddons.module.command.IsisModuleExtCommandLogImpl;
import org.isisaddons.module.command.dom.CommandJdo;
import org.isisaddons.module.command.dom.ReplayState;

import lombok.extern.log4j.Log4j2;


@Action(
        semantics = SemanticsOf.NON_IDEMPOTENT_ARE_YOU_SURE,
        domainEvent = CommandJdo_exclude.ActionDomainEvent.class,
        commandPersistence = CommandPersistence.NOT_PERSISTED
)
@Log4j2
public class CommandJdo_exclude {

    public static class ActionDomainEvent
            extends IsisModuleExtCommandLogImpl.ActionDomainEvent<CommandJdo_exclude> { }

    private final CommandJdo commandJdo;
    public CommandJdo_exclude(CommandJdo commandJdo) {
        this.commandJdo = commandJdo;
    }

    @MemberOrder(name = "executeIn", sequence = "2")
    public CommandJdo act() {
        commandJdo.setReplayState(ReplayState.EXCLUDED);
        return commandJdo;
    }

    public boolean hideAct() {
        return commandJdo.getReplayState() == null;
    }
    public String disableAct() {
        final boolean notInError =
                commandJdo.getReplayState() == null || !commandJdo.getReplayState().isFailed();
        return notInError
                ? "This command is not in error, so cannot be excluded."
                : null;
    }

}
