package com.wetjens.gwt.server.rest;

import java.util.Set;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CreateGameRequest {

    int numberOfPlayers;

    @NotNull
    @Size(min = 1, max = 5)
    Set<String> inviteUserIds;

    boolean beginner;

}
