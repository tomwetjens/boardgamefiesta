package com.wetjens.gwt.server.repository;

import com.wetjens.gwt.api.Implementation;
import com.wetjens.gwt.server.domain.Implementations;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ImplementationRepository implements Implementations {

    public ImplementationRepository() {
        // TODO Discover implementations
    }

    @Override
    public Implementation get(String name) {
        // TODO Implement
        return null;
    }
}
