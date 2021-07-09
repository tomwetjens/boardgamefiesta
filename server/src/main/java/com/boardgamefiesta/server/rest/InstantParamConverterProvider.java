package com.boardgamefiesta.server.rest;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Instant;

@SuppressWarnings("unchecked")
@Provider
public class InstantParamConverterProvider implements ParamConverterProvider {
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (Instant.class.equals(rawType)) {
            return (ParamConverter<T>) new InstantParamConverter();
        }
        return null;
    }
}
