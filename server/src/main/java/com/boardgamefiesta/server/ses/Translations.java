package com.boardgamefiesta.server.ses;

import lombok.Value;

import javax.enterprise.context.ApplicationScoped;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class Translations {

    private static final String BASE_NAME = "messages";

    private static final ResourceBundle defaultBundle = ResourceBundle.getBundle(BASE_NAME, Locale.ENGLISH);
    private static final Map<Locale, CachedBundle> bundles = new ConcurrentHashMap<>();

    static {
        // Must set this to reliably fallback to English when no translation is found for a given locale
        Locale.setDefault(Locale.ENGLISH);
    }

    private ResourceBundle getBundle(Locale locale) {
        var bundle = bundles.computeIfAbsent(locale, l -> new CachedBundle(ResourceBundle.getBundle(BASE_NAME, locale)));
        return bundle.getBundle() != null ? bundle.getBundle() : defaultBundle;
    }

    public String getTranslation(String key, Locale locale, Object... arguments) {
        try {
            var pattern = getBundle(locale).getString(key);
            var messageFormat = new MessageFormat(pattern, locale);

            var result = new StringBuffer();
            messageFormat.format(arguments, result, null);

            return result.toString();
        } catch (MissingResourceException e) {
            return key;
        }
    }

    @Value
    private static class CachedBundle {
        ResourceBundle bundle;
    }

}
