package com.wetjens.gwt;

import java.util.function.Function;
import java.util.function.Supplier;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum StationMaster {

    GAIN_2_DOLLARS_POINT_FOR_EACH_WORKER(() -> ImmediateActions.of(PossibleAction.optional(Action.Gain2Dollars.class)), playerState -> playerState.getNumberOfCowboys() + playerState.getNumberOfCraftsmen() + playerState.getNumberOfEngineers()),
    REMOVE_HAZARD_OR_TEEPEE_POINTS_FOR_EACH_2_OBJECTIVE_CARDS(() -> ImmediateActions.of(PossibleAction.optional(
            PossibleAction.choice(Action.TradeWithIndians.class, Action.RemoveHazardForFree.class))), playerState -> (playerState.numberOfObjectiveCards() / 2) * 3),
    PERM_CERT_POINTS_FOR_EACH_2_HAZARDS(ImmediateActions::none, playerState -> (playerState.getHazards().size() / 2) * 3),
    PERM_CERT_POINTS_FOR_TEEPEE_PAIRS(ImmediateActions::none, playerState -> playerState.numberOfTeepeePairs() * 3),
    PERM_CERT_POINTS_FOR_EACH_2_CERTS(ImmediateActions::none, playerState -> ((playerState.getCertificates() + playerState.permanentCertificates()) / 2) * 3);

    private final Supplier<ImmediateActions> immediateActionsSupplier;
    private final Function<PlayerState, Integer> scoreFunction;

    ImmediateActions getImmediateActions() {
        return immediateActionsSupplier.get();
    }

    public int score(PlayerState playerState) {
        return scoreFunction.apply(playerState);
    }
}
