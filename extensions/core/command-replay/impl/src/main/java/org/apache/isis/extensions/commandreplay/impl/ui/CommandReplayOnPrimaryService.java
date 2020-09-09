package org.apache.isis.extensions.commandreplay.impl.ui;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.isis.applib.ApplicationException;
import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.DomainServiceLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.ParameterLayout;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.conmap.command.ContentMappingServiceForCommandsDto;
import org.apache.isis.applib.services.jaxb.JaxbService;
import org.apache.isis.applib.services.message.MessageService;
import org.apache.isis.applib.value.Clob;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdoRepository;
import org.apache.isis.extensions.commandreplay.impl.IsisModuleExtCommandReplayImpl;
import org.apache.isis.schema.cmd.v2.CommandDto;
import org.apache.isis.schema.cmd.v2.CommandsDto;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@DomainService(
        nature = NatureOfService.VIEW,
        objectType = "isisExtensionsCommandReplay.CommandReplayOnPrimaryService"
)
@DomainServiceLayout(
        named = "Activity",
        menuBar = DomainServiceLayout.MenuBar.SECONDARY
)
@Log4j2
public class CommandReplayOnPrimaryService {

    public static abstract class ActionDomainEvent
            extends IsisModuleExtCommandReplayImpl.ActionDomainEvent<CommandReplayOnPrimaryService> { }


    public static class FindCommandsOnPrimarySinceDomainEvent extends ActionDomainEvent { }
    public static class NotFoundException extends ApplicationException {
        private static final long serialVersionUID = 1L;
        @Getter
        private final UUID uniqueId;
        public NotFoundException(final UUID uniqueId) {
            super("Command not found");
            this.uniqueId = uniqueId;
        }
    }

    /**
     * These actions should be called with HTTP Accept Header set to:
     * <code>application/xml;profile="urn:org.restfulobjects:repr-types/action-result";x-ro-domain-type="org.apache.isis.schema.cmd.v1.CommandsDto"</code>
     *
     * @param transactionId - to search from.  This transactionId will <i>not</i> be included in the response.
     * @param batchSize - the maximum number of commands to return.  If not specified, all found will be returned.
     *
     * @return
     * @throws NotFoundException - if the command with specified transaction cannot be found.
     */
    @Action(domainEvent = FindCommandsOnPrimarySinceDomainEvent.class, semantics = SemanticsOf.SAFE)
    @ActionLayout(cssClassFa = "fa-files-o")
    @MemberOrder(sequence="40")
    public List<CommandJdo> findCommandsOnMasterSince(
            @Nullable
            @ParameterLayout(named="Transaction Id")
            final UUID transactionId,
            @Nullable
            @ParameterLayout(named="Batch size")
            final Integer batchSize)
            throws NotFoundException {
        final List<CommandJdo> commands = commandServiceRepository.findSince(transactionId, batchSize);
        if(commands == null) {
            throw new NotFoundException(transactionId);
        }
        return commands;
    }
    public Integer default1FindCommandsOnMasterSince() {
        return 25;
    }



    public static class DownloadCommandsOnMasterSinceDomainEvent extends ActionDomainEvent { }
    /**
     * These actions should be called with HTTP Accept Header set to:
     * <code>application/xml;profile="urn:org.restfulobjects:repr-types/action-result";x-ro-domain-type="org.apache.isis.schema.cmd.v1.CommandsDto"</code>
     *
     * @param uniqueId - to search from.  This transactionId will <i>not</i> be included in the response.
     * @param batchSize - the maximum number of commands to return.  If not specified, all found will be returned.
     *
     * @return
     * @throws NotFoundException - if the command with specified transaction cannot be found.
     */
    @Action(domainEvent = DownloadCommandsOnMasterSinceDomainEvent.class, semantics = SemanticsOf.SAFE)
    @ActionLayout(cssClassFa = "fa-download")
    @MemberOrder(sequence="50")
    public Clob downloadCommandsOnMasterSince(
            @Nullable
            final UUID uniqueId,
            @Nullable
            final Integer batchSize,
            final String filenamePrefix) {
        final List<CommandJdo> commands = commandServiceRepository.findSince(uniqueId, batchSize);
        if(commands == null) {
            messageService.informUser("No commands found");
        }

        final CommandsDto commandsDto =
                contentMappingServiceForCommandsDto.map(commands);

        final String fileName = String.format(
                "%s_%s.xml", filenamePrefix, elseDefault(uniqueId));

        final String xml = jaxbService.toXml(commandsDto);
        return new Clob(fileName, "application/xml", xml);
    }
    public Integer default1DownloadCommandsOnMasterSince() {
        return 25;
    }
    public String default2DownloadCommandsOnMasterSince() {
        return "commands_since";
    }



    public static class DownloadCommandFromPrimaryDomainEvent extends ActionDomainEvent { }
    /**
     * This action should be called with HTTP Accept Header set to:
     * <code>application/xml;profile="urn:org.restfulobjects:repr-types/action-result";x-ro-domain-type="org.apache.isis.schema.cmd.v1.CommandDto"</code>
     *
     * @param uniqueId - to download.
     *
     * @return
     * @throws NotFoundException - if the command with specified transaction cannot be found.
     */
    @Action(domainEvent = DownloadCommandFromPrimaryDomainEvent.class, semantics = SemanticsOf.SAFE)
    @ActionLayout(cssClassFa = "fa-download")
    @MemberOrder(sequence="50")
    public Clob downloadCommandById(
            final UUID uniqueId,
            final String filenamePrefix) {

        return commandServiceRepository.findByUniqueId(uniqueId)
                .map(commandJdo -> {

                    final CommandDto commandDto = commandJdo.getCommandDto();

                    final String fileName = String.format(
                            "%s_%s.xml", filenamePrefix, elseDefault(uniqueId));

                    final String xml = jaxbService.toXml(commandDto);
                    return new Clob(fileName, "application/xml", xml);

                }).orElseGet(() -> {
                    messageService.informUser("No command found");
                    return null;
                });
    }
    public String default1DownloadCommandById() {
        return "command";
    }


    private static String elseDefault(final UUID uuid) {
        return uuid != null ? uuid.toString() : "00000000-0000-0000-0000-000000000000";
    }

    @Inject CommandJdoRepository commandServiceRepository;
    @Inject JaxbService jaxbService;
    @Inject MessageService messageService;
    @Inject ContentMappingServiceForCommandsDto contentMappingServiceForCommandsDto;
}

