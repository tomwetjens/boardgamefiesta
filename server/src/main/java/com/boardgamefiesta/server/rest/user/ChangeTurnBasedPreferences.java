package com.boardgamefiesta.server.rest.user;

import lombok.Data;

@Data
public class ChangeTurnBasedPreferences {

    Boolean sendTurnEmail;
    Boolean sendEndedEmail;

}
