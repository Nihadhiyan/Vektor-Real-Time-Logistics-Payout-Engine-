package com.vektor.dispatch_engine.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

@IdGeneratorType(UuidV7Generator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD, METHOD})
public @interface UuidV7 {
    
}
