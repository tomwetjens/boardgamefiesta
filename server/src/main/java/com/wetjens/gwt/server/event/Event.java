package com.wetjens.gwt.server.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    private EventType type;
    private String tableId;
    private String userId;

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
}
