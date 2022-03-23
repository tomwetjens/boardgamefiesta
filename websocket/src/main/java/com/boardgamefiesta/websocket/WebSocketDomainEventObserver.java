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

package com.boardgamefiesta.websocket;

import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.domain.user.Friend;
import com.boardgamefiesta.domain.user.User;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ApplicationScoped
@Slf4j
class WebSocketDomainEventObserver {

    @Inject
    Tables tables;

    @Inject
    @Any
    Instance<WebSocketSender> senders;

    void accepted(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Accepted event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.ACCEPTED, event.getTableId().getId(), event.getUserId().getId()));

        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(event.getUserId(), table, new WebSocketServerEvent(WebSocketServerEvent.Type.ACCEPTED, event.getTableId().getId(), event.getUserId().getId())));
    }

    void rejected(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Rejected event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.REJECTED, event.getTableId().getId(), event.getUserId().getId()));

        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(event.getUserId(), table, new WebSocketServerEvent(WebSocketServerEvent.Type.REJECTED, event.getTableId().getId(), event.getUserId().getId())));
    }

    void started(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Started event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.STARTED, event.getTableId().getId(), null));

        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(null, table, new WebSocketServerEvent(WebSocketServerEvent.Type.STARTED, event.getTableId().getId(), null)));
    }

    void ended(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Ended event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.ENDED, event.getTableId().getId(), null));

        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(null, table, new WebSocketServerEvent(WebSocketServerEvent.Type.ENDED, event.getTableId().getId(), null)));
    }

    void stateChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.StateChanged event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.STATE_CHANGED, event.getTableId().getId(), null));

        var table = event.getTable().get();
        notifyOtherPlayers(null, table, new WebSocketServerEvent(WebSocketServerEvent.Type.STATE_CHANGED, event.getTableId().getId(), null));
    }

    void invited(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Invited event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.INVITED, event.getTableId().getId(), event.getUserId().getId()));

        tables.findById(event.getTableId()).ifPresent(table -> {
            notifyUser(event.getUserId(), new WebSocketServerEvent(WebSocketServerEvent.Type.INVITED, event.getTableId().getId(), event.getUserId().getId()));
            notifyOtherPlayers(event.getUserId(), table, new WebSocketServerEvent(WebSocketServerEvent.Type.INVITED, table.getId().getId(), event.getUserId().getId()));
        });
    }

    void uninvited(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Kicked event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.UNINVITED, event.getTableId().getId(), event.getUserId().getId()));

        tables.findById(event.getTableId()).ifPresent(table -> {
            notifyUser(event.getUserId(), new WebSocketServerEvent(WebSocketServerEvent.Type.UNINVITED, event.getTableId().getId(), event.getUserId().getId()));
            notifyOtherPlayers(event.getUserId(), table, new WebSocketServerEvent(WebSocketServerEvent.Type.UNINVITED, table.getId().getId(), event.getUserId().getId()));
        });
    }

    void joined(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Joined event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.JOINED, event.getTableId().getId(), event.getUserId().getId()));
    }

    void visibilityChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.VisibilityChanged event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.VISIBILITY_CHANGED, event.getTableId().getId(), null));
    }

    void left(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Left event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.LEFT, event.getTableId().getId(), event.getUserId().getId()));

        tables.findById(event.getTableId()).ifPresent(table -> {
            notifyUser(event.getUserId(), new WebSocketServerEvent(WebSocketServerEvent.Type.LEFT, event.getTableId().getId(), event.getUserId().getId()));
            notifyOtherPlayers(event.getUserId(), table, new WebSocketServerEvent(WebSocketServerEvent.Type.LEFT, event.getTableId().getId(), event.getUserId().getId()));
        });
    }

    void abandoned(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Abandoned event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.ABANDONED, event.getTableId().getId(), null));

        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(null, table, new WebSocketServerEvent(WebSocketServerEvent.Type.ABANDONED, event.getTableId().getId(), null)));
    }

    void proposedToLeave(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.ProposedToLeave event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.PROPOSED_TO_LEAVE, event.getTableId().getId(), event.getUserId().getId()));
    }

    void agreedToLeave(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.AgreedToLeave event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.AGREED_TO_LEAVE, event.getTableId().getId(), event.getUserId().getId()));
    }

    void kicked(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.Kicked event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.KICKED, event.getTableId().getId(), event.getUserId().getId()));

        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(null, table, new WebSocketServerEvent(WebSocketServerEvent.Type.KICKED, event.getTableId().getId(), event.getUserId().getId())));
    }

    void optionsChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.OptionsChanged event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.OPTIONS_CHANGED, event.getTableId().getId(), null));

        // TODO Is this actually needed?
        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(null, table, new WebSocketServerEvent(WebSocketServerEvent.Type.OPTIONS_CHANGED, event.getTableId().getId(), null)));
    }

    void computerAdded(@Observes(during = TransactionPhase.AFTER_SUCCESS) Table.ComputerAdded event) {
        notifyTable(event.getTableId(), new WebSocketServerEvent(WebSocketServerEvent.Type.COMPUTER_ADDED, event.getTableId().getId(), null));

        // TODO Is this actually needed?
        tables.findById(event.getTableId()).ifPresent(table ->
                notifyOtherPlayers(null, table, new WebSocketServerEvent(WebSocketServerEvent.Type.COMPUTER_ADDED, event.getTableId().getId(), null)));
    }

    void addedAsFriend(@Observes(during = TransactionPhase.AFTER_SUCCESS) Friend.Started event) {
        notifyUser(event.getId().getOtherUserId(), new WebSocketServerEvent(WebSocketServerEvent.Type.ADDED_AS_FRIEND, null, event.getId().getUserId().getId()));
    }

    private void notifyTable(Table.Id tableId, WebSocketServerEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Notifying table {} through {} senders of: {}", tableId.getId(), senders.stream().count(), event);
        }
        senders.forEach(sender -> {
            log.debug("Notifying table {} through sender {} of: {}", tableId.getId(), sender, event);
            sender.sendToTable(tableId, event);
        });
    }

    private void notifyUser(User.Id userId, WebSocketServerEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Notifying user {} through {} senders of: {}", userId.getId(), senders.stream().count(), event);
        }
        senders.forEach(sender -> {
            log.debug("Notifying user {} through sender {} of: {}", userId.getId(), sender, event);
            sender.sendToUser(userId, event);
        });
    }

    private void notifyOtherPlayers(User.Id currentUserId, Table table, WebSocketServerEvent event) {
        table.getPlayers().stream()
                .filter(player -> player.getType() == Player.Type.USER)
                .flatMap(player -> player.getUserId().stream())
                .filter(userId -> !userId.equals(currentUserId))
                .forEach(userId -> notifyUser(userId, event));
    }

}
