package com.boardgamefiesta.server.domain;

import javax.enterprise.inject.spi.CDI;

public interface DomainService {

    static <T> T instance(Class<T> clazz) {
        return CDI.current().select(clazz).get();
    }

}
