package org.apache.isis.extensions.commandreplay.impl.mixins;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.CommandPersistence;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.ParameterLayout;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.value.Clob;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;
import org.apache.isis.extensions.commandreplay.impl.IsisModuleExtCommandReplayImpl;

import org.apache.isis.extensions.commandreplay.impl.ui.CommandReplayOnMasterService;

@Action(
        semantics = SemanticsOf.NON_IDEMPOTENT,
        domainEvent = CommandJdo_download.ActionDomainEvent.class,
        commandPersistence = CommandPersistence.NOT_PERSISTED
)
@ActionLayout(
        cssClassFa = "fa-download",
        position = ActionLayout.Position.PANEL
)
public class CommandJdo_download {

    public static class ActionDomainEvent
            extends IsisModuleExtCommandReplayImpl.ActionDomainEvent<CommandJdo_download> { }

    private final CommandJdo commandJdo;
    public CommandJdo_download(CommandJdo commandJdo) {
        this.commandJdo = commandJdo;
    }


    @MemberOrder(name = "arguments", sequence = "1")
    public Clob act(
            @ParameterLayout(named="Filename prefix")
            final String fileNamePrefix) {
        return commandReplayOnMasterService.downloadCommandById(commandJdo.getTransactionId(), fileNamePrefix);
    }
    public String default0Act() {
        return "command";
    }


    @Inject
    CommandReplayOnMasterService commandReplayOnMasterService;
}
