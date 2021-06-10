package com.boardgamefiesta.server.rest.user;

import lombok.Data;

@Data
public class ChangeEmailPreferences {

    Boolean sendInviteEmail;

    ChangeTurnBasedPreferences turnBasedPreferences;

}
