package com.boardgamefiesta.domain.user;

import lombok.*;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class EmailPreferences {

    @Getter
    @Setter
    @Builder.Default
    boolean sendInviteEmail = true;

    @Getter
    @NonNull
    @Builder.Default
    private final TurnBasedPreferences turnBasedPreferences = new TurnBasedPreferences();

}
