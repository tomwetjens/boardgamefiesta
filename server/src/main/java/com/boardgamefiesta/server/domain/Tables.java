package com.boardgamefiesta.server.domain;

import java.util.stream.Stream;

public interface Tables {

    Table findById(Table.Id id);

    void add(Table table);

    void update(Table table) throws TableConcurrentlyModifiedException;

    Stream<Table> findActive(User.Id userId);

    class TableConcurrentlyModifiedException extends Exception {
        public TableConcurrentlyModifiedException(Throwable cause) {
            super(cause);
        }
    }

}
