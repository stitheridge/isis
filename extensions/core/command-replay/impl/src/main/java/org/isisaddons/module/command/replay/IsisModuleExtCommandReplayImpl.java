package org.isisaddons.module.command.replay;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.isisaddons.module.command.IsisModuleExtCommandLogImpl;
import org.isisaddons.module.command.replay.impl.CommandExecutorServiceWithTime;
import org.isisaddons.module.command.replay.impl.CommandReplayAnalysisService;
import org.isisaddons.module.command.replay.impl.CommandReplayOnMasterService;
import org.isisaddons.module.command.replay.impl.CommandReplayOnSlaveService;
import org.isisaddons.module.command.replay.impl.SlaveConfiguration;
import org.isisaddons.module.command.replay.impl.TickingClockService;

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
