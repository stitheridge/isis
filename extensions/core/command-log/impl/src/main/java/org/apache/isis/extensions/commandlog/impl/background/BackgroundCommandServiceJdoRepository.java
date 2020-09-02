package org.apache.isis.extensions.commandlog.impl.background;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.OrderPrecedence;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandServiceJdoRepository;

import lombok.extern.log4j.Log4j2;

/**
 * Provides supporting functionality for querying
 * {@link CommandJdo command} entities that have been persisted
 * to execute in the background.
 *
 * <p>
 * This supporting service with no UI and no side-effects, and is there are no other implementations of the service,
 * thus has been annotated with {@link org.apache.isis.applib.annotation.DomainService}.  This means that there is no
 * need to explicitly register it as a service (eg in <tt>isis.properties</tt>).
 */
@Service()
@Named("isisExtensionsCommandLog.BackgroundCommandServiceJdoRepository")
@Order(OrderPrecedence.MIDPOINT)
@Qualifier("Jdo")
@Log4j2
public class BackgroundCommandServiceJdoRepository {

    public List<CommandJdo> findByParent(CommandJdo parent) {
        return commandServiceRepository.findBackgroundCommandsByParent(parent);
    }

    public List<CommandJdo> findBackgroundCommandsNotYetStarted() {
        return commandServiceRepository.findBackgroundCommandsNotYetStarted();
    }

    @Inject CommandServiceJdoRepository commandServiceRepository;
}
