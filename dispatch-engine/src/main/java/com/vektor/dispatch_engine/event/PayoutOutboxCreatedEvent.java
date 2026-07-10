package com.vektor.dispatch_engine.event;

import org.springframework.context.ApplicationEvent;

public class PayoutOutboxCreatedEvent extends ApplicationEvent {

    public PayoutOutboxCreatedEvent(Object source) {
        super(source);
    }
}
