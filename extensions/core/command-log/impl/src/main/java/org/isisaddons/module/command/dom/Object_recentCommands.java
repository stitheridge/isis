package org.isisaddons.module.command.dom;

import java.util.List;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.Mixin;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.HasTransactionId;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.bookmark.BookmarkService;

import org.isisaddons.module.command.IsisModuleExtCommandLogImpl;

/**
 * This mixin contributes a <tt>recentCommands</tt> action to any domain object
 * (unless also a {@link HasTransactionId} - cmmands don't themselves have commands).
 */
@Mixin(method = "act")
@Action(
    semantics = SemanticsOf.SAFE,
    domainEvent = Object_recentCommands.ActionDomainEvent.class
)
@ActionLayout(
    cssClassFa = "fa-bolt",
    position = ActionLayout.Position.PANEL_DROPDOWN
)
public class Object_recentCommands {

    public static class ActionDomainEvent
            extends IsisModuleExtCommandLogImpl.ActionDomainEvent<Object_recentCommands> { }

    private final Object domainObject;
    public Object_recentCommands(final Object domainObject) {
        this.domainObject = domainObject;
    }

    @MemberOrder(name = "datanucleusIdLong", sequence = "900.1")
    public List<CommandJdo> act() {
        final Bookmark bookmark = bookmarkService.bookmarkFor(domainObject);
        return commandServiceRepository.findRecentByTarget(bookmark);
    }
    /**
     * Hide if the contributee is itself {@link HasTransactionId}
     * (commands don't have commands).
     */
    public boolean hideAct() {
        return (domainObject instanceof HasTransactionId);
    }

    @javax.inject.Inject
    CommandServiceJdoRepository commandServiceRepository;

    @javax.inject.Inject
    BookmarkService bookmarkService;

}
