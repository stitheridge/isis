package org.isisaddons.module.command.dom;

import java.util.Collections;
import java.util.List;

import org.apache.isis.applib.annotation.Collection;
import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.services.HasUsername;

import org.isisaddons.module.command.IsisModuleExtCommandLogImpl;


@Collection(
    domainEvent = HasUsername_recentCommandsByUser.CollectionDomainEvent.class
)
@CollectionLayout(
    defaultView = "table"
)
public class HasUsername_recentCommandsByUser {

    public static class CollectionDomainEvent
            extends IsisModuleExtCommandLogImpl.CollectionDomainEvent<HasUsername_recentCommandsByUser, CommandJdo> { }

    private final HasUsername hasUsername;
    public HasUsername_recentCommandsByUser(final HasUsername hasUsername) {
        this.hasUsername = hasUsername;
    }

    @MemberOrder(name="user", sequence = "3")
    public List<CommandJdo> coll() {
        final String username = hasUsername.getUsername();
        return username != null
                ? commandServiceRepository.findRecentByUser(username)
                : Collections.emptyList();
    }
    public boolean hideColl() {
        return hasUsername.getUsername() == null;
    }

    @javax.inject.Inject
    private CommandServiceJdoRepository commandServiceRepository;
}
