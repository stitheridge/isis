package org.apache.isis.extensions.commandlog.impl.jdo;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.isis.applib.annotation.CommandExecuteIn;
import org.apache.isis.applib.annotation.CommandPersistence;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.command.Command.Executor;
import org.apache.isis.applib.services.command.spi.CommandService;
import org.apache.isis.applib.services.factory.FactoryService;
import org.apache.isis.applib.services.repository.RepositoryService;

@DomainService()
public class CommandServiceJdo implements CommandService {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(CommandServiceJdo.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public Command create() {
        CommandJdo command = factoryService.instantiate(CommandJdo.class);
        command.internal().setExecutor(Executor.OTHER);
        command.internal().setPersistence(CommandPersistence.IF_HINTED);
        return command;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void complete(final Command command) {
        final CommandJdo commandJdo = asUserInitiatedCommandJdo(command);
        if(commandJdo == null) {
            return;
        }
        commandServiceJdoRepository.persistIfHinted(commandJdo);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean persistIfPossible(Command command) {
        if(!(command instanceof CommandJdo)) {
            // ought not to be the case, since this service created the object in the #create() method
            return false;
        }
        final CommandJdo commandJdo = (CommandJdo)command;
        repositoryService.persist(commandJdo);
        return true;
    }


    /**
     * Not API, also used by {@link CommandServiceJdoRepository}.
     */
    CommandJdo asUserInitiatedCommandJdo(final Command command) {
        if(!(command instanceof CommandJdo)) {
            // ought not to be the case, since this service created the object in the #create() method
            return null;
        }
        if(command.getExecuteIn() != CommandExecuteIn.FOREGROUND) {
            return null;
        } 
        final CommandJdo commandJdo = (CommandJdo) command;
        return commandJdo.shouldPersist()? commandJdo: null;
    }



    @Inject
    RepositoryService repositoryService;

    @Inject
    CommandServiceJdoRepository commandServiceJdoRepository;

    @Inject
    FactoryService factoryService;

}
