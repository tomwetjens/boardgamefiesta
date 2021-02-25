package com.boardgamefiesta.server.ses;

import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.server.event.domain.WebSocketConnections;
import lombok.NonNull;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import java.time.Instant;

@ApplicationScoped
public class TurnBasedEmailer {

    private final Users users;
    private final WebSocketConnections webSocketConnections;
    private final EmailTemplates emailTemplates;
    private final Emailer emailer;

    @Inject
    public TurnBasedEmailer(@NonNull Users users,
                            @NonNull WebSocketConnections webSocketConnections,
                            @NonNull EmailTemplates emailTemplates,
                            @NonNull Emailer emailer) {
        this.users = users;
        this.webSocketConnections = webSocketConnections;
        this.emailTemplates = emailTemplates;
        this.emailer = emailer;
    }

    void beginTurn(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.BeginTurn event) {
        if (event.getType() != Table.Type.TURN_BASED) {
            return;
        }

        event.getUserId()
                .filter(userId -> !webSocketConnections.wasActiveAfter(userId, Instant.now().minusSeconds(60)))
                .flatMap(users::findOptionallyById)
                .ifPresent(user -> sendEmail(event, user));
    }

    private void sendEmail(Table.BeginTurn event, User user) {
        emailer.sendEmailToUser(emailTemplates.createBeginTurnMessage(event, user), user);
    }

}
