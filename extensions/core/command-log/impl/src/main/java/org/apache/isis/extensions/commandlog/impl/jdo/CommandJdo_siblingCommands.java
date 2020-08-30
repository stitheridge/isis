package org.apache.isis.extensions.commandlog.impl.jdo;

import java.util.Collections;
import java.util.List;

import org.apache.isis.applib.annotation.Collection;
import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.extensions.commandlog.impl.IsisModuleExtCommandLogImpl;
import org.apache.isis.extensions.commandlog.impl.background.BackgroundCommandServiceJdoRepository;

@Collection(domainEvent = CommandJdo_siblingCommands.CollectionDomainEvent.class)
@CollectionLayout(defaultView = "table")
public class CommandJdo_siblingCommands {

    public static class CollectionDomainEvent
            extends IsisModuleExtCommandLogImpl.CollectionDomainEvent<CommandJdo_siblingCommands, CommandJdo> { }

    private final CommandJdo commandJdo;
    public CommandJdo_siblingCommands(final CommandJdo commandJdo) {
        this.commandJdo = commandJdo;
    }

    @MemberOrder(sequence = "100.110")
    public List<CommandJdo> coll() {
        final Command parent = commandJdo.getParent();
        if(!(parent instanceof CommandJdo)) {
            return Collections.emptyList();
        }
        final CommandJdo parentJdo = (CommandJdo) parent;
        final List<CommandJdo> siblingCommands = backgroundCommandRepository.findByParent(parentJdo);
        siblingCommands.remove(commandJdo);
        return siblingCommands;
    }


    @javax.inject.Inject
    private BackgroundCommandServiceJdoRepository backgroundCommandRepository;
    
}
