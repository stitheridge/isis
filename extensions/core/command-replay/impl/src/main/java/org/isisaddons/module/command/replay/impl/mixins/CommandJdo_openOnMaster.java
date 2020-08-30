package org.isisaddons.module.command.replay.impl.mixins;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.isis.applib.ApplicationException;
import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.bookmark.BookmarkService;
import org.apache.isis.core.config.IsisConfiguration;

import org.isisaddons.module.command.dom.CommandJdo;
import org.isisaddons.module.command.replay.IsisModuleExtCommandReplayImpl;
import org.isisaddons.module.command.replay.impl.ConfigurationKeys;

@Action(
        semantics = SemanticsOf.SAFE,
        domainEvent = CommandJdo_openOnMaster.ActionDomainEvent.class
)
public class CommandJdo_openOnMaster<T> {

    public static class ActionDomainEvent
            extends IsisModuleExtCommandReplayImpl.ActionDomainEvent<CommandJdo_openOnMaster> { }

    private final CommandJdo commandJdo;
    public CommandJdo_openOnMaster(CommandJdo commandJdo) {
        this.commandJdo = commandJdo;
    }

    @MemberOrder(name = "transactionId", sequence = "1")
    public URL act() {
        final String baseUrlPrefix = lookupBaseUrlPrefix();
        final String urlSuffix = bookmarkService2.bookmarkFor(commandJdo).toString();

        try {
            return new URL(baseUrlPrefix + urlSuffix);
        } catch (MalformedURLException e) {
            throw new ApplicationException(e);
        }
    }

    public boolean hideAct() {
        return lookupBaseUrlPrefix() == null;
    }

    private String lookupBaseUrlPrefix() {
        return isisConfiguration.getExtensions().getCommandReplay().getMaster().getBaseUrlEndUser()
                .map(x -> !x.endsWith("/") ? x + "/" : x)
                .map(x -> x + "wicket/entity/")
                .orElse(null);
    }

    @Inject IsisConfiguration isisConfiguration;
    @Inject BookmarkService bookmarkService2;

}
