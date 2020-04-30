package com.wetjens.gwt.server.rest;

import com.wetjens.gwt.server.domain.LogEntry;
import com.wetjens.gwt.server.domain.User;
import com.wetjens.gwt.server.rest.view.UserView;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Value
public class LogEntryView {

    Instant timestamp;
    UserView user;
    String type;
    List<Object> values;

    public LogEntryView(LogEntry logEntry, Map<User.Id, User> userMap) {
        this.timestamp = logEntry.getTimestamp();
        this.user = new UserView(logEntry.getUserId(), userMap.get(logEntry.getUserId()));
        this.type = logEntry.getType();
        this.values = logEntry.getValues();
    }

}
