package org.apache.isis.extensions.commandlog.impl.ui;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.BookmarkPolicy;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.DomainServiceLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.applib.annotation.Optionality;
import org.apache.isis.applib.annotation.OrderPrecedence;
import org.apache.isis.applib.annotation.Parameter;
import org.apache.isis.applib.annotation.ParameterLayout;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.clock.ClockService;
import org.apache.isis.extensions.commandlog.impl.IsisModuleExtCommandLogImpl;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdoRepository;

@DomainService(
    nature = NatureOfService.VIEW,
    objectType = "isisExtensionsCommandLog.CommandServiceMenu"
)
@DomainServiceLayout(
    named = "Activity",
    menuBar = DomainServiceLayout.MenuBar.SECONDARY
)
@Service
@Named("isisExtensionsCommandLog.CommandServiceMenu")
@Order(OrderPrecedence.MIDPOINT)
@Qualifier("Jdo")
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
        return commandServiceRepository.findByUniqueId(transactionId).orElse(null);
    }
    public boolean hideFindCommandById() {
        return commandServiceRepository == null;
    }


    @Inject
    CommandJdoRepository commandServiceRepository;
    @Inject ClockService clockService;
}

