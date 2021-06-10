package com.boardgamefiesta.server.ses;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class DateTimeTool {

    private final DateTimeFormatter dateTimeFormatter;

    DateTimeTool(Locale locale, ZoneId zoneId) {
        dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(zoneId);
    }

    public String format(Instant instant) {
        return dateTimeFormatter.format(instant);
    }

}
