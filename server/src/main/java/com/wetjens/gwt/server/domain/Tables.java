package com.wetjens.gwt.server.domain;

import java.util.stream.Stream;

public interface Tables {

    static Tables instance() {
        return DomainService.instance(Tables.class);
    }

    Table findById(Table.Id id);

    void add(Table table);

    void update(Table table);

    Stream<Table> findByUserId(User.Id id);

    int countActiveRealtimeByUserId(User.Id id);
}
