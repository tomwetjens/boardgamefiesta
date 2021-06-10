package com.boardgamefiesta.server.rest.user.view;

import com.boardgamefiesta.domain.user.TurnBasedPreferences;
import lombok.Value;

@Value
public class TurnBasedPreferencesView {

    boolean sendTurnEmail;
    boolean sendEndedEmail;

    public TurnBasedPreferencesView(TurnBasedPreferences turnBasedPreferences) {
        sendTurnEmail = turnBasedPreferences.isSendTurnEmail();
        sendEndedEmail = turnBasedPreferences.isSendEndedEmail();
    }
}
