package com.boardgamefiesta.server.event.domain;

import com.boardgamefiesta.server.domain.user.User;

import java.time.Instant;

public interface WebSocketConnections {

    void add(WebSocketConnection webSocketConnection);

    void remove(String connectionId);

    void updateStatus(String id, Instant updated, WebSocketConnection.Status status);

    boolean wasActiveAfter(User.Id userId, Instant since);
}
