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

import java.util.stream.Stream;

public abstract class NeutralBuilding extends Building {

    NeutralBuilding(String name) {
        super(name, Hand.NONE);
    }

    public static NeutralBuilding forName(String name) {
        switch (name) {
            case "A":
                return new NeutralBuilding.A();
            case "B":
                return new NeutralBuilding.B();
            case "C":
                return new NeutralBuilding.C();
            case "D":
                return new NeutralBuilding.D();
            case "E":
                return new NeutralBuilding.E();
            case "F":
                return new NeutralBuilding.F();
            case "G":
                return new NeutralBuilding.G();
            default:
                throw new IllegalArgumentException("Unsupported neutral building: " + name);
        }
    }

    public static final class A extends NeutralBuilding {

        A() {
            super("A");
        }

        @Override
        public PossibleAction getPossibleAction(GWT game) {
            return PossibleAction.any(Action.Discard1Guernsey.class, Action.HireWorker.class, Action.HireWorkerPlus2.class);
        }
    }

    public static final class B extends NeutralBuilding {

        B() {
            super("B");
        }

        @Override
        public PossibleAction getPossibleAction(GWT game) {
            return PossibleAction.any(Action.Discard1DutchBeltToGain2Dollars.class, Action.PlaceBuilding.class);
        }
    }

    public static final class C extends NeutralBuilding {

        C() {
            super("C");
        }

        @Override
        public PossibleAction getPossibleAction(GWT game) {
            return PossibleAction.any(
                    PossibleAction.choice(Action.Gain1Certificate.class, Action.TakeObjectiveCard.class),
                    Action.MoveEngineForward.class);
        }
    }

    public static final class D extends NeutralBuilding {

        D() {
            super("D");
        }

        @Override
        public PossibleAction getPossibleAction(GWT game) {
            return PossibleAction.any(
                    PossibleAction.choice(Action.TradeWithTribes.class, Action.Pay2DollarsToMoveEngine2Forward.class),
                    Action.SingleOrDoubleAuxiliaryAction.class);
        }
    }

    public static final class E extends NeutralBuilding {

        E() {
            super("E");
        }

        @Override
        public PossibleAction getPossibleAction(GWT game) {
            return PossibleAction.any(Stream.of(
                    PossibleAction.optional(Action.Discard1BlackAngusToGain2Dollars.class),
                    PossibleAction.repeat(0, game.currentPlayerState().getNumberOfCowboys(), Action.BuyCattle.class),
                    PossibleAction.repeat(0, game.currentPlayerState().getNumberOfCowboys(), Action.Draw2CattleCards.class)));
        }

    }

    public static final class F extends NeutralBuilding {

        F() {
            super("F");
        }

        @Override
        public PossibleAction getPossibleAction(GWT game) {
            return PossibleAction.any(Action.DiscardPairToGain4Dollars.class, Action.RemoveHazard.class);
        }
    }

    public static final class G extends NeutralBuilding {

        G() {
            super("G");
        }

        @Override
        public PossibleAction getPossibleAction(GWT game) {
            return PossibleAction.any(Action.MoveEngineForward.class, Action.SingleOrDoubleAuxiliaryAction.class);
        }
    }
}
