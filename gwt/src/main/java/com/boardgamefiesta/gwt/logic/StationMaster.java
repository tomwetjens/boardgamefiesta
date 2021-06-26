package com.boardgamefiesta.gwt.logic;

import lombok.AllArgsConstructor;

import java.util.List;
import java.util.function.Function;

@AllArgsConstructor
public enum StationMaster {

    GAIN_2_DOLLARS_POINT_FOR_EACH_WORKER(game -> ImmediateActions.of(PossibleAction.optional(Action.Gain2Dollars.class))),
    REMOVE_HAZARD_OR_TEEPEE_POINTS_FOR_EACH_2_OBJECTIVE_CARDS(game -> ImmediateActions.of(PossibleAction.optional(PossibleAction.choice(Action.TradeWithTribes.class, Action.RemoveHazardForFree.class)))),
    PERM_CERT_POINTS_FOR_EACH_2_HAZARDS(game -> ImmediateActions.none()),
    PERM_CERT_POINTS_FOR_TEEPEE_PAIRS(game -> ImmediateActions.none()),
    PERM_CERT_POINTS_FOR_EACH_2_CERTS(game -> ImmediateActions.none()),

    // Promo tiles / Rails To The North:
    TWO_PERM_CERTS(game -> ImmediateActions.none()),
    TWELVE_DOLLARS(game -> ImmediateActions.of(PossibleAction.optional(Action.Gain12Dollars.class))),

    // Rails To The North:
    PERM_CERT_POINTS_PER_2_STATIONS(game -> ImmediateActions.none()),
    GAIN_2_CERTS_POINTS_PER_BUILDING(game -> ImmediateActions.of(PossibleAction.optional(Action.Gain2Certificates.class))),
    PLACE_BRANCHLET_POINTS_PER_2_EXCHANGE_TOKENS(game -> ImmediateActions.of(PossibleAction.optional(Action.PlaceBranchlet.class))),
    GAIN_EXCHANGE_TOKEN_POINTS_PER_AREA(game -> ImmediateActions.of(PossibleAction.optional(Action.GainExchangeToken.class)));

    static final List<StationMaster> ORIGINAL = List.of(
            GAIN_2_DOLLARS_POINT_FOR_EACH_WORKER,
            REMOVE_HAZARD_OR_TEEPEE_POINTS_FOR_EACH_2_OBJECTIVE_CARDS,
            PERM_CERT_POINTS_FOR_EACH_2_HAZARDS,
            PERM_CERT_POINTS_FOR_TEEPEE_PAIRS,
            PERM_CERT_POINTS_FOR_EACH_2_CERTS);

    static final List<StationMaster> PROMOS = List.of(
            TWO_PERM_CERTS,
            TWELVE_DOLLARS
    );

    static final List<StationMaster> RTTN_AND_2ND_EDITION = List.of(
            TWO_PERM_CERTS,
            TWELVE_DOLLARS,
            PERM_CERT_POINTS_PER_2_STATIONS,
            GAIN_2_CERTS_POINTS_PER_BUILDING,
            PLACE_BRANCHLET_POINTS_PER_2_EXCHANGE_TOKENS,
            GAIN_EXCHANGE_TOKEN_POINTS_PER_AREA
    );

    private final Function<GWT, ImmediateActions> activateFunction;

    ImmediateActions activate(GWT state) {
        return activateFunction.apply(state);
    }
}
