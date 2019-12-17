/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package demoapp.dom.events;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.events.domain.AbstractDomainEvent;
import org.apache.isis.applib.services.eventbus.EventBusService;
import org.apache.isis.applib.services.wrapper.WrapperFactory;

import static demoapp.utils.DemoUtils.emphasize;

import demoapp.dom.events.EventLogMenu.EventTestProgrammaticEvent;
import lombok.extern.log4j.Log4j2;

@DomainService // allows async invocation of the storeEvent(...) Action
@Service
@Named("demoapp.eventSubscriber")
@Qualifier("demo")
@Log4j2
public class EventSubscriber {

    @Inject private WrapperFactory wrapper;
    @Inject private EventBusService eventBusService;
    @Inject private EventLogRepository eventLogRepository;

    public static class EventSubscriberEvent extends AbstractDomainEvent<Object> {}

    @PostConstruct
    public void init() {
        log.info(emphasize("EventSubscriber - PostConstruct"));
        eventBusService.post(new EventSubscriberEvent());
    }

    @EventListener(EventTestProgrammaticEvent.class)
    public void on(EventTestProgrammaticEvent ev) {

        if(ev.getEventPhase() != null && !ev.getEventPhase().isExecuted()) {
            return;
        }

        log.info(emphasize("DomainEvent: "+ev.getClass().getName()));
        
        // store in event log, by calling the storeEvent(...) Action
        wrapper.async(this)
        .run(EventSubscriber::storeEvent, ev);

    }

    
    @Action // allows async invocation 
    public void storeEvent(EventTestProgrammaticEvent ev) {
        eventLogRepository.add(EventLogEntry.of(ev));
    }

}
