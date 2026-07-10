package com.vektor.dispatch_engine.utils;

import java.util.EnumSet;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;

import com.github.f4b6a3.uuid.UuidCreator;

public class UuidV7Generator implements BeforeExecutionGenerator{

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EventTypeSets.INSERT_ONLY;
    }

    @Override
    public Object generate(SharedSessionContractImplementor arg0, Object arg1, Object arg2, EventType arg3) {
        return UuidCreator.getTimeOrderedEpoch();
    }
    
}
