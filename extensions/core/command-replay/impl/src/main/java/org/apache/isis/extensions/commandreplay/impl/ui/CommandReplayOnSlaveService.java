package org.apache.isis.extensions.commandreplay.impl.ui;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.CommandReification;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.DomainServiceLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.jaxb.JaxbService;
import org.apache.isis.applib.value.Clob;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandServiceJdoRepository;
import org.apache.isis.extensions.commandreplay.impl.IsisModuleExtCommandReplayImpl;
import org.apache.isis.schema.cmd.v2.CommandDto;
import org.apache.isis.schema.cmd.v2.CommandsDto;

import lombok.extern.log4j.Log4j2;

@DomainService(
        nature = NatureOfService.VIEW,
        objectType = "isiscommand.CommandReplayOnSlaveService"
)
@DomainServiceLayout(
        named = "Activity",
        menuBar = DomainServiceLayout.MenuBar.SECONDARY
)
@Log4j2
public class CommandReplayOnSlaveService {

    public static abstract class ActionDomainEvent
            extends IsisModuleExtCommandReplayImpl.ActionDomainEvent<CommandReplayOnMasterService> { }


    public static class FindReplayHwmOnSlaveDomainEvent extends ActionDomainEvent { }
    @Action(domainEvent = FindReplayHwmOnSlaveDomainEvent.class, semantics = SemanticsOf.SAFE)
    @ActionLayout(cssClassFa = "fa-bath")
    @MemberOrder(sequence="60.1")
    public CommandJdo findReplayHwmOnSlave() {
        return commandServiceJdoRepository.findReplayHwm();
    }



    public static class UploadCommandsToSlaveDomainEvent extends ActionDomainEvent { }
    @Action(
        command = CommandReification.DISABLED,
        domainEvent = UploadCommandsToSlaveDomainEvent.class,
        semantics = SemanticsOf.NON_IDEMPOTENT
    )
    @ActionLayout(cssClassFa = "fa-upload")
    @MemberOrder(sequence="60.2")
    public void uploadCommandsToSlave(final Clob commandsDtoAsXml) {
        final CharSequence chars = commandsDtoAsXml.getChars();
        List<CommandDto> commandDtoList;

        try {
            final CommandsDto commandsDto = jaxbService.fromXml(CommandsDto.class, chars.toString());
            commandDtoList = commandsDto.getCommandDto();

        } catch(Exception ex) {
            final CommandDto commandDto = jaxbService.fromXml(CommandDto.class, chars.toString());
            commandDtoList = Collections.singletonList(commandDto);
        }

        for (final CommandDto commandDto : commandDtoList) {
            commandServiceJdoRepository.saveForReplay(commandDto);
        }
    }



    @Inject
    CommandServiceJdoRepository commandServiceJdoRepository;
    @Inject JaxbService jaxbService;

}

