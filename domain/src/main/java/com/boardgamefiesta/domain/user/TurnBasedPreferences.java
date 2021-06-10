package com.boardgamefiesta.domain.user;

import lombok.*;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class TurnBasedPreferences {

    @Getter
    @Setter
    @Builder.Default
    boolean sendTurnEmail = true;

    @Getter
    @Setter
    @Builder.Default
    boolean sendEndedEmail = true;

}
