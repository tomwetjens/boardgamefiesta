package com.boardgamefiesta.server.ses;

import com.boardgamefiesta.server.domain.game.Games;
import com.boardgamefiesta.server.domain.table.Table;
import com.boardgamefiesta.server.domain.user.User;
import com.boardgamefiesta.server.domain.user.Users;
import lombok.NonNull;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

@ApplicationScoped
public class TurnBasedEmailer {

    private final Users users;
    private final Games games;
    private final Translations translations;
    private final String url;
    private final String from;
    private final SesClient sesClient;

    @Inject
    public TurnBasedEmailer(@NonNull Users users,
                            @NonNull Games games,
                            @NonNull Translations translations,
                            @ConfigProperty(name = "bgf.url") String url,
                            @ConfigProperty(name = "bgf.from") String from,
                            @NonNull SesClient sesClient) {
        this.users = users;
        this.games = games;
        this.translations = translations;
        this.url = url;
        this.from = from;
        this.sesClient = sesClient;
    }

    void beginTurn(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.BeginTurn event) {
        if (event.getType() != Table.Type.TURN_BASED) {
            return;
        }

        event.getUserId()
                .flatMap(users::findOptionallyById)
                .ifPresent(user -> sendEmail(event, user));
    }

    private void sendEmail(Table.BeginTurn event, User user) {
        var tableUrl = event.getGameId().getId() + "/" + event.getTableId().getId();

        var locale = user.getLocale();
        var timeZone = user.getTimeZone();

        var dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(timeZone);

        var game = translations.getTranslation("game." + event.getGameId().getId() + ".name", locale);
        var started = dateTimeFormatter.format(event.getStarted());
        var link = url + "/" + event.getGameId().getId() + "/" + event.getTableId().getId();

        sesClient.sendEmail(SendEmailRequest.builder()
                .source(from)
                .destination(Destination.builder()
                        .toAddresses(user.getEmail())
                        .build())
                .message(Message.builder()
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
                        .build())
                .build());
    }

}
