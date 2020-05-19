package com.tomsboardgames.gwt;

import com.tomsboardgames.api.InGameEvent;
import com.tomsboardgames.api.Player;
import lombok.Value;

import java.util.List;

@Value
public class GWTEvent implements InGameEvent {

    Player player;
    String type;
    List<String> parameters;

    GWTEvent(Player player, Type type, List<String> parameters) {
        this.player = player;
        this.type = type.name();
        this.parameters = parameters;
    }

    public enum Type {
        SKIP,
        BEGIN_TURN,
        END_TURN,
        ACTION,
        PAY_FEE,
        MAY_DRAW_CATTLE_CARDS,
        MAY_APPOINT_STATION_MASTER,
        MUST_TAKE_OBJECTIVE_CARD,
        MAY_REMOVE_BLACK_DISC_INSTEAD_OF_WHITE,
        MUST_REMOVE_DISC_FROM_STATION,
        MAY_REMOVE_HAZARD_FOR_FREE,
        MAY_TRADE_WITH_INDIANS,
        MAY_PLACE_CHEAP_BUILDING,
        MAY_DISCARD_1_JERSEY_TO_GAIN_1_CERTIFICATE,
        MAY_DISCARD_1_JERSEY_TO_GAIN_2_DOLLARS,
        MAY_HIRE_CHEAP_WORKER,
        MAY_DISCARD_1_JERSEY_TO_GAIN_2_CERTIFICATES,
        GAINS_JOB_MARKET_TOKEN,
        EVERY_OTHER_PLAYER_HAS_1_TURN,
        ENDS_GAME,
        FILL_UP_CATTLE_MARKET,
        MAY_DISCARD_1_JERSEY_TO_GAIN_4_DOLLARS
    }
}
