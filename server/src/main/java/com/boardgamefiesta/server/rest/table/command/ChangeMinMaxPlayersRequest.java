package com.boardgamefiesta.server.rest.table.command;

import com.boardgamefiesta.domain.table.Table;
import lombok.Data;

@Data
public class ChangeMinMaxPlayersRequest {
    int minNumberOfPlayers;
    int maxNumberOfPlayers;
}
