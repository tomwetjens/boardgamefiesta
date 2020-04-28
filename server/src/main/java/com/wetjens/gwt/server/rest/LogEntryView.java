package com.wetjens.gwt.server.rest;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.JsonNumber;

import com.wetjens.gwt.Action;
import com.wetjens.gwt.CattleType;
import com.wetjens.gwt.City;
import com.wetjens.gwt.Location;
import com.wetjens.gwt.Unlockable;
import com.wetjens.gwt.Worker;
import com.wetjens.gwt.server.domain.ActionType;
import com.wetjens.gwt.server.domain.GameLogEntry;
import com.wetjens.gwt.server.domain.GameLogEntryType;
import com.wetjens.gwt.server.rest.view.UserView;

public class LogEntryView {

    Instant timestamp;
    UserView user;
    GameLogEntryType type;
    List<Object> values;

    static LogEntryView of(GameLogEntry gameLogEntry) {
return gameLogEntry.getAction().map(action ->{
    ActionType actionType = ActionType.of(action.getClass());
    switch (actionType) {
        case APPOINT_STATION_MASTER:
            return new Action.AppointStationMaster(Worker.valueOf(jsonObject.getString(ActionRequest.JsonProperties.WORKER)));
        case BUY_CATTLE:
            return new Action.BuyCattle(findCattleCards(game, jsonObject.getJsonArray(ActionRequest.JsonProperties.CATTLE_CARDS)));
        case DELIVER_TO_CITY:
            return new Action.DeliverToCity(City.valueOf(jsonObject.getString(ActionRequest.JsonProperties.CITY)), jsonObject.getInt(ActionRequest.JsonProperties.CERTIFICATES));
        case DISCARD_CARD:
            return new Action.DiscardCard(findCardInHand(game.currentPlayerState().getHand(), jsonObject.getJsonObject(ActionRequest.JsonProperties.CARD)));
        case DISCARD_1_CATTLE_CARD_TO_GAIN_3_DOLLARS_AND_ADD_1_OBJECTIVE_CARD_TO_HAND:
            return new Action.Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand(CattleType.valueOf(jsonObject.getString(ActionRequest.JsonProperties.CATTLE_TYPE)));
        case ADD_1_OBJECTIVE_CARD_TO_HAND:
            return new Action.Add1ObjectiveCardToHand(findObjectiveCard(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.OBJECTIVE_CARD)));
        case DISCARD_1_CATTLE_CARD_TO_GAIN_1_CERTIFICATE:
            return new Action.Discard1CattleCardToGain1Certificate(CattleType.valueOf(jsonObject.getString(ActionRequest.JsonProperties.CATTLE_TYPE)));
        case DISCARD_1_OBJECTIVE_CARD_TO_GAIN_2_CERTIFICATES:
            return new Action.Discard1ObjectiveCardToGain2Certificates(findObjectiveCardInHand(game.currentPlayerState().getHand(), jsonObject.getJsonObject(ActionRequest.JsonProperties.OBJECTIVE_CARD)));
        case DISCARD_PAIR_TO_GAIN_3_DOLLARS:
            return new Action.DiscardPairToGain3Dollars(CattleType.valueOf(jsonObject.getString(ActionRequest.JsonProperties.CATTLE_TYPE)));
        case DISCARD_PAIR_TO_GAIN_4_DOLLARS:
            return new Action.DiscardPairToGain4Dollars(CattleType.valueOf(jsonObject.getString(ActionRequest.JsonProperties.CATTLE_TYPE)));
        case EXTRAORDINARY_DELIVERY:
            return new Action.ExtraordinaryDelivery(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)), City.valueOf(jsonObject.getString(ActionRequest.JsonProperties.CITY)));
        case HIRE_CHEAP_WORKER:
            return new Action.HireCheapWorker(Worker.valueOf(jsonObject.getString(ActionRequest.JsonProperties.WORKER)));
        case HIRE_SECOND_WORKER:
            return new Action.HireSecondWorker(Worker.valueOf(jsonObject.getString(ActionRequest.JsonProperties.WORKER)));
        case HIRE_WORKER:
            return new Action.HireWorker(Worker.valueOf(jsonObject.getString(ActionRequest.JsonProperties.WORKER)));
        case MOVE:
            return new Action.Move(getSteps(jsonObject, game));
        case MOVE_1_FORWARD:
            return new Action.Move1Forward(getSteps(jsonObject, game));
        case MOVE_2_FORWARD:
            return new Action.Move2Forward(getSteps(jsonObject, game));
        case MOVE_3_FORWARD:
            return new Action.Move3Forward(getSteps(jsonObject, game));
        case MOVE_3_FORWARD_WITHOUT_FEES:
            return new Action.Move3ForwardWithoutFees(getSteps(jsonObject, game));
        case MOVE_4_FORWARD:
            return new Action.Move4Forward(getSteps(jsonObject, game));
        case MOVE_ENGINE_1_FORWARD:
            return new Action.MoveEngine1Forward(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case MOVE_ENGINE_1_BACKWARDS_TO_GAIN_3_DOLLARS:
            return new Action.MoveEngine1BackwardsToGain3Dollars(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD:
            return new Action.MoveEngine1BackwardsToRemove1Card(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS:
            return new Action.MoveEngine2BackwardsToRemove2Cards(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case MOVE_ENGINE_2_OR_3_FORWARD:
            return new Action.MoveEngine2Or3Forward(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case MOVE_ENGINE_AT_LEAST_1_BACKWARDS_AND_GAIN_3_DOLLARS:
            return new Action.MoveEngineAtLeast1BackwardsAndGain3Dollars(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case MOVE_ENGINE_AT_MOST_2_FORWARD:
            return new Action.MoveEngineAtMost2Forward(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case MOVE_ENGINE_AT_MOST_3_FORWARD:
            return new Action.MoveEngineAtMost3Forward(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case MOVE_ENGINE_AT_MOST_4_FORWARD:
            return new Action.MoveEngineAtMost4Forward(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case MOVE_ENGINE_AT_MOST_5_FORWARD:
            return new Action.MoveEngineAtMost5Forward(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case MOVE_ENGINE_FORWARD:
            return new Action.MoveEngineForward(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_BUILDINGS_IN_WOODS:
            return new Action.MoveEngineForwardUpToNumberOfBuildingsInWoods(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case PAY_1_DOLLAR_AND_MOVE_ENGINE_1_BACKWARDS_TO_GAIN_1_CERTIFICATE:
            return new Action.Pay1DollarAndMoveEngine1BackwardsToGain1Certificate(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case PAY_1_DOLLAR_TO_MOVE_ENGINE_1_FORWARD:
            return new Action.Pay1DollarToMoveEngine1Forward(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case PAY_2_DOLLARS_AND_MOVE_ENGINE_2_BACKWARDS_TO_GAIN_2_CERTIFICATES:
            return new Action.Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case PAY_2_DOLLARS_TO_MOVE_ENGINE_2_FORWARD:
            return new Action.Pay2DollarsToMoveEngine2Forward(findSpace(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.TO)));
        case PLACE_BUILDING:
            return new Action.PlaceBuilding((Location.BuildingLocation) game.getTrail().getLocation(jsonObject.getString(ActionRequest.JsonProperties.LOCATION)), findPlayerBuilding(game, jsonObject.getString(ActionRequest.JsonProperties.BUILDING)));
        case PLACE_CHEAP_BUILDING:
            return new Action.PlaceCheapBuilding((Location.BuildingLocation) game.getTrail().getLocation(jsonObject.getString(ActionRequest.JsonProperties.LOCATION)), findPlayerBuilding(game, jsonObject.getString(ActionRequest.JsonProperties.BUILDING)));
        case PLAY_OBJECTIVE_CARD:
            return new Action.PlayObjectiveCard(findObjectiveCardInHand(game.currentPlayerState().getHand(), jsonObject.getJsonObject(ActionRequest.JsonProperties.OBJECTIVE_CARD)));
        case REMOVE_CARD:
            return new Action.RemoveCard(findCardInHand(game.currentPlayerState().getHand(), jsonObject.getJsonObject(ActionRequest.JsonProperties.CARD)));
        case REMOVE_HAZARD:
            return new Action.RemoveHazard(findHazard(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.HAZARD)));
        case REMOVE_HAZARD_FOR_5_DOLLARS:
            return new Action.RemoveHazardFor5Dollars(findHazard(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.HAZARD)));
        case REMOVE_HAZARD_FOR_FREE:
            return new Action.RemoveHazardForFree(findHazard(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.HAZARD)));
        case SINGLE_AUXILIARY_ACTION:
            return new Action.SingleAuxiliaryAction();
        case SINGLE_OR_DOUBLE_AUXILIARY_ACTION:
            return new Action.SingleOrDoubleAuxiliaryAction();
        case TAKE_OBJECTIVE_CARD:
            return new Action.TakeObjectiveCard(findObjectiveCard(game, jsonObject.getJsonObject(ActionRequest.JsonProperties.OBJECTIVE_CARD)));
        case TRADE_WITH_INDIANS:
            return new Action.TradeWithIndians(jsonObject.getInt(ActionRequest.JsonProperties.REWARD));
        case UPGRADE_ANY_STATION_BEHIND_ENGINE:
            return new Action.UpgradeAnyStationBehindEngine(game.getRailroadTrack().getStations().get(jsonObject.getInt(ActionRequest.JsonProperties.STATION)));
        case CHOOSE_FORESIGHTS:
            return new Action.ChooseForesights(jsonObject.getJsonArray(ActionRequest.JsonProperties.CHOICES).stream()
                    .map(jsonValue -> (JsonNumber) jsonValue)
                    .map(JsonNumber::intValue)
                    .collect(Collectors.toList()));
        case UNLOCK_BLACK_OR_WHITE:
            return new Action.UnlockBlackOrWhite(Unlockable.valueOf(jsonObject.getString(ActionRequest.JsonProperties.UNLOCK)));
        case UNLOCK_WHITE:
            return new Action.UnlockWhite(Unlockable.valueOf(jsonObject.getString(ActionRequest.JsonProperties.UNLOCK)));
        case DOWNGRADE_STATION:
            return new Action.DowngradeStation(findStation(game, jsonObject.getInt(ActionRequest.JsonProperties.STATION)));
    }
})
    }

}
