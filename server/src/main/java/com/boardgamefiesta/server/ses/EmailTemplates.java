package com.boardgamefiesta.server.ses;

import com.boardgamefiesta.server.domain.table.Table;
import com.boardgamefiesta.server.domain.user.User;
import lombok.NonNull;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Message;

import javax.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

@ApplicationScoped
public class EmailTemplates {

    private final Translations translations;
    private final String url;

    public EmailTemplates(@NonNull Translations translations,
                          @ConfigProperty(name = "bgf.url") String url) {
        this.translations = translations;
        this.url = url;
    }

    Message createBeginTurnMessage(Table.BeginTurn event, User user) {
        var locale = user.getLocale();
        var timeZone = user.getTimeZone();

        var dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(timeZone);

        var game = translations.getTranslation("game." + event.getGameId().getId() + ".name", locale);
        var started = dateTimeFormatter.format(event.getStarted());
        var link = url + "/" + event.getGameId().getId() + "/" + event.getTableId().getId();

        return Message.builder()
                .subject(Content.builder()
                        .charset(StandardCharsets.UTF_8.name())
                        .data(translations.getTranslation("email.turn.subject", locale, game, started))
                        .build())
                .body(Body.builder()
                        .html(Content.builder()
                                .charset(StandardCharsets.UTF_8.name())
                                .data(translations.getTranslation("email.turn.body", locale, game, started, link) +
                                        "Board Game Fiesta<br/><a href=\"" + url + "\">" + url + "</a>")
                                .build())
                        .build())
                .build();
    }

    Message createInvitedMessage(Table.Invited event, User user, User host) {
        var locale = user.getLocale();
        var timeZone = user.getTimeZone();

        var game = translations.getTranslation("game." + event.getGameId().getId() + ".name", locale);
        var link = url + "/" + event.getGameId().getId() + "/" + event.getTableId().getId();

        return Message.builder()
                .subject(Content.builder()
                        .charset(StandardCharsets.UTF_8.name())
                        .data(translations.getTranslation("email.invited.subject", locale, game, host.getUsername()))
                        .build())
                .body(Body.builder()
                        .html(Content.builder()
                                .charset(StandardCharsets.UTF_8.name())
                                .data(translations.getTranslation("email.invited.body", locale, game, host.getUsername(), link) +
                                        "Board Game Fiesta<br/><a href=\"" + url + "\">" + url + "</a>")
                                .build())
                        .build())
                .build();
    }
}
