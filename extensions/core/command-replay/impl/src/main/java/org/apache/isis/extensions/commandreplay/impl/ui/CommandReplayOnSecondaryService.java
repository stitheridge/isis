package org.apache.isis.extensions.commandreplay.impl.ui;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.DomainServiceLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.jaxb.JaxbService;
import org.apache.isis.applib.value.Clob;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdoRepository;
import org.apache.isis.extensions.commandreplay.impl.IsisModuleExtCommandReplayImpl;
import org.apache.isis.schema.cmd.v2.CommandDto;
import org.apache.isis.schema.cmd.v2.CommandsDto;

import lombok.extern.log4j.Log4j2;

@DomainService(
        nature = NatureOfService.VIEW,
        objectType = "isisExtensionsCommandReplay.CommandReplayOnSecondaryService"
)
@DomainServiceLayout(
        named = "Activity",
        menuBar = DomainServiceLayout.MenuBar.SECONDARY
)
@Log4j2
public class CommandReplayOnSecondaryService {

    public static abstract class ActionDomainEvent
            extends IsisModuleExtCommandReplayImpl.ActionDomainEvent<CommandReplayOnSecondaryService> { }

    public static class FindReplayHwmOnSecondaryDomainEvent extends ActionDomainEvent { }
    @Action(domainEvent = FindReplayHwmOnSecondaryDomainEvent.class, semantics = SemanticsOf.SAFE)
    @ActionLayout(cssClassFa = "fa-bath")
    @MemberOrder(sequence="60.1")
    public CommandJdo findReplayHwmOnSecondary() {
        return commandJdoRepository.findReplayHwm();
    }



    public static class UploadCommandsToSecondaryDomainEvent extends ActionDomainEvent { }
    @Action(
        domainEvent = UploadCommandsToSecondaryDomainEvent.class,
        semantics = SemanticsOf.NON_IDEMPOTENT
    )
    @ActionLayout(cssClassFa = "fa-upload")
    @MemberOrder(sequence="60.2")
    public void uploadCommandsToSecondary(final Clob commandsDtoAsXml) {
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
            commandJdoRepository.saveForReplay(commandDto);
        }
    }


    @Inject CommandJdoRepository commandJdoRepository;
    @Inject JaxbService jaxbService;

}

