package com.boardgamefiesta.server.rest.exception;

import lombok.Value;

@Value
public class Error {
    String errorCode;
    String gameId;
    String reasonCode;
}
