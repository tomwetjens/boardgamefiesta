package com.wetjens.gwt.server.rest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;

import com.wetjens.gwt.Action;
import com.wetjens.gwt.Card;
import com.wetjens.gwt.CattleType;
import com.wetjens.gwt.City;
import com.wetjens.gwt.GWTError;
import com.wetjens.gwt.Game;
import com.wetjens.gwt.Hazard;
import com.wetjens.gwt.HazardType;
import com.wetjens.gwt.Location;
import com.wetjens.gwt.ObjectiveCard;
import com.wetjens.gwt.PlayerBuilding;
import com.wetjens.gwt.RailroadTrack;
import com.wetjens.gwt.Station;
import com.wetjens.gwt.Unlockable;
import com.wetjens.gwt.Worker;
import com.wetjens.gwt.server.domain.ActionType;
import com.wetjens.gwt.server.domain.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@JsonbTypeDeserializer(ActionRequest.Deserializer.class)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionRequest {

    @Getter
    private final ActionType type;
    private final JsonObject jsonObject;

    public Action toAction(Game game) {
        switch (type) {
            case APPOINT_STATION_MASTER:
                return new Action.AppointStationMaster(Worker.valueOf(jsonObject.getString(JsonProperties.WORKER)));
            case BUY_CATTLE:
                return new Action.BuyCattle(findCattleCards(game, jsonObject.getJsonArray(JsonProperties.CATTLE_CARDS)));
            case DELIVER_TO_CITY:
                return new Action.DeliverToCity(City.valueOf(jsonObject.getString(JsonProperties.CITY)), jsonObject.getInt(JsonProperties.CERTIFICATES));
            case DISCARD_1_BLACK_ANGUS_TO_GAIN_2_CERTIFICATES:
                return new Action.Discard1BlackAngusToGain2Certificates();
            case DISCARD_1_BLACK_ANGUS_TO_GAIN_2_DOLLARS:
                return new Action.Discard1BlackAngusToGain2Dollars();
            case DISCARD_CARD:
                return new Action.DiscardCard(findCardInHand(game.currentPlayerState().getHand(), jsonObject.getJsonObject(JsonProperties.CARD)));
            case DISCARD_1_CATTLE_CARD_TO_GAIN_3_DOLLARS_AND_ADD_1_OBJECTIVE_CARD_TO_HAND:
                return new Action.Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand(CattleType.valueOf(jsonObject.getString(JsonProperties.CATTLE_TYPE)));
            case ADD_1_OBJECTIVE_CARD_TO_HAND:
                return new Action.Add1ObjectiveCardToHand(findObjectiveCard(game, jsonObject.getJsonObject(JsonProperties.OBJECTIVE_CARD)));
            case DISCARD_1_CATTLE_CARD_TO_GAIN_1_CERTIFICATE:
                return new Action.Discard1CattleCardToGain1Certificate(CattleType.valueOf(jsonObject.getString(JsonProperties.CATTLE_TYPE)));
            case DISCARD_1_DUTCH_BELT_TO_GAIN_2_DOLLARS:
                return new Action.Discard1DutchBeltToGain2Dollars();
            case DISCARD_1_DUTCH_BELT_TO_GAIN_3_DOLLARS:
                return new Action.Discard1DutchBeltToGain3Dollars();
            case DISCARD_1_GUERNSEY:
                return new Action.Discard1Guernsey();
            case DISCARD_1_HOLSTEIN_TO_GAIN_10_DOLLARS:
                return new Action.Discard1HolsteinToGain10Dollars();
            case DISCARD_1_JERSEY_TO_GAIN_1_CERTIFICATE:
                return new Action.Discard1JerseyToGain1Certificate();
            case DISCARD_1_JERSEY_TO_GAIN_1_CERTIFICATE_AND_2_DOLLARS:
                return new Action.Discard1JerseyToGain1CertificateAnd2Dollars();
            case DISCARD_1_JERSEY_TO_GAIN_2_CERTIFICATES:
                return new Action.Discard1JerseyToGain2Certificates();
            case DISCARD_1_JERSEY_TO_GAIN_2_DOLLARS:
                return new Action.Discard1JerseyToGain2Dollars();
            case DISCARD_1_JERSEY_TO_GAIN_4_DOLLARS:
                return new Action.Discard1JerseyToGain4Dollars();
            case DISCARD_1_JERSEY_TO_MOVE_ENGINE_1_FORWARD:
                return new Action.Discard1JerseyToMoveEngine1Forward();
            case DISCARD_1_OBJECTIVE_CARD_TO_GAIN_2_CERTIFICATES:
                return new Action.Discard1ObjectiveCardToGain2Certificates(findObjectiveCardInHand(game.currentPlayerState().getHand(), jsonObject.getJsonObject(JsonProperties.OBJECTIVE_CARD)));
            case DISCARD_2_GUERNSEY_TO_GAIN_4_DOLLARS:
                return new Action.Discard2GuernseyToGain4Dollars();
            case DISCARD_PAIR_TO_GAIN_3_DOLLARS:
                return new Action.DiscardPairToGain3Dollars(CattleType.valueOf(jsonObject.getString(JsonProperties.CATTLE_TYPE)));
            case DISCARD_PAIR_TO_GAIN_4_DOLLARS:
                return new Action.DiscardPairToGain4Dollars(CattleType.valueOf(jsonObject.getString(JsonProperties.CATTLE_TYPE)));
            case DRAW_CARD:
                return new Action.DrawCard();
            case DRAW_2_CATTLE_CARDS:
                return new Action.Draw2CattleCards();
            case EXTRAORDINARY_DELIVERY:
                return new Action.ExtraordinaryDelivery(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)), City.valueOf(jsonObject.getString(JsonProperties.CITY)));
            case GAIN_1_CERTIFICATE:
                return new Action.Gain1Certificate();
            case GAIN_1_DOLLAR:
                return new Action.Gain1Dollar();
            case GAIN_2_DOLLARS_PER_BUILDING_IN_WOODS:
                return new Action.Gain2DollarsPerBuildingInWoods();
            case GAIN_1_DOLLAR_PER_ENGINEER:
                return new Action.Gain1DollarPerEngineer();
            case GAIN_2_CERTIFICATES_AND_2_DOLLARS_PER_TEEPEE_PAIR:
                return new Action.Gain2CertificatesAnd2DollarsPerTeepeePair();
            case GAIN_2_DOLLARS:
                return new Action.Gain2Dollars();
            case GAIN_4_DOLLARS:
                return new Action.Gain4Dollars();
            case HIRE_CHEAP_WORKER:
                return new Action.HireCheapWorker(Worker.valueOf(jsonObject.getString(JsonProperties.WORKER)));
            case HIRE_SECOND_WORKER:
                return new Action.HireSecondWorker(Worker.valueOf(jsonObject.getString(JsonProperties.WORKER)));
            case HIRE_WORKER:
                return new Action.HireWorker(Worker.valueOf(jsonObject.getString(JsonProperties.WORKER)));
            case MAX_CERTIFICATES:
                return new Action.MaxCertificates();
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
                return new Action.MoveEngine1Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case MOVE_ENGINE_1_BACKWARDS_TO_GAIN_3_DOLLARS:
                return new Action.MoveEngine1BackwardsToGain3Dollars(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD:
                return new Action.MoveEngine1BackwardsToRemove1Card(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS:
                return new Action.MoveEngine2BackwardsToRemove2Cards(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case MOVE_ENGINE_2_OR_3_FORWARD:
                return new Action.MoveEngine2Or3Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case MOVE_ENGINE_AT_LEAST_1_BACKWARDS_AND_GAIN_3_DOLLARS:
                return new Action.MoveEngineAtLeast1BackwardsAndGain3Dollars(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case MOVE_ENGINE_AT_MOST_2_FORWARD:
                return new Action.MoveEngineAtMost2Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case MOVE_ENGINE_AT_MOST_3_FORWARD:
                return new Action.MoveEngineAtMost3Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case MOVE_ENGINE_AT_MOST_4_FORWARD:
                return new Action.MoveEngineAtMost4Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case MOVE_ENGINE_AT_MOST_5_FORWARD:
                return new Action.MoveEngineAtMost5Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case MOVE_ENGINE_FORWARD:
                return new Action.MoveEngineForward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_BUILDINGS_IN_WOODS:
                return new Action.MoveEngineForwardUpToNumberOfBuildingsInWoods(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case PAY_1_DOLLAR_AND_MOVE_ENGINE_1_BACKWARDS_TO_GAIN_1_CERTIFICATE:
                return new Action.Pay1DollarAndMoveEngine1BackwardsToGain1Certificate(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case PAY_1_DOLLAR_TO_MOVE_ENGINE_1_FORWARD:
                return new Action.Pay1DollarToMoveEngine1Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case PAY_2_DOLLARS_AND_MOVE_ENGINE_2_BACKWARDS_TO_GAIN_2_CERTIFICATES:
                return new Action.Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case PAY_2_DOLLARS_TO_MOVE_ENGINE_2_FORWARD:
                return new Action.Pay2DollarsToMoveEngine2Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)));
            case PLACE_BUILDING:
                return new Action.PlaceBuilding((Location.BuildingLocation) game.getTrail().getLocation(jsonObject.getString(JsonProperties.LOCATION)), findPlayerBuilding(game, jsonObject.getString(JsonProperties.BUILDING)));
            case PLACE_CHEAP_BUILDING:
                return new Action.PlaceCheapBuilding((Location.BuildingLocation) game.getTrail().getLocation(jsonObject.getString(JsonProperties.LOCATION)), findPlayerBuilding(game, jsonObject.getString(JsonProperties.BUILDING)));
            case PLAY_OBJECTIVE_CARD:
                return new Action.PlayObjectiveCard(findObjectiveCardInHand(game.currentPlayerState().getHand(), jsonObject.getJsonObject(JsonProperties.OBJECTIVE_CARD)));
            case REMOVE_CARD:
                return new Action.RemoveCard(findCardInHand(game.currentPlayerState().getHand(), jsonObject.getJsonObject(JsonProperties.CARD)));
            case REMOVE_HAZARD:
                return new Action.RemoveHazard(findHazard(game, jsonObject.getJsonObject(JsonProperties.HAZARD)));
            case REMOVE_HAZARD_FOR_5_DOLLARS:
                return new Action.RemoveHazardFor5Dollars(findHazard(game, jsonObject.getJsonObject(JsonProperties.HAZARD)));
            case REMOVE_HAZARD_FOR_FREE:
                return new Action.RemoveHazardForFree(findHazard(game, jsonObject.getJsonObject(JsonProperties.HAZARD)));
            case SINGLE_AUXILIARY_ACTION:
                return new Action.SingleAuxiliaryAction();
            case SINGLE_OR_DOUBLE_AUXILIARY_ACTION:
                return new Action.SingleOrDoubleAuxiliaryAction();
            case TAKE_OBJECTIVE_CARD:
                return new Action.TakeObjectiveCard(findObjectiveCard(game, jsonObject.getJsonObject(JsonProperties.OBJECTIVE_CARD)));
            case TRADE_WITH_INDIANS:
                return new Action.TradeWithIndians(jsonObject.getInt(JsonProperties.REWARD));
            case UPGRADE_ANY_STATION_BEHIND_ENGINE:
                return new Action.UpgradeAnyStationBehindEngine(game.getRailroadTrack().getStations().get(jsonObject.getInt(JsonProperties.STATION)));
            case UPGRADE_STATION:
                return new Action.UpgradeStation();
            case USE_ADJACENT_BUILDING:
                return new Action.UseAdjacentBuilding();
            case CHOOSE_FORESIGHTS:
                return new Action.ChooseForesights(jsonObject.getJsonArray(JsonProperties.CHOICES).stream()
                        .map(jsonValue -> (JsonNumber) jsonValue)
                        .map(JsonNumber::intValue)
                        .collect(Collectors.toList()));
            case UNLOCK_BLACK_OR_WHITE:
                return new Action.UnlockBlackOrWhite(Unlockable.valueOf(jsonObject.getString(JsonProperties.UNLOCK)));
            case UNLOCK_WHITE:
                return new Action.UnlockWhite(Unlockable.valueOf(jsonObject.getString(JsonProperties.UNLOCK)));
            case DOWNGRADE_STATION:
                return new Action.DowngradeStation(findStation(game, jsonObject.getInt(JsonProperties.STATION)));
            default:
                return null;
        }
    }

    public static final class Deserializer implements JsonbDeserializer<ActionRequest> {
        @Override
        public ActionRequest deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, Type type) {
            JsonObject jsonObject = jsonParser.getObject();

            var actionType = ActionType.valueOf(jsonObject.getString("type"));

            return new ActionRequest(actionType, jsonObject);
        }
    }

    private static List<Location> getSteps(JsonObject jsonObject, Game game) {
        return getJsonStrings(jsonObject, JsonProperties.STEPS).stream()
                .map(JsonString::getString)
                .map(game.getTrail()::getLocation)
                .collect(Collectors.toList());
    }

    private static Station findStation(Game game, int index) {
        return game.getRailroadTrack().getStations().get(index);
    }

    private static ObjectiveCard findObjectiveCard(Game game, JsonObject jsonObject) {
        List<ObjectiveCard.Task> tasks = getJsonStrings(jsonObject, JsonProperties.TASKS).stream()
                .map(JsonString::getString)
                .map(ObjectiveCard.Task::valueOf)
                .collect(Collectors.toList());
        int points = jsonObject.getInt(JsonProperties.POINTS);

        return game.getObjectiveCards().getAvailable().stream()
                .filter(objectiveCard -> objectiveCard.getPoints() == points)
                .filter(objectiveCard -> objectiveCard.getTasks().size() == tasks.size() && objectiveCard.getTasks().containsAll(tasks))
                .findAny()
                .orElseThrow(() -> new APIException(GWTError.OBJECTIVE_CARD_NOT_AVAILABLE));
    }

    private static Hazard findHazard(Game game, JsonObject jsonObject) {
        HazardType hazardType = HazardType.valueOf(jsonObject.getString(JsonProperties.TYPE));
        return game.getTrail().getHazardLocations(hazardType).stream()
                .flatMap(hazardLocation -> hazardLocation.getHazard().stream())
                .max(Comparator.comparingInt(Hazard::getPoints))
                .orElseThrow(() -> new APIException(GWTError.HAZARD_NOT_ON_TRAIL));
    }

    private static PlayerBuilding findPlayerBuilding(Game game, String building) {
        return game.currentPlayerState().getBuildings().stream()
                .filter(playerBuilding -> playerBuilding.getName().equals(building))
                .findAny()
                .orElseThrow(() -> new APIException(GWTError.BUILDING_NOT_AVAILABLE));
    }

    private static RailroadTrack.Space findSpace(Game game, JsonObject jsonObject) {
        if (jsonObject.containsKey(JsonProperties.NUMBER) && jsonObject.get(JsonProperties.NUMBER).getValueType() == JsonValue.ValueType.NUMBER) {
            int number = jsonObject.getInt(JsonProperties.NUMBER);
            return game.getRailroadTrack().getSpace(number);
        } else {
            return game.getRailroadTrack().getTurnouts().get(jsonObject.getInt(JsonProperties.TURNOUT));
        }
    }

    private static Card.CattleCard findCattleCardInHand(Collection<Card> hand, JsonObject jsonObject) {
        CattleType type = CattleType.valueOf(jsonObject.getString(JsonProperties.TYPE));

        return hand.stream()
                .filter(card -> card instanceof Card.CattleCard)
                .map(card -> (Card.CattleCard) card)
                .filter(cattleCard -> cattleCard.getType() == type)
                .findAny()
                .orElseThrow(() -> new APIException(GWTError.CARD_NOT_IN_HAND));
    }

    private static Card findCardInHand(Collection<Card> hand, JsonObject jsonObject) {
        if (jsonObject.containsKey(JsonProperties.TYPE)) {
            return findCattleCardInHand(hand, jsonObject);
        } else {
            return findObjectiveCardInHand(hand, jsonObject);
        }
    }

    private static ObjectiveCard findObjectiveCardInHand(Collection<Card> hand, JsonObject jsonObject) {
        List<ObjectiveCard.Task> tasks = getJsonStrings(jsonObject, JsonProperties.TASKS).stream()
                .map(JsonString::getString)
                .map(ObjectiveCard.Task::valueOf)
                .collect(Collectors.toList());
        int points = jsonObject.getInt(JsonProperties.POINTS);

        return hand.stream()
                .filter(card -> card instanceof ObjectiveCard)
                .map(card -> (ObjectiveCard) card)
                .filter(objectiveCard -> objectiveCard.getPoints() == points)
                .filter(objectiveCard -> containsExactlyInAnyOrder(objectiveCard.getTasks(), tasks))
                .findAny()
                .orElseThrow(() -> new APIException(GWTError.CARD_NOT_IN_HAND));
    }

    private static <T> boolean containsExactlyInAnyOrder(Collection<T> actual, Collection<T> values) {
        List<Object> notExpected = new ArrayList<>(actual);

        for (T value : values) {
            if (!notExpected.remove(value)) {
                return false;
            }
        }

        return notExpected.isEmpty();
    }

    private static Set<Card.CattleCard> findCattleCards(Game game, JsonArray cattleCards) {
        return cattleCards.stream()
                .map(JsonValue::asJsonObject)
                .map(cattleCardJsonObject -> {
                    CattleType type = CattleType.valueOf(cattleCardJsonObject.getString(JsonProperties.TYPE));
                    int points = cattleCardJsonObject.getInt(JsonProperties.POINTS);

                    return game.getCattleMarket().getMarket().stream()
                            .filter(cattleCard -> cattleCard.getType() == type)
                            .filter(cattleCard -> cattleCard.getPoints() == points)
                            .findAny().orElseThrow(() -> new APIException(GWTError.CATTLE_CARD_NOT_AVAILABLE));
                })
                .collect(Collectors.toSet());
    }

    private static List<JsonString> getJsonStrings(JsonObject jsonObject, String key) {
        JsonArray jsonArray = jsonObject.getJsonArray(key);
        if (jsonArray == null) {
            throw new JsonException("Property missing: " + key);
        }
        return jsonArray.getValuesAs(JsonString.class);
    }

    private static class JsonProperties {
        private static final String CATTLE_TYPE = "cattleType";
        private static final String WORKER = "worker";
        private static final String CARD = "card";
        private static final String CATTLE_CARDS = "cattleCards";
        private static final String OBJECTIVE_CARD = "objectiveCard";
        private static final String CERTIFICATES = "certificates";
        private static final String CITY = "city";
        private static final String TO = "to";
        private static final String HAZARD = "hazard";
        private static final String REWARD = "reward";
        private static final String LOCATION = "location";
        private static final String BUILDING = "building";
        private static final String STATION = "station";
        private static final String CHOICES = "choices";
        private static final String UNLOCK = "unlock";
        private static final String TASKS = "tasks";
        private static final String POINTS = "points";
        private static final String TYPE = "type";
        private static final String NUMBER = "number";
        private static final String TURNOUT = "turnout";
        private static final String STEPS = "steps";
    }
}
