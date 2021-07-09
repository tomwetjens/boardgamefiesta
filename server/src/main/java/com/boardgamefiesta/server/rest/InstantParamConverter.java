package com.boardgamefiesta.server.rest;

import javax.ws.rs.ext.ParamConverter;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public class InstantParamConverter implements ParamConverter<Instant> {
    @Override
    public Instant fromString(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Value is not a valid ISO-8601 timestamp");
        }
    }

    @Override
    public String toString(Instant value) {
        return value.toString();
    }
}
