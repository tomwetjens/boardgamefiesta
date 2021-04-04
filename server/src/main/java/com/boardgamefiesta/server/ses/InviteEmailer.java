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
public class InviteEmailer {

    private final Users users;
    private final WebSocketConnections webSocketConnections;
    private final EmailTemplates emailTemplates;
    private final Emailer emailer;

    @Inject
    public InviteEmailer(@NonNull Users users,
                         @NonNull WebSocketConnections webSocketConnections,
                         @NonNull EmailTemplates emailTemplates,
                         @NonNull Emailer emailer) {
        this.users = users;
        this.webSocketConnections = webSocketConnections;
        this.emailTemplates = emailTemplates;
        this.emailer = emailer;
    }

    void invited(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Invited event) {
        if (!webSocketConnections.wasActiveAfter(event.getUserId(), Instant.now().minusSeconds(60))) {
            users.findById(event.getUserId()).ifPresent(user ->
                    users.findById(event.getHostId()).ifPresent(host ->
                            sendEmail(event, user, host)));
        }
    }

    private void sendEmail(Table.Invited event, User user, User host) {
        emailer.sendEmailToUser(emailTemplates.createInvitedMessage(event, user, host), user);
    }

}
