package com.wetjens.gwt.server.game.command;

import java.util.Set;

import lombok.Data;

@Data
public class CreateGameCommand {

    Set<String> inviteUserIds;

}
