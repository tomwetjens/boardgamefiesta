package com.boardgamefiesta.domain;

import javax.enterprise.inject.spi.CDI;

public interface DomainEvent {

    default void fire() {
        CDI.current().getBeanManager().fireEvent(this);
    }

}
