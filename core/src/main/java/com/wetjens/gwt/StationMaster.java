package com.wetjens.gwt;

import java.util.function.Supplier;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum StationMaster {

    GAIN_2_DOLLARS_POINT_FOR_EACH_WORKER(() -> ImmediateActions.of(PossibleAction.optional(Action.Gain2Dollars.class))),
    REMOVE_HAZARD_OR_TEEPEE_POINTS_FOR_EACH_2_OBJECTIVE_CARDS(() -> ImmediateActions.of(PossibleAction.optional(
            PossibleAction.choice(Action.TradeWithIndians.class, Action.RemoveHazardForFree.class)))),
    PERM_CERT_POINTS_FOR_EACH_2_HAZARDS(ImmediateActions::none),
    PERM_CERT_POINTS_FOR_TEEPEE_PAIRS(ImmediateActions::none),
    PERM_CERT_POINTS_FOR_EACH_2_CERTS(ImmediateActions::none);

    private final Supplier<ImmediateActions> immediateActionsSupplier;

    ImmediateActions getImmediateActions() {
        return immediateActionsSupplier.get();
    }
}
