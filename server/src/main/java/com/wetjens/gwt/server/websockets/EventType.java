package com.wetjens.gwt.server.websockets;

public enum EventType {
    STARTED,
    ENDED,
    INVITED,
    ACCEPTED,
    REJECTED,
    STATE_CHANGED,
    UNINVITED,
    LEFT,
    PROPOSED_TO_LEAVE,
    AGREED_TO_LEAVE,
    ABANDONED
}
