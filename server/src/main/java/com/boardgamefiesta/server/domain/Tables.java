package com.boardgamefiesta.server.domain;

import java.util.stream.Stream;

public interface Tables {

    Table findById(Table.Id id, boolean consistentRead);

    void add(Table table);

    void update(Table table) throws TableConcurrentlyModifiedException;

    Stream<Table> findActive(User.Id userId);

    Stream<Table> findAllByUserId(User.Id userId, int maxResults);

    class TableConcurrentlyModifiedException extends Exception {
        public TableConcurrentlyModifiedException(Throwable cause) {
            super(cause);
        }
    }

}
