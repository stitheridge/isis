package org.apache.isis.extensions.commandlog.impl.jdo;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.bookmark.BookmarkService;
import org.apache.isis.applib.services.message.MessageService;
import org.apache.isis.extensions.commandlog.impl.IsisModuleExtCommandLogImpl;

@Action(
    semantics = SemanticsOf.SAFE
    , domainEvent = CommandJdo_openResultObject.ActionDomainEvent.class
    , associateWith = "result"
)
@ActionLayout(named = "Open")
public class CommandJdo_openResultObject {

    public static abstract class ActionDomainEvent
            extends IsisModuleExtCommandLogImpl.ActionDomainEvent<CommandJdo_openResultObject> { }

    private final CommandJdo commandJdo;
    public CommandJdo_openResultObject(CommandJdo commandJdo) {
        this.commandJdo = commandJdo;
    }

    @MemberOrder(name="ResultStr", sequence="1")
    public Object act() {
        return lookupBookmark(commandJdo.getResult());
    }
    public boolean hideAct() {
        return commandJdo.getResult() == null;
    }

    private Object lookupBookmark(Bookmark bookmark) {
        try {
            return bookmarkService != null ? bookmarkService.lookup(bookmark) : null;
        } catch (RuntimeException ex) {
            if (ex.getClass().getName().contains("ObjectNotFoundException")) {
                messageService.warnUser("Object not found - has it since been deleted?");
                return null;
            } else {
                throw ex;
            }
        }
    }

    @javax.inject.Inject
    BookmarkService bookmarkService;

    @javax.inject.Inject
    MessageService messageService;



}
