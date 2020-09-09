package org.apache.isis.extensions.commandreplay.impl;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.apache.isis.extensions.commandlog.impl.IsisModuleExtCommandLogImpl;
import org.apache.isis.extensions.commandreplay.impl.executor.CommandExecutorServiceWithTime;
import org.apache.isis.extensions.commandreplay.impl.fetch.MasterConfiguration;
import org.apache.isis.extensions.commandreplay.impl.clock.TickingClockService;
import org.apache.isis.extensions.commandreplay.impl.analysis.CommandReplayAnalysisService;
import org.apache.isis.extensions.commandreplay.impl.ui.CommandReplayOnPrimaryService;
import org.apache.isis.extensions.commandreplay.impl.ui.CommandReplayOnSecondaryService;

@Configuration
@Import({
        // @Configuration's
        IsisModuleExtCommandLogImpl.class,

        // @DomainService's
        CommandExecutorServiceWithTime.class,
        CommandReplayAnalysisService.class,
        CommandReplayOnPrimaryService.class,
        CommandReplayOnSecondaryService.class,
        TickingClockService.class,

        // @Service's
        MasterConfiguration.class,

})
public class IsisModuleExtCommandReplayImpl {

    public abstract static class ActionDomainEvent<S>
        extends org.apache.isis.applib.events.domain.ActionDomainEvent<S> { }

    public abstract static class CollectionDomainEvent<S,T>
            extends org.apache.isis.applib.events.domain.CollectionDomainEvent<S,T> { }

    public abstract static class PropertyDomainEvent<S,T>
            extends org.apache.isis.applib.events.domain.PropertyDomainEvent<S,T> { }

}
