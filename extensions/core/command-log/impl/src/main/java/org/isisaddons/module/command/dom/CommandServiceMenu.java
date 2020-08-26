package org.isisaddons.module.command.dom;

import java.util.List;
import java.util.UUID;

import org.joda.time.LocalDate;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.BookmarkPolicy;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.DomainServiceLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.Optionality;
import org.apache.isis.applib.annotation.Parameter;
import org.apache.isis.applib.annotation.ParameterLayout;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.clock.ClockService;

import org.isisaddons.module.command.IsisModuleExtCommandLogImpl;

@DomainService(
        nature = NatureOfService.VIEW,
        objectType = "isisextcorecommandlog.CommandServiceMenu"
)
@DomainServiceLayout(
        named = "Activity"
        , menuBar = DomainServiceLayout.MenuBar.SECONDARY
)
public class CommandServiceMenu {

    public static abstract class PropertyDomainEvent<T>
            extends IsisModuleExtCommandLogImpl.PropertyDomainEvent<CommandServiceMenu, T> { }
    public static abstract class CollectionDomainEvent<T>
            extends IsisModuleExtCommandLogImpl.CollectionDomainEvent<CommandServiceMenu, T> { }
    public static abstract class ActionDomainEvent
            extends IsisModuleExtCommandLogImpl.ActionDomainEvent<CommandServiceMenu> {
    }


    public static class ActiveCommandsDomainEvent extends ActionDomainEvent { }
    @Action(domainEvent = ActiveCommandsDomainEvent.class, semantics = SemanticsOf.SAFE)
    @ActionLayout(bookmarking = BookmarkPolicy.AS_ROOT, cssClassFa = "fa-bolt")
    @MemberOrder(sequence="10")
    public List<CommandJdo> activeCommands() {
        return commandServiceRepository.findCurrent();
    }
    public boolean hideActiveCommands() {
        return commandServiceRepository == null;
    }


    public static class FindCommandsDomainEvent extends ActionDomainEvent { }
    @Action(domainEvent = FindCommandsDomainEvent.class, semantics = SemanticsOf.SAFE)
    @ActionLayout(cssClassFa = "fa-search")
    @MemberOrder(sequence="20")
    public List<CommandJdo> findCommands(
            @Parameter(optionality= Optionality.OPTIONAL)
            @ParameterLayout(named="From")
            final LocalDate from,
            @Parameter(optionality= Optionality.OPTIONAL)
            @ParameterLayout(named="To")
            final LocalDate to) {
        return commandServiceRepository.findByFromAndTo(from, to);
    }
    public boolean hideFindCommands() {
        return commandServiceRepository == null;
    }
    public LocalDate default0FindCommands() {
        return clockService.nowAsJodaLocalDate().minusDays(7);
    }
    public LocalDate default1FindCommands() {
        return clockService.nowAsJodaLocalDate();
    }


    public static class FindCommandByIdDomainEvent extends ActionDomainEvent { }
    @Action(domainEvent = FindCommandByIdDomainEvent.class, semantics = SemanticsOf.SAFE)
    @ActionLayout(cssClassFa = "fa-crosshairs")
    @MemberOrder(sequence="30")
    public CommandJdo findCommandById(
            @ParameterLayout(named="Transaction Id")
            final UUID transactionId) {
        return commandServiceRepository.findByTransactionId(transactionId).orElse(null);
    }
    public boolean hideFindCommandById() {
        return commandServiceRepository == null;
    }



    @javax.inject.Inject
    CommandServiceJdoRepository commandServiceRepository;

    @javax.inject.Inject
    ClockService clockService;

}

