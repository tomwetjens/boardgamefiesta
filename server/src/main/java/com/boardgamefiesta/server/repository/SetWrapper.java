package com.boardgamefiesta.server.repository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import java.util.Set;

@RequiredArgsConstructor
class SetWrapper<T> implements Set<T> {
    @Delegate
    private final Set<T> actual;
}
