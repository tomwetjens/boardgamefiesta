package com.wetjens.gwt.server.game.view;

import com.wetjens.gwt.Action;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ActionView {

    APPOINT_STATION_MASTER(Action.AppointStationMaster.class),
    BUY_CATTLE(Action.BuyCattle.class),
    DELIVER_TO_CITY(Action.DeliverToCity.class),
    DISCARD_1_BLACK_ANGUS_TO_GAIN_2_CERTIFICATES(Action.Discard1BlackAngusToGain2Certificates.class),
    DISCARD_1_BLACK_ANGUS_TO_GAIN_2_DOLLARS(Action.Discard1BlackAngusToGain2Dollars.class),
    DISCARD_1_CARD(Action.DiscardCards.Discard1Card.class),
    DISCARD_1_CATTLE_CARD_TO_GAIN_3_DOLLARS_AND_ADD_1_OBJECTIVE_CARD_TO_HAND(Action.Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand.class),
    DISCARD_1_DUTCH_BELT_TO_GAIN_2_DOLLARS(Action.Discard1DutchBeltToGain2Dollars.class),
    DISCARD_1_DUTCH_BELT_TO_GAIN_3_DOLLARS(Action.Discard1DutchBeltToGain3Dollars.class),
    DISCARD_1_GUERNSEY(Action.Discard1Guernsey.class),
    DISCARD_1_HOLSTEIN_TO_GAIN_10_DOLLARS(Action.Discard1HolsteinToGain10Dollars.class),
    DISCARD_1_JERSEY_TO_GAIN_1_CERTIFICATE(Action.Discard1JerseyToGain1Certificate.class),
    DISCARD_1_JERSEY_TO_GAIN_2_CERTIFICATES(Action.Discard1JerseyToGain2Certificates.class),
    DISCARD_1_JERSEY_TO_GAIN_2_DOLLARS(Action.Discard1JerseyToGain2Dollars.class),
    DISCARD_1_JERSEY_TO_GAIN_4_DOLLARS(Action.Discard1JerseyToGain4Dollars.class),
    DISCARD_1_JERSEY_TO_MOVE_ENGINE_1_FORWARD(Action.Discard1JerseyToMoveEngine1Forward.class),
    DISCARD_1_OBJECTIVE_CARD_TO_GAIN_2_CERTIFICATES(Action.Discard1ObjectiveCardToGain2Certificates.class),
    DISCARD_2_CARDS(Action.DiscardCards.Discard2Cards.class),
    DISCARD_2_GUERNSEY_TO_GAIN_4_DOLLARS(Action.Discard2GuernseyToGain4Dollars.class),
    DISCARD_3_CARDS(Action.DiscardCards.Discard3Cards.class),
    DISCARD_PAIR_TO_GAIN_3_DOLLARS(Action.DiscardPairToGain3Dollars.class),
    DISCARD_PAIR_TO_GAIN_4_DOLLARS(Action.DiscardPairToGain4Dollars.class),
    DRAW_1_CARD_THEN_DISCARD_1_CARD(Action.DrawCardsThenDiscardCards.Draw1CardThenDiscard1Card.class),
    DRAW_2_CARDS_THEN_DISCARD_2_CARDS(Action.DrawCardsThenDiscardCards.Draw2CardsThenDiscard2Cards.class),
    DRAW_2_CATTLE_CARDS(Action.Draw2CattleCards.class),
    DRAW_CARDS_UP_TO_NUMBER_OF_COWBOYS_THEN_DISCARD_CARDS(Action.DrawCardsUpToNumberOfCowboysThenDiscardCards.class),
    DRAW_UP_TO_1_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo1CardsThenDiscardCards.class),
    DRAW_UP_TO_2_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo2CardsThenDiscardCards.class),
    DRAW_UP_TO_3_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo3CardsThenDiscardCards.class),
    DRAW_UP_TO_4_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo4CardsThenDiscardCards.class),
    DRAW_UP_TO_5_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo5CardsThenDiscardCards.class),
    DRAW_UP_TO_6_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo6CardsThenDiscardCards.class),
    EXTRAORDINARY_DELIVERY(Action.ExtraordinaryDelivery.class),
    GAIN_1_CERTIFICATE(Action.Gain1Certificate.class),
    GAIN_1_DOLLAR(Action.Gain1Dollar.class),
    GAIN_1_DOLLAR_PER_BUILDING_IN_WOODS(Action.Gain1DollarPerBuildingInWoods.class),
    GAIN_1_DOLLAR_PER_ENGINEER(Action.Gain1DollarPerEngineer.class),
    GAIN_2_CERTIFICATES_AND_2_DOLLARS_PER_TEEPEE_PAIR(Action.Gain2CertificatesAnd2DollarsPerTeepeePair.class),
    GAIN_2_DOLLARS(Action.Gain2Dollars.class),
    GAIN_4_DOLLARS(Action.Gain4Dollars.class),
    HIRE_CHEAP_WORKER(Action.HireCheapWorker.class),
    HIRE_SECOND_WORKER(Action.HireSecondWorker.class),
    HIRE_WORKER(Action.HireWorker.class),
    MAX_CERTIFICATES(Action.MaxCertificates.class),
    MOVE(Action.Move.class),
    MOVE_1_FORWARD(Action.Move1Forward.class),
    MOVE_2_FORWARD(Action.Move2Forward.class),
    MOVE_3_FORWARD(Action.Move3Forward.class),
    MOVE_3_FORWARD_WITHOUT_FEES(Action.Move3ForwardWithoutFees.class),
    MOVE_4_FORWARD(Action.Move4Forward.class),
    MOVE_ENGINE_1_BACKWARDS_TO_GAIN_3_DOLLARS(Action.MoveEngine1BackwardsToGain3Dollars.class),
    MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD(Action.MoveEngine1BackwardsToRemove1Card.class),
    MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS(Action.MoveEngine2BackwardsToRemove2Cards.class),
    MOVE_ENGINE_2_OR_3_FORWARD(Action.MoveEngine2Or3Forward.class),
    MOVE_ENGINE_AT_LEAST_1_BACKWARDS_AND_GAIN_3_DOLLARS(Action.MoveEngineAtLeast1BackwardsAndGain3Dollars.class),
    MOVE_ENGINE_AT_MOST_2_FORWARD(Action.MoveEngineAtMost2Forward.class),
    MOVE_ENGINE_AT_MOST_3_FORWARD(Action.MoveEngineAtMost3Forward.class),
    MOVE_ENGINE_AT_MOST_4_FORWARD(Action.MoveEngineAtMost4Forward.class),
    MOVE_ENGINE_AT_MOST_5_FORWARD(Action.MoveEngineAtMost5Forward.class),
    MOVE_ENGINE_FORWARD(Action.MoveEngineForward.class),
    MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_BUILDINGS_IN_WOODS(Action.MoveEngineForwardUpToNumberOfBuildingsInWoods.class),
    PAY_1_DOLLAR_AND_MOVE_ENGINE_1_BACKWARDS_TO_GAIN_1_CERTIFICATE(Action.Pay1DollarAndMoveEngine1BackwardsToGain1Certificate.class),
    PAY_1_DOLLAR_TO_MOVE_ENGINE_1_FORWARD(Action.Pay1DollarToMoveEngine1Forward.class),
    PAY_2_DOLLARS_AND_MOVE_ENGINE_2_BACKWARDS_TO_GAIN_2_CERTIFICATES(Action.Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates.class),
    PAY_2_DOLLARS_TO_MOVE_ENGINE_2_FORWARD(Action.Pay2DollarsToMoveEngine2Forward.class),
    PLACE_BUILDING(Action.PlaceBuilding.class),
    PLACE_CHEAP_BUILDING(Action.PlaceCheapBuilding.class),
    PLAY_OBJECTIVE_CARD(Action.PlayObjectiveCard.class),
    REMOVE_1_CARD(Action.Remove1Card.class),
    REMOVE_2_CARDS(Action.Remove2Cards.class),
    REMOVE_HAZARD(Action.RemoveHazard.class),
    REMOVE_HAZARD_FOR_5_DOLLARS(Action.RemoveHazardFor5Dollars.class),
    REMOVE_HAZARD_FOR_FREE(Action.RemoveHazardForFree.class),
    SINGLE_AUXILIARY_ACTION(Action.SingleAuxiliaryAction.class),
    SINGLE_OR_DOUBLE_AUXILIARY_ACTION(Action.SingleOrDoubleAuxiliaryAction.class),
    TAKE_OBJECTIVE_CARD(Action.TakeObjectiveCard.class),
    TRADE_WITH_INDIANS(Action.TradeWithIndians.class),
    UPGRADE_ANY_STATION_BEHIND_ENGINE(Action.UpgradeAnyStationBehindEngine.class),
    UPGRADE_STATION(Action.UpgradeStation.class),
    USE_ADJACENT_BUILDING(Action.UseAdjacentBuilding.class),
    CHOOSE_FORESIGHTS(Action.ChooseForesights.class);

    @Getter
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
