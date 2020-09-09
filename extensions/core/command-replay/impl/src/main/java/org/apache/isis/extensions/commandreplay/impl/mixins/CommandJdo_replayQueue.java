package org.apache.isis.extensions.commandreplay.impl.mixins;

import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Collection;
import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.Mixin;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdoRepository;
import org.apache.isis.extensions.commandreplay.impl.IsisModuleExtCommandReplayImpl;
import org.apache.isis.extensions.commandreplay.impl.fetch.MasterConfiguration;

@Collection(
        domainEvent = CommandJdo_replayQueue.CollectionDomainEvent.class
)
@CollectionLayout(
        defaultView = "table"
)
@Mixin(method = "coll")
public class CommandJdo_replayQueue {

    public static class CollectionDomainEvent
            extends IsisModuleExtCommandReplayImpl.CollectionDomainEvent<CommandJdo_replayQueue, CommandJdo> { }

    private final CommandJdo commandJdo;
    public CommandJdo_replayQueue(final CommandJdo commandJdo) {
        this.commandJdo = commandJdo;
    }

    @MemberOrder(sequence = "100.100")
    public List<CommandJdo> coll() {
        return commandJdoRepository.findReplayedOnSecondary();
    }

    public boolean hideColl() {
        return !masterConfiguration.isConfigured();
    }

    @Inject
    MasterConfiguration masterConfiguration;
    @Inject
    CommandJdoRepository commandJdoRepository;

}
