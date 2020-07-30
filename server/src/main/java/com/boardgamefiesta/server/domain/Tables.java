package com.boardgamefiesta.server.domain;

import java.util.stream.Stream;

public interface Tables {

    static Tables instance() {
        return DomainService.instance(Tables.class);
    }

    Table findById(Table.Id id);

    void add(Table table);

    void update(Table table) throws TableConcurrentlyModifiedException;

    Stream<Table> findByUserId(User.Id id);

    class TableConcurrentlyModifiedException extends Exception {
        public TableConcurrentlyModifiedException(Throwable cause) {
            super(cause);
        }
    }

}
