package com.wetjens.gwt.server.domain;

import javax.enterprise.inject.spi.CDI;

public interface DomainEvent {

    default void fire() {
        CDI.current().getBeanManager().fireEvent(this);
    }

}
