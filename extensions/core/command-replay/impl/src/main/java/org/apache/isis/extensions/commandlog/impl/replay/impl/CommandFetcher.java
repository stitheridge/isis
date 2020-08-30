package org.apache.isis.extensions.commandlog.impl.replay.impl;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.jaxb.JaxbService;
import org.apache.isis.extensions.jaxrsclient.applib.client.JaxRsClient;
import org.apache.isis.extensions.jaxrsclient.applib.client.JaxRsResponse;
import org.apache.isis.extensions.jaxrsclient.impl.client.JaxRsClientDefault;
import org.apache.isis.schema.cmd.v2.CommandDto;
import org.apache.isis.schema.cmd.v2.CommandsDto;

import lombok.extern.log4j.Log4j2;


@DomainService()
@Log4j2
public class CommandFetcher {

    static final String URL_SUFFIX =
            "services/isiscommand.CommandReplayOnMasterService/actions/findCommandsOnMasterSince/invoke";


    /**
     * Replicates a single command.
     *
     * @param previousHwm
     * @param slaveConfig
     *
     * @return
     * @throws StatusException
     */
    @Programmatic
    public CommandDto fetchCommand(
            final Command previousHwm,
            final SlaveConfiguration slaveConfig)
            throws StatusException {

        log.debug("finding command on master ...");

        final CommandsDto commandsDto = fetchCommands(previousHwm, slaveConfig);

        if (commandsDto == null) {
            return null;
        }
        final List<CommandDto> commandDto = commandsDto.getCommandDto();
        if (commandDto.isEmpty()) {
            return null;
        }
        return commandDto.get(0);
    }

    /**
     * @return - the commands, or <tt>null</tt> if none were found
     * @throws StatusException
     */
    private CommandsDto fetchCommands(
            final Command previousHwm,
            final SlaveConfiguration slaveConfig) throws StatusException {
        final UUID transactionId = previousHwm != null ? previousHwm.getUniqueId() : null;

        log.debug("finding commands on master ...");

        final URI uri = buildUri(transactionId, slaveConfig);

        final JaxRsResponse response = callMaster(slaveConfig, uri);

        final CommandsDto commandsDto = unmarshal(response, uri);

        final int size = commandsDto.getCommandDto().size();
        if(size == 0) {
            return null;
        }
        return commandsDto;
    }



    private URI buildUri(final UUID transactionId, final SlaveConfiguration slaveConfig) {
        final UriBuilder uriBuilder = UriBuilder.fromUri(
                transactionId != null
                        ? String.format(
                        "%s%s?transactionId=%s&batchSize=%d",
                        slaveConfig.masterBaseUrl, URL_SUFFIX, transactionId, slaveConfig.masterBatchSize)
                        : String.format(
                        "%s%s?batchSize=%d",
                        slaveConfig.masterBaseUrl, URL_SUFFIX, slaveConfig.masterBatchSize)
        );
        final URI uri = uriBuilder.build();
        log.info("uri = {}", uri);
        return uri;
    }

    private JaxRsResponse callMaster(final SlaveConfiguration slaveConfig, final URI uri) throws StatusException {
        final JaxRsResponse response;
        final JaxRsClient jaxRsClient = new JaxRsClientDefault();
        try {
            final String user = slaveConfig.masterUser;
            final String password = slaveConfig.masterPassword;
            response = jaxRsClient.get(uri, CommandsDto.class, JaxRsClient.ReprType.ACTION_RESULT, user, password);
            int status = response.getStatus();
            if(status != Response.Status.OK.getStatusCode()) {
                final String entity = readEntityFrom(response);
                if(entity != null) {
                    log.warn("status: {}, entity: \n{}", status, entity);
                } else {
                    log.warn("status: {}, unable to read entity from response", status);
                }
                throw new StatusException(SlaveStatus.REST_CALL_FAILING);
            }
        } catch(Exception ex) {
            log.warn("rest call failed", ex);
            throw new StatusException(SlaveStatus.REST_CALL_FAILING, ex);
        }
        return response;
    }

    private CommandsDto unmarshal(final JaxRsResponse response, final URI uri) throws StatusException {
        CommandsDto commandsDto;
        String entity = "<unable to read from response entity>";
        try {
            entity = readEntityFrom(response);
            final JaxbService jaxbService = new JaxbService.Simple();
            commandsDto = jaxbService.fromXml(CommandsDto.class, entity);
            log.debug("commands:\n{}", entity);
        } catch(Exception ex) {
            log.warn("unable to unmarshal entity from {} to CommandsDto.class; was:\n{}", uri, entity);
            throw new StatusException(SlaveStatus.FAILED_TO_UNMARSHALL_RESPONSE, ex);
        }
        return commandsDto;
    }

    private static String readEntityFrom(final JaxRsResponse response) {
        try {
            return response.readEntity(String.class);
        } catch(Exception e) {
            return null;
        }
    }

    @Inject SlaveConfiguration slaveConfiguration;

}