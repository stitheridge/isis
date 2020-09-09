package org.apache.isis.extensions.commandreplay.impl.mixins;

import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.command.CommandExecutorService;
import org.apache.isis.applib.services.message.MessageService;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdoRepository;
import org.apache.isis.extensions.commandreplay.impl.IsisModuleExtCommandReplayImpl;
import org.apache.isis.extensions.commandreplay.impl.fetch.MasterConfiguration;
import org.apache.isis.extensions.commandreplay.impl.StatusException;
import org.apache.isis.schema.cmd.v2.CommandDto;

import org.apache.isis.extensions.commandreplay.impl.fetch.CommandFetcher;
import org.apache.isis.extensions.commandreplay.impl.analysis.CommandReplayAnalysisService;

import lombok.val;

@Action(
        semantics = SemanticsOf.NON_IDEMPOTENT,
        domainEvent = CommandJdo_replayNext.ActionDomainEvent.class
)
public class CommandJdo_replayNext {

    public static class ActionDomainEvent extends IsisModuleExtCommandReplayImpl.ActionDomainEvent<CommandJdo_replayNext> { }

    private final CommandJdo commandJdo;
    public CommandJdo_replayNext(CommandJdo commandJdo) {
        this.commandJdo = commandJdo;
    }

    @MemberOrder(name = "executeIn", sequence = "3")
    public CommandJdo act() throws StatusException {

        // double check this is still the HWM
        final CommandJdo replayHwm = commandJdoRepository.findReplayHwm();
        if(commandJdo != replayHwm) {
            messageService.informUser("HWM has changed");
            return replayHwm;
        }

        final CommandJdo nextHwm = fetchNext();
        if(nextHwm == null) {
            messageService.informUser("No more commands on master");
            return commandJdo;
        }

        execute(nextHwm);
        analysisService.analyse(nextHwm);

        return nextHwm;
    }


    private CommandJdo fetchNext() throws StatusException {
        final CommandDto commandDto = commandFetcher.fetchCommand(this.commandJdo);
        return commandDto == null
                ? null
                : commandJdoRepository.saveForReplay(commandDto);
    }

    private void execute(final CommandJdo hwmCommand) {

        // execute the hwm command
        commandExecutorService.executeCommand(CommandExecutorService.SudoPolicy.SWITCH, hwmCommand.getCommandDto());

        // find child commands, and run them
        val childCommands = commandJdoRepository.findByParent(hwmCommand);
        for (final CommandJdo childCommand : childCommands) {
            commandExecutorService.executeCommand(CommandExecutorService.SudoPolicy.SWITCH, childCommand.getCommandDto());
        }
    }

    public String disableAct() {
        final CommandJdo replayHwm = commandJdoRepository.findReplayHwm();

        if(commandJdo != replayHwm) {
            return "This action can only be performed against the 'HWM' command on the slave";
        }
        if(commandJdo.getReplayState() != null && commandJdo.getReplayState().isFailed()) {
            return "Replayable command is in error.  Exclude the command to continue.";
        }
        if(!commandJdo.isComplete()) {
            return "Replayable command is not complete";
        }

        return null;
    }

    public boolean hideAct() {
        return !masterConfiguration.isConfigured();
    }


    @Inject
    CommandJdoRepository commandJdoRepository;
    @Inject CommandFetcher commandFetcher;
    @Inject CommandExecutorService commandExecutorService;
    @Inject
    MasterConfiguration masterConfiguration;
    @Inject MessageService messageService;
    @Inject CommandReplayAnalysisService analysisService;
}
