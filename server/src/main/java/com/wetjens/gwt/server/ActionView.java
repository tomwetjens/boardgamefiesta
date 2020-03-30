package com.wetjens.gwt.server;

import com.wetjens.gwt.Action;
import com.wetjens.gwt.CattleMarket;
import com.wetjens.gwt.Location;
import com.wetjens.gwt.Move;
import com.wetjens.gwt.NeutralBuilding;
import com.wetjens.gwt.ObjectiveCard;
import com.wetjens.gwt.PlayObjectiveCard;
import com.wetjens.gwt.PlayerState;
import com.wetjens.gwt.RailroadTrack;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ActionView {

    CHOOSE_FORESIGHTS(Location.KansasCity.ChooseForesights.class),
    MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS(Action.SingleOrDoubleAuxiliaryAction.MoveEngine2SpacesBackwardsToRemove2Cards.class),
    PAY_2_TO_MOVE_ENGINE_2_FORWARD(Action.Pay2DollarsToMoveEngine2SpacesForward.class),
    DRAW_2_CATTLE_CARDS(CattleMarket.Draw2CattleCards.class),
    DRAW_3_CARDS_THEN_DISCARD_3_CARDS(Action.DrawCardThenDiscardCard.Draw3CardsThenDiscard3Cards.class),
    GAIN_1_DOLLAR(Action.SingleAuxiliaryAction.Gain1Dollars.class),
    DISCARD_1_DUTCH_BELT(NeutralBuilding.B.DiscardOneDutchBelt.class),
    DISCARD_1_CARD(Action.DiscardCards.Discard1Card.class),
    DISCARD_2_CARDS(Action.DiscardCards.Discard2Cards.class),
    DISCARD_3_CARDS(Action.DiscardCards.Discard3Cards.class),
    DISCARD_PAIR_TO_GAIN_4_DOLLARS(NeutralBuilding.F.DiscardPairToGain4Dollars.class),
    DRAW_2_CARDS_THEN_DISCARD_2_CARDS(Action.DrawCardThenDiscardCard.Draw2CardsThenDiscard2Cards.class),
    PAY_1_DOLLAR_TO_MOVE_ENGINE_1_FORWARD(Action.SingleAuxiliaryAction.Pay1DollarToMoveEngine1SpaceForward.class),
    DISCARD_1_JERSEY_TO_GAIN_2_DOLLARS(PlayerState.DiscardOneJerseyToGainTwoDollars.class),
    REMOVE_2_CARDS(Action.Remove2Cards.class),
    DELIVER_TO_CITY(Location.KansasCity.DeliverToCity.class),
    SINGLE_AUX_ACTION(Action.SingleAuxiliaryAction.class),
    SINGLE_OR_DOUBLE_AUX_ACTION(Action.SingleOrDoubleAuxiliaryAction.class),
    PLACE_BUILDING_FOR_2_DOLLARS_PER_CRAFTSMAN(NeutralBuilding.B.PlaceBuilding.class),
    DISCARD_1_GUERNSEY(NeutralBuilding.A.DiscardOneGuernsey.class),
    MOVE_ENGINE_2_OR_3_FORWARD(ObjectiveCard.MoveEngine2Or3SpacesForward.class),
    REMOVE_HAZARD_FOR_FREE(Action.RemoveHazardForFree.class),
    PAY_2_DOLLARS_AND_MOVE_ENGINE_2_BACKWARDS_TO_GAIN_2_CERTS(Action.SingleOrDoubleAuxiliaryAction.Pay2DollarsAndMoveEngine2SpacesBackwardsToGain2Certificates.class),
    GAIN_OBJECTIVE_CARD(Action.GainObjectiveCard.class),
    UPGRADE_STATION(RailroadTrack.UpgradeStation.class),
    APPOINT_STATION_MASTER(RailroadTrack.AppointStationMaster.class),
    PLAY_OBJECTIVE_CARD(PlayObjectiveCard.class),
    DISCARD_1_JERSEY_TO_GAIN_2_CERTS(PlayerState.DiscardOneJerseyToGainTwoCertificates.class),
    REMOVE_HAZARD(NeutralBuilding.F.RemoveHazard.class),
    PAY_1_DOLLAR_AND_MOVE_ENGINE_1_BACKWARDS_TO_GAIN_1_CERT(Action.SingleAuxiliaryAction.Pay1DollarAndMoveEngine1SpaceBackwardsToGain1Certificate.class),
    MOVE(Move.class),
    REMOVE_1_CARD(Action.Remove1Card.class),
    MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD(Action.SingleAuxiliaryAction.MoveEngine1SpaceBackwardsToRemove1Card.class),
    HIRE_WORKER_DISCOUNT(PlayerState.HireCheapWorker.class),
    HIRE_WORKER(NeutralBuilding.A.HireWorker.class),
    HIRE_SECOND_WORKER(NeutralBuilding.A.HireSecondWorker.class),
    DISCARD_1_BLACK_ANGUS_TO_GAIN_2_DOLLARS(NeutralBuilding.E.Discard1BlackAngusToGain2Dollars.class),
    DRAW_1_CARD_THEN_DISCARD_1_CARD(Action.DrawCardThenDiscardCard.Draw1CardThenDiscard1Card.class),
    GAIN_2_DOLLARS(Action.SingleOrDoubleAuxiliaryAction.Gain2Dollars.class),
    BUY_CATTLE(Action.BuyCattle.class),
    TRADE_WITH_INDIANS(Action.TradeWithIndians.class),
    DISCARD_1_JERSEY_TO_GAIN_1_CERT(PlayerState.DiscardOneJerseyToGainCertificate.class),
    DISCARD_1_JERSEY_TO_GAIN_4_DOLLARS(PlayerState.DiscardOneJerseyToGainFourDollars.class),
    MOVE_ENGINE_FORWARD(Action.MoveEngineForward.class),
    PLACE_BUILDING_FOR_1_DOLLAR_PER_CRAFTSMAN(PlayerState.PlaceCheapBuilding.class),
    GAIN_1_CERT(NeutralBuilding.C.GainCertificate.class),
    PAY_2_DOLLARS_TO_MOVE_ENGINE_2_FORWARD(Action.Pay2DollarsToMoveEngine2SpacesForward.class);

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
