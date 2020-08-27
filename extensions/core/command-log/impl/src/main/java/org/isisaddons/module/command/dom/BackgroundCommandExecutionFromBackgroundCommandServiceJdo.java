package org.isisaddons.module.command.dom;

import java.util.List;

import org.springframework.stereotype.Service;

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

    @javax.inject.Inject
    BackgroundCommandServiceJdoRepository backgroundCommandRepository;
}