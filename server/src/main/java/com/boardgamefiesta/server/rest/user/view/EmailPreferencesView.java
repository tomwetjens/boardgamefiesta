package com.boardgamefiesta.server.rest.user.view;

import com.boardgamefiesta.domain.user.EmailPreferences;
import lombok.Value;

@Value
public class EmailPreferencesView {

    boolean sendInviteEmail;

    TurnBasedPreferencesView turnBasedPreferences;

    public EmailPreferencesView(EmailPreferences emailPreferences) {
        sendInviteEmail = emailPreferences.isSendInviteEmail();
        turnBasedPreferences = new TurnBasedPreferencesView(emailPreferences.getTurnBasedPreferences());
    }
}
