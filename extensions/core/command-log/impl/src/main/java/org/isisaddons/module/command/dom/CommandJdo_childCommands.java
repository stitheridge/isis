package org.isisaddons.module.command.dom;

import java.util.List;

import org.apache.isis.applib.annotation.Collection;
import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.applib.annotation.MemberOrder;

import org.isisaddons.module.command.IsisModuleExtCommandLogImpl;


@Collection(domainEvent = CommandJdo_childCommands.CollectionDomainEvent.class)
@CollectionLayout(defaultView = "table")
public class CommandJdo_childCommands {

    public static class CollectionDomainEvent
            extends IsisModuleExtCommandLogImpl.CollectionDomainEvent<CommandJdo_childCommands, CommandJdo> { }

    private final CommandJdo commandJdo;
    public CommandJdo_childCommands(final CommandJdo commandJdo) {
        this.commandJdo = commandJdo;
    }

    @MemberOrder(sequence = "100.100")
    public List<CommandJdo> coll() {
        return backgroundCommandRepository.findByParent(commandJdo);
    }

    @javax.inject.Inject
    private BackgroundCommandServiceJdoRepository backgroundCommandRepository;
    
}
