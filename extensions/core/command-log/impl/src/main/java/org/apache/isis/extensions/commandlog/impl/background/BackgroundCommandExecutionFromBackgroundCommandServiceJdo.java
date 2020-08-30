package org.apache.isis.extensions.commandlog.impl.background;

import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.command.CommandExecutorService;
import org.apache.isis.core.runtimeservices.background.BackgroundCommandExecution;

@DomainService
public class BackgroundCommandExecutionFromBackgroundCommandServiceJdo
        extends BackgroundCommandExecution {

    public BackgroundCommandExecutionFromBackgroundCommandServiceJdo() {
        super(CommandExecutorService.SudoPolicy.NO_SWITCH);
    }

    @Override
    protected List<? extends Command> findBackgroundCommandsToExecute() {
        return backgroundCommandRepository.findBackgroundCommandsNotYetStarted();
    }

    @Inject
    BackgroundCommandServiceJdoRepository backgroundCommandRepository;
}