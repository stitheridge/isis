package org.apache.isis.extensions.commandreplay.impl.executor;

import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.services.command.CommandExecutorService;
import org.apache.isis.applib.services.xactn.TransactionService;
import org.apache.isis.core.runtimeservices.background.CommandExecutionAbstract;
import org.apache.isis.extensions.commandlog.impl.jdo.ReplayState;
import org.apache.isis.extensions.commandreplay.impl.SlaveStatus;
import org.apache.isis.extensions.commandreplay.impl.StatusException;
import org.apache.isis.extensions.commandreplay.impl.analysis.CommandReplayAnalysisService;
import org.apache.isis.extensions.commandreplay.impl.fetch.MasterConfiguration;
import org.apache.isis.extensions.commandreplay.impl.fetch.CommandFetcher;
import org.apache.isis.extensions.commandreplay.impl.spi.ReplayCommandExecutionController;
import org.apache.isis.extensions.commandreplay.impl.util.Holder;
import org.apache.isis.schema.cmd.v2.CommandDto;

import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdoRepository;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ReplayableCommandExecution
        extends CommandExecutionAbstract {

    private final MasterConfiguration slaveConfig;

    public ReplayableCommandExecution(final MasterConfiguration slaveConfig) {
        super(CommandExecutorService.SudoPolicy.SWITCH);
        this.slaveConfig = slaveConfig;
    }

    @Override
    protected void doExecute(final Object context) {
        Holder<SlaveStatus> holder = (Holder<SlaveStatus>) context;
        try {
            replicateAndRunCommands();
        } catch (StatusException e) {
            holder.setObject(e.slaveStatus);
        }
    }

    private void replicateAndRunCommands() throws  StatusException  {

        CommandJdo hwmCommand = null;
        if(!isRunning()) {
            log.debug("ReplayableCommandExecution is paused");
            return;
        }

        while(isRunning()) {

            if(hwmCommand == null) {
                // first time through the loop we need to find the HWM command
                // (subsequent iterations we use the command from before as the HWM)
                log.debug("searching for hwm on slave ...");
                hwmCommand = commandJdoRepository.findReplayHwm();
            }

            if(hwmCommand == null) {
                log.debug("could not find HWM on slave, breaking");
                return;

            }

            log.debug("current hwm transactionId = {} {} {} {}",
                    hwmCommand.getUniqueId(), hwmCommand.getTimestamp(),
                    hwmCommand.getExecuteIn(), hwmCommand.getMemberIdentifier());


            boolean fetchNext;
            switch (hwmCommand.getExecuteIn()) {
            case FOREGROUND:
                fetchNext = true;
                break;
            case REPLAYABLE:
                if(hwmCommand.getReplayState() == null || hwmCommand.getReplayState() == ReplayState.PENDING) {

                    // the HWM has not been replayed.
                    // this might be because it has been marked for retry by the administrator.
                    // so, we will just use it directly

                    fetchNext = false;
                } else {
                    //
                    // check that the current HWM was replayed successfully, otherwise break out
                    //
                    if(hwmCommand.getReplayState().isFailed()) {
                        log.info("Command xactnId={} hit replay error", hwmCommand.getUniqueId());
                        return;
                    }
                    fetchNext = true;
                }
                break;
            case BACKGROUND:
            default:
                log.error(
                        "HWM command xactnId={} should be either FOREGROUND or REPLAYABLE but is instead {}; aborting",
                        hwmCommand.getUniqueId(), hwmCommand.getExecuteIn());
                return;
            }

            if(fetchNext) {
                //
                // replicate next command from master (if any)
                //
                final CommandDto commandDto = commandFetcher.fetchCommand(hwmCommand);
                if (commandDto == null) {
                    log.info("No more commands found, breaking out");
                    return;
                }

                hwmCommand = transactionService.executeWithinTransaction(
                        () -> commandJdoRepository.saveForReplay(commandDto));
            }

            log.info("next HWM transactionId = {} {} {} {}", hwmCommand.getUniqueId());



            //
            // run command
            //
            this.execute(hwmCommand, transactionService);


            //
            // find background commands, and run them
            //
            final CommandJdo parent = hwmCommand;
            final List<CommandJdo> backgroundCommands =
                    transactionService.executeWithinTransaction(
                            () -> commandJdoRepository.findBackgroundCommandsByParent(parent));
            for (final CommandJdo backgroundCommand : backgroundCommands) {
                execute(backgroundCommand, transactionService);
            }


            //
            // if hit an issue, then mark this as in error.
            // this will effectively block the running of any further commands until the adminstrator fixes
            //
            transactionService.executeWithinTransaction(() -> analysisService.analyse(parent));
        }
    }

    private boolean isRunning() {

        // if no controller implementation provided, then just continue
        if (controller == null) {
            return true;
        }

        final ReplayCommandExecutionController.State state =
                transactionService.executeWithinTransaction(() -> controller.getState());

        // if null, then not yet initialized, so fail back to not running
        if(state == null) {
            return false;
        }

        return state == ReplayCommandExecutionController.State.RUNNING;
    }


    @Inject TransactionService transactionService;
    @Inject
    CommandFetcher commandFetcher;
    @Inject
    CommandJdoRepository commandJdoRepository;
    @Inject
    CommandReplayAnalysisService analysisService;
    @Inject ReplayCommandExecutionController controller;
}