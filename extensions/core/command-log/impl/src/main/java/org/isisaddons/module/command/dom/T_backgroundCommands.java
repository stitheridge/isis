package org.isisaddons.module.command.dom;

import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Collection;
import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.bookmark.BookmarkService;
import org.apache.isis.applib.services.queryresultscache.QueryResultsCache;

import org.isisaddons.module.command.IsisModuleExtCommandLogImpl;

@Collection(
    domainEvent = T_backgroundCommands.CollectionDomainEvent.class
)
@CollectionLayout(
    defaultView = "table"
)
public abstract class T_backgroundCommands<T> {

    public static class CollectionDomainEvent extends IsisModuleExtCommandLogImpl.CollectionDomainEvent<T_backgroundCommands, CommandJdo> { }

    private final T domainObject;
    public T_backgroundCommands(final T domainObject) {
        this.domainObject = domainObject;
    }

    public List<CommandJdo> $$() {
        return findRecentBackground();
    }

    private List<CommandJdo> findRecentBackground() {
        final Bookmark bookmark = bookmarkService.bookmarkFor(domainObject);
        return queryResultsCache.execute(
                () -> commandServiceJdoRepository.findRecentBackgroundByTarget(bookmark)
                , T_backgroundCommands.class
                , "findRecentBackground"
                , domainObject);
    }

    @Inject
    CommandServiceJdoRepository commandServiceJdoRepository;
    @Inject
    BookmarkService bookmarkService;
    @Inject
    QueryResultsCache queryResultsCache;

}
