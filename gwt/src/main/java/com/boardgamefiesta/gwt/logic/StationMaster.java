package com.boardgamefiesta.gwt.logic;

import lombok.AllArgsConstructor;

import java.util.function.Function;

@AllArgsConstructor
public enum StationMaster {

    GAIN_2_DOLLARS_POINT_FOR_EACH_WORKER(game -> ImmediateActions.of(PossibleAction.optional(Action.Gain2Dollars.class)), playerState -> playerState.getNumberOfCowboys() + playerState.getNumberOfCraftsmen() + playerState.getNumberOfEngineers()),
    REMOVE_HAZARD_OR_TEEPEE_POINTS_FOR_EACH_2_OBJECTIVE_CARDS(game -> ImmediateActions.of(PossibleAction.optional(
            PossibleAction.choice(Action.TradeWithTribes.class, Action.RemoveHazardForFree.class))), playerState -> (playerState.getCommittedObjectives().size() / 2) * 3),
    PERM_CERT_POINTS_FOR_EACH_2_HAZARDS(game -> ImmediateActions.none(), playerState -> (playerState.getHazards().size() / 2) * 3),
    PERM_CERT_POINTS_FOR_TEEPEE_PAIRS(game -> ImmediateActions.none(), playerState -> playerState.numberOfTeepeePairs() * 3),
    PERM_CERT_POINTS_FOR_EACH_2_CERTS(game -> ImmediateActions.none(), playerState -> ((playerState.getTempCertificates() + playerState.permanentCertificates()) / 2) * 3);

    private final Function<Game, ImmediateActions> activateFunction;
    private final Function<PlayerState, Integer> scoreFunction;

    ImmediateActions activate(Game state) {
        return activateFunction.apply(state);
    }

    public int score(PlayerState playerState) {
        return scoreFunction.apply(playerState);
    }
}
