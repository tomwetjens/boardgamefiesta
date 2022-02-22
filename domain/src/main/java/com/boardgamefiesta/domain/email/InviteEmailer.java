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

import com.boardgamefiesta.domain.email.EmailTemplates;
import com.boardgamefiesta.domain.email.Emailer;
import com.boardgamefiesta.domain.event.WebSocketConnections;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
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
            users.findById(event.getUserId())
                    .filter(user -> user.getEmailPreferences().isSendInviteEmail())
                    .ifPresent(user ->
                            users.findById(event.getHostId()).ifPresent(host ->
                                    sendEmail(event, user, host)));
        }
    }

    private void sendEmail(Table.Invited event, User user, User host) {
        emailer.sendEmailToUser(emailTemplates.createInvitedMessage(event, user, host), user);
    }

}
