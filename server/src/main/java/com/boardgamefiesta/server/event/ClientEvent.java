package com.boardgamefiesta.server.event;

import lombok.Data;

@Data
public class ClientEvent {

    Type type;

    public enum Type {
        ACTIVE,
        INACTIVE
    }
}
