package com.boardgamefiesta.domain.event;

import com.boardgamefiesta.domain.user.User;

import java.time.Instant;

public interface WebSocketConnections {

    void add(WebSocketConnection webSocketConnection);

    void remove(String connectionId);

    void updateStatus(String id, User.Id userId, Instant updated, WebSocketConnection.Status status);

    boolean wasActiveAfter(User.Id userId, Instant since);
}
