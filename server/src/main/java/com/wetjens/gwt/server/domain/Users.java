package com.wetjens.gwt.server.domain;

public interface Users {

    User get(User.Id id);

    User findById(User.Id id);

}
