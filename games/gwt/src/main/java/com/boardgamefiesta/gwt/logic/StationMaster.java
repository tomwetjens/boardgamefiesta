/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

    static final List<StationMaster> SECOND_EDITION = List.of(
            TWO_PERM_CERTS,
            TWELVE_DOLLARS,
            PERM_CERT_POINTS_PER_2_STATIONS,
            GAIN_2_CERTS_POINTS_PER_BUILDING
    );

    static final List<StationMaster> RTTN = List.of(
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
