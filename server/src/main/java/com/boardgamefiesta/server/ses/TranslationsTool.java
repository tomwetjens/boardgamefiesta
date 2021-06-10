package com.boardgamefiesta.server.ses;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@RequiredArgsConstructor
public class TranslationsTool {

    private final Translations translations;
    @NonNull
    private final Locale locale;

    public String get(String key, Object... arguments) {
        return translations.getTranslation(key, locale, arguments);
    }
}
