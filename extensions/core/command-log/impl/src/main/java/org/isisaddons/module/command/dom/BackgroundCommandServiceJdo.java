package org.isisaddons.module.command.dom;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.annotation.CommandExecuteIn;
import org.apache.isis.applib.services.background.BackgroundCommandService;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.clock.ClockService;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.factory.FactoryService;
import org.apache.isis.applib.util.schema.CommandDtoUtils;
import org.apache.isis.schema.cmd.v2.CommandDto;
import org.apache.isis.schema.common.v2.OidDto;

/**
 * Persists a memento-ized action such that it can be executed asynchronously,
 * for example through a Quartz scheduler (using
 * {@link BackgroundCommandExecutionFromBackgroundCommandServiceJdo}).
 */
@Service()
public class BackgroundCommandServiceJdo implements BackgroundCommandService {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(BackgroundCommandServiceJdo.class);

    @Override
    public void schedule(
            final CommandDto dto,
            final Command parentCommand,
            final String targetClassName,
            final String targetActionName,
            final String targetArgs) {

        final CommandJdo backgroundCommand =
                newBackgroundCommand(parentCommand, targetClassName, targetActionName, targetArgs);

        final OidDto firstTarget = dto.getTargets().getOid().get(0);
        backgroundCommand.setTargetStr(Bookmark.from(firstTarget).toString());
        backgroundCommand.internal().setMemento(CommandDtoUtils.toXml(dto));
        backgroundCommand.setMemberIdentifier(dto.getMember().getMemberIdentifier());

        commandServiceJdoRepository.persist(backgroundCommand);
    }

    private CommandJdo newBackgroundCommand(
            final Command parentCommand,
            final String targetClassName,
            final String targetActionName,
            final String targetArgs) {

        final CommandJdo backgroundCommand = factoryService.instantiate(CommandJdo.class);

        backgroundCommand.internal().setParent(parentCommand);

        // workaround for ISIS-1472; parentCommand not properly set up if invoked via RO viewer
        if(parentCommand.getMemberIdentifier() == null) {
            backgroundCommand.internal().setParent(null);
        }

        final UUID transactionId = UUID.randomUUID();
        final String user = parentCommand.getUser();

        backgroundCommand.setTransactionId(transactionId);

        backgroundCommand.internal().setUser(user);
        backgroundCommand.internal().setTimestamp(clockService.nowAsJavaSqlTimestamp());
        backgroundCommand.internal().setExecuteIn(CommandExecuteIn.BACKGROUND);

        backgroundCommand.setTargetClass(targetClassName);
        backgroundCommand.setTargetAction(targetActionName);

        backgroundCommand.internal().setArguments(targetArgs);
        backgroundCommand.internal().setPersistHint(true);

        return backgroundCommand;
    }


    @Inject
    CommandServiceJdoRepository commandServiceJdoRepository;

    @Inject
    FactoryService factoryService;

    @Inject
    ClockService clockService;

}

