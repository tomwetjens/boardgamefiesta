package com.wetjens.gwt.server.rest;

import java.util.Map;
import java.util.Set;

import com.wetjens.gwt.server.domain.Game;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CreateGameRequest {

    @NotNull
    String name;

    @NotNull
    Game.Type type;

    @NotNull
    @Size(min = 1, max = 5)
    Set<String> inviteUserIds;

    @NotNull
    Map<String, String> options;

}
