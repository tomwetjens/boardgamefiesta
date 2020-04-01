package com.wetjens.gwt.server;

import com.wetjens.gwt.Action;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ActionView {

    CHOOSE_FORESIGHTS(Action.ChooseForesights.class),
    MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS(Action.MoveEngine2BackwardsToRemove2Cards.class),
    PAY_2_TO_MOVE_ENGINE_2_FORWARD(Action.Pay2DollarsToMoveEngine2Forward.class),
    DRAW_2_CATTLE_CARDS(Action.Draw2CattleCards.class),
    DRAW_UP_TO_1_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo1CardsThenDiscardCards.class),
    DRAW_UP_TO_2_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo2CardsThenDiscardCards.class),
    DRAW_UP_TO_3_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo3CardsThenDiscardCards.class),
    DRAW_UP_TO_4_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo4CardsThenDiscardCards.class),
    DRAW_UP_TO_5_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo5CardsThenDiscardCards.class),
    DRAW_UP_TO_6_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo6CardsThenDiscardCards.class),
    GAIN_1_DOLLAR(Action.Gain1Dollars.class),
    DISCARD_1_DUTCH_BELT(Action.Discard1DutchBeltToGain2Dollars.class),
    DISCARD_1_CARD(Action.DiscardCards.Discard1Card.class),
    DISCARD_2_CARDS(Action.DiscardCards.Discard2Cards.class),
    DISCARD_3_CARDS(Action.DiscardCards.Discard3Cards.class),
    DISCARD_PAIR_TO_GAIN_4_DOLLARS(Action.DiscardPairToGain4Dollars.class),
    DRAW_1_CARD_THEN_DISCARD_1_CARD(Action.DrawCardsThenDiscardCards.Draw1CardThenDiscard1Card.class),
    DRAW_2_CARDS_THEN_DISCARD_2_CARDS(Action.DrawCardsThenDiscardCards.Draw2CardsThenDiscard2Cards.class),
    PAY_1_DOLLAR_TO_MOVE_ENGINE_1_FORWARD(Action.Pay1DollarToMoveEngine1Forward.class),
    DISCARD_1_JERSEY_TO_GAIN_2_DOLLARS(Action.Discard1JerseyToGain2Dollars.class),
    REMOVE_2_CARDS(Action.Remove2Cards.class),
    DELIVER_TO_CITY(Action.DeliverToCity.class),
    SINGLE_AUX_ACTION(Action.SingleAuxiliaryAction.class),
    SINGLE_OR_DOUBLE_AUX_ACTION(Action.SingleOrDoubleAuxiliaryAction.class),
    PLACE_BUILDING_FOR_2_DOLLARS_PER_CRAFTSMAN(Action.PlaceBuilding.class),
    DISCARD_1_GUERNSEY(Action.Discard1Guernsey.class),
    MOVE_ENGINE_2_OR_3_FORWARD(Action.MoveEngine2Or3Forward.class),
    REMOVE_HAZARD_FOR_FREE(Action.RemoveHazardForFree.class),
    PAY_2_DOLLARS_AND_MOVE_ENGINE_2_BACKWARDS_TO_GAIN_2_CERTS(Action.Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates.class),
    GAIN_OBJECTIVE_CARD(Action.GainObjectiveCard.class),
    UPGRADE_STATION(Action.UpgradeStation.class),
    APPOINT_STATION_MASTER(Action.AppointStationMaster.class),
    PLAY_OBJECTIVE_CARD(Action.PlayObjectiveCard.class),
    DISCARD_1_JERSEY_TO_GAIN_2_CERTS(Action.Discard1JerseyToGain2Certificates.class),
    REMOVE_HAZARD(Action.RemoveHazard.class),
    PAY_1_DOLLAR_AND_MOVE_ENGINE_1_BACKWARDS_TO_GAIN_1_CERT(Action.Pay1DollarAndMoveEngine1BackwardsToGain1Certificate.class),
    MOVE(Action.Move.class),
    REMOVE_1_CARD(Action.Remove1Card.class),
    MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD(Action.MoveEngine1BackwardsToRemove1Card.class),
    HIRE_WORKER_DISCOUNT(Action.HireCheapWorker.class),
    HIRE_WORKER(Action.HireWorker.class),
    HIRE_SECOND_WORKER(Action.HireSecondWorker.class),
    DISCARD_1_BLACK_ANGUS_TO_GAIN_2_DOLLARS(Action.Discard1BlackAngusToGain2Dollars.class),
    GAIN_2_DOLLARS(Action.Gain2Dollars.class),
    BUY_CATTLE(Action.BuyCattle.class),
    TRADE_WITH_INDIANS(Action.TradeWithIndians.class),
    DISCARD_1_JERSEY_TO_GAIN_1_CERT(Action.Discard1JerseyToGain1Certificate.class),
    DISCARD_1_JERSEY_TO_GAIN_4_DOLLARS(Action.Discard1JerseyToGain4Dollars.class),
    MOVE_ENGINE_FORWARD(Action.MoveEngineForward.class),
    PLACE_BUILDING_FOR_1_DOLLAR_PER_CRAFTSMAN(Action.PlaceCheapBuilding.class),
    GAIN_1_CERT(Action.GainCertificate.class),
    PAY_2_DOLLARS_TO_MOVE_ENGINE_2_FORWARD(Action.Pay2DollarsToMoveEngine2Forward.class),
    MOVE_ENGINE_AT_LEAST_1_BACKWARDS_AND_GAIN_3_DOLLARS(Action.MoveEngineAtLeast1BackwardsAndGain3Dollars.class);

    Class<? extends Action> action;

    static ActionView of(Class<? extends Action> action) {
        for (ActionView value : values()) {
            if (value.action.equals(action)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported action: " + action);
    }
}
