/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
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

import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import lombok.NonNull;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

@ApplicationScoped
public class InviteEmailer {

    private final Users users;
    private final EmailTemplates emailTemplates;
    private final SesEmailSender sender;

    @Inject
    public InviteEmailer(@NonNull Users users,
                         @NonNull EmailTemplates emailTemplates,
                         @NonNull SesEmailSender sender) {
        this.users = users;
        this.emailTemplates = emailTemplates;
        this.sender = sender;
    }

    void invited(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Invited event) {
        if (event.getType() == Table.Type.TURN_BASED) { // TODO Change this to send e-mail only when user is offline
            users.findById(event.getUserId())
                    .filter(user -> user.getEmailPreferences().isSendInviteEmail())
                    .ifPresent(user ->
                            users.findById(event.getHostId()).ifPresent(host ->
                                    sendEmail(event, user, host)));
        }
    }

    private void sendEmail(Table.Invited event, User user, User host) {
        sender.sendEmailToUser(emailTemplates.createInvitedMessage(event, user, host), user);
    }

}
