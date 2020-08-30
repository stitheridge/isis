package org.apache.isis.extensions.commandlog.impl.replay;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.apache.isis.extensions.commandlog.impl.IsisModuleExtCommandLogImpl;
import org.apache.isis.extensions.commandlog.impl.replay.impl.CommandExecutorServiceWithTime;
import org.apache.isis.extensions.commandlog.impl.replay.impl.SlaveConfiguration;
import org.apache.isis.extensions.commandlog.impl.replay.impl.TickingClockService;
import org.apache.isis.extensions.commandlog.impl.replay.impl.CommandReplayAnalysisService;
import org.apache.isis.extensions.commandlog.impl.replay.impl.CommandReplayOnMasterService;
import org.apache.isis.extensions.commandlog.impl.replay.impl.CommandReplayOnSlaveService;

@Configuration
@Import({
        // @Configuration's
        IsisModuleExtCommandLogImpl.class,

        // @DomainService's
        CommandExecutorServiceWithTime.class,
        CommandReplayAnalysisService.class,
        CommandReplayOnMasterService.class,
        CommandReplayOnSlaveService.class,
        TickingClockService.class,

        // @Service's
        SlaveConfiguration.class,

})
public class IsisModuleExtCommandReplayImpl {

    public abstract static class ActionDomainEvent<S>
        extends org.apache.isis.applib.events.domain.ActionDomainEvent<S> { }

    public abstract static class CollectionDomainEvent<S,T>
            extends org.apache.isis.applib.events.domain.CollectionDomainEvent<S,T> { }

    public abstract static class PropertyDomainEvent<S,T>
            extends org.apache.isis.applib.events.domain.PropertyDomainEvent<S,T> { }

}
