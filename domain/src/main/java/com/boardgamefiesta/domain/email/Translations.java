/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.domain.email;

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
