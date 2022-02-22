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

import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import com.boardgamefiesta.domain.event.WebSocketConnections;
import lombok.NonNull;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class TurnBasedEmailer {

    private final Tables tables;
    private final Users users;
    private final WebSocketConnections webSocketConnections;
    private final EmailTemplates emailTemplates;
    private final Emailer emailer;

    @Inject
    public TurnBasedEmailer(@NonNull Tables tables,
                            @NonNull Users users,
                            @NonNull WebSocketConnections webSocketConnections,
                            @NonNull EmailTemplates emailTemplates,
                            @NonNull Emailer emailer) {
        this.tables = tables;
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
                .filter(this::isInactive)
                .flatMap(users::findById)
                .filter(user -> user.getEmailPreferences().getTurnBasedPreferences().isSendTurnEmail())
                .ifPresent(user -> sendEmail(event, user));
    }

    void ended(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Ended event) {
        tables.findById(event.getTableId())
                .filter(table -> table.getType() == Table.Type.TURN_BASED)
                .ifPresent(table -> {
                    var humanPlayers = table.getPlayers().stream()
                            .filter(Player::isPlaying)
                            .filter(Player::isUser)
                            .collect(Collectors.toList());

                    if (humanPlayers.size() > 1) {
                        var userMap = humanPlayers.stream()
                                .flatMap(player -> player.getUserId().stream())
                                .flatMap(userId -> users.findById(userId).stream())
                                .collect(Collectors.toMap(User::getId, Function.identity()));

                        humanPlayers.stream()
                                .filter(player -> isInactive(player.getUserId().get()))
                                .forEach(player -> users.findById(player.getUserId().get())
                                        .filter(user -> user.getEmailPreferences().getTurnBasedPreferences().isSendEndedEmail())
                                        .ifPresent(user -> sendEmail(table, player, userMap)));
                    }
                });
    }

    private void sendEmail(Table table, Player player, Map<User.Id, User> userMap) {
        emailer.sendEmailToUser(emailTemplates.createEndedMessage(table, player, userMap), userMap.get(player.getUserId().get()));
    }

    private void sendEmail(Table.BeginTurn event, User user) {
        emailer.sendEmailToUser(emailTemplates.createBeginTurnMessage(event, user), user);
    }

    private boolean isInactive(User.Id userId) {
        return !webSocketConnections.wasActiveAfter(userId, Instant.now().minusSeconds(60));
    }
}
