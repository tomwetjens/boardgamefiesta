package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.json.*;
import javax.ws.rs.BadRequestException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@AllArgsConstructor
public enum ActionType {

    APPOINT_STATION_MASTER(Action.AppointStationMaster.class, (jsonObject, game) -> new Action.AppointStationMaster(Worker.valueOf(jsonObject.getString(JsonProperties.WORKER)))),
    BUY_CATTLE(Action.BuyCattle.class, (jsonObject, game) -> new Action.BuyCattle(findCattleCards(game, jsonObject.getJsonArray(JsonProperties.CATTLE_CARDS)))),
    DELIVER_TO_CITY(Action.DeliverToCity.class, (jsonObject, game) -> new Action.DeliverToCity(City.valueOf(jsonObject.getString(JsonProperties.CITY)), jsonObject.getInt(JsonProperties.CERTIFICATES))),
    DISCARD_1_BLACK_ANGUS_TO_GAIN_2_CERTIFICATES(Action.Discard1BlackAngusToGain2Certificates.class, (jsonObject, game) -> new Action.Discard1BlackAngusToGain2Certificates()),
    DISCARD_1_BLACK_ANGUS_TO_GAIN_2_DOLLARS(Action.Discard1BlackAngusToGain2Dollars.class, (jsonObject, game) -> new Action.Discard1BlackAngusToGain2Dollars()),
    DISCARD_CARD(Action.DiscardCard.class, (jsonObject, game) -> new Action.DiscardCard(findCardInHand(game.currentPlayerState().getHand(), jsonObject.getJsonObject(JsonProperties.CARD)))),
    DISCARD_1_CATTLE_CARD_TO_GAIN_3_DOLLARS_AND_ADD_1_OBJECTIVE_CARD_TO_HAND(Action.Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand.class, (jsonObject, game) -> new Action.Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand(findCattleCardInHand(game.currentPlayerState().getHand(), jsonObject.getJsonObject(JsonProperties.CATTLE_CARD)))),
    ADD_1_OBJECTIVE_CARD_TO_HAND(Action.Add1ObjectiveCardToHand.class, (jsonObject, game) -> new Action.Add1ObjectiveCardToHand(findObjectiveCard(game, jsonObject.getJsonObject(JsonProperties.OBJECTIVE_CARD)))),
    DISCARD_1_CATTLE_CARD_TO_GAIN_1_CERTIFICATE(Action.Discard1CattleCardToGain1Certificate.class, (jsonObject, game) -> new Action.Discard1CattleCardToGain1Certificate(findCattleCardInHand(game.currentPlayerState().getHand(), jsonObject.getJsonObject(JsonProperties.CATTLE_CARD)))),
    DISCARD_1_DUTCH_BELT_TO_GAIN_2_DOLLARS(Action.Discard1DutchBeltToGain2Dollars.class, (jsonObject, game) -> new Action.Discard1DutchBeltToGain2Dollars()),
    DISCARD_1_DUTCH_BELT_TO_GAIN_3_DOLLARS(Action.Discard1DutchBeltToGain3Dollars.class, (jsonObject, game) -> new Action.Discard1DutchBeltToGain3Dollars()),
    DISCARD_1_GUERNSEY(Action.Discard1Guernsey.class, (jsonObject, game) -> new Action.Discard1Guernsey()),
    DISCARD_1_HOLSTEIN_TO_GAIN_10_DOLLARS(Action.Discard1HolsteinToGain10Dollars.class, (jsonObject, game) -> new Action.Discard1HolsteinToGain10Dollars()),
    DISCARD_1_JERSEY_TO_GAIN_1_CERTIFICATE(Action.Discard1JerseyToGain1Certificate.class, (jsonObject, game) -> new Action.Discard1JerseyToGain1Certificate()),
    DISCARD_1_JERSEY_TO_GAIN_1_CERTIFICATE_AND_2_DOLLARS(Action.Discard1JerseyToGain1CertificateAnd2Dollars.class, (jsonObject, game) -> new Action.Discard1JerseyToGain1CertificateAnd2Dollars()),
    DISCARD_1_JERSEY_TO_GAIN_2_CERTIFICATES(Action.Discard1JerseyToGain2Certificates.class, (jsonObject, game) -> new Action.Discard1JerseyToGain2Certificates()),
    DISCARD_1_JERSEY_TO_GAIN_2_DOLLARS(Action.Discard1JerseyToGain2Dollars.class, (jsonObject, game) -> new Action.Discard1JerseyToGain2Dollars()),
    DISCARD_1_JERSEY_TO_GAIN_4_DOLLARS(Action.Discard1JerseyToGain4Dollars.class, (jsonObject, game) -> new Action.Discard1JerseyToGain4Dollars()),
    DISCARD_1_JERSEY_TO_MOVE_ENGINE_1_FORWARD(Action.Discard1JerseyToMoveEngine1Forward.class, (jsonObject, game) -> new Action.Discard1JerseyToMoveEngine1Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    DISCARD_1_OBJECTIVE_CARD_TO_GAIN_2_CERTIFICATES(Action.Discard1ObjectiveCardToGain2Certificates.class, (jsonObject, game) -> new Action.Discard1ObjectiveCardToGain2Certificates(findObjectiveCardInHand(game.currentPlayerState().getHand(), jsonObject.getJsonObject(JsonProperties.OBJECTIVE_CARD)))),
    DISCARD_2_GUERNSEY_TO_GAIN_4_DOLLARS(Action.Discard2GuernseyToGain4Dollars.class, (jsonObject, game) -> new Action.Discard2GuernseyToGain4Dollars()),
    DISCARD_PAIR_TO_GAIN_3_DOLLARS(Action.DiscardPairToGain3Dollars.class, (jsonObject, game) -> new Action.DiscardPairToGain3Dollars(CattleType.valueOf(jsonObject.getString(JsonProperties.CATTLE_TYPE)))),
    DISCARD_PAIR_TO_GAIN_4_DOLLARS(Action.DiscardPairToGain4Dollars.class, (jsonObject, game) -> new Action.DiscardPairToGain4Dollars(CattleType.valueOf(jsonObject.getString(JsonProperties.CATTLE_TYPE)))),
    DRAW_CARD(Action.DrawCard.class, (jsonObject, game) -> new Action.DrawCard()),
    DRAW_2_CATTLE_CARDS(Action.Draw2CattleCards.class, ((jsonObject, game) -> new Action.SingleAuxiliaryAction())),
    EXTRAORDINARY_DELIVERY(Action.ExtraordinaryDelivery.class, (jsonObject, game) -> new Action.ExtraordinaryDelivery(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)), City.valueOf(jsonObject.getString(JsonProperties.CITY)))),
    GAIN_1_CERTIFICATE(Action.Gain1Certificate.class, ((jsonObject, game) -> new Action.Gain1Certificate())),
    GAIN_1_DOLLAR(Action.Gain1Dollar.class, ((jsonObject, game) -> new Action.Gain1Dollar())),
    GAIN_1_DOLLAR_PER_BUILDING_IN_WOODS(Action.Gain1DollarPerBuildingInWoods.class, ((jsonObject, game) -> new Action.Gain1DollarPerBuildingInWoods())),
    GAIN_1_DOLLAR_PER_ENGINEER(Action.Gain1DollarPerEngineer.class, ((jsonObject, game) -> new Action.Gain1DollarPerEngineer())),
    GAIN_2_CERTIFICATES_AND_2_DOLLARS_PER_TEEPEE_PAIR(Action.Gain2CertificatesAnd2DollarsPerTeepeePair.class, ((jsonObject, game) -> new Action.Gain2CertificatesAnd2DollarsPerTeepeePair())),
    GAIN_2_DOLLARS(Action.Gain2Dollars.class, (jsonObject, game) -> new Action.Gain2Dollars()),
    GAIN_4_DOLLARS(Action.Gain4Dollars.class, (jsonObject, game) -> new Action.Gain4Dollars()),
    HIRE_CHEAP_WORKER(Action.HireCheapWorker.class, (jsonObject, game) -> new Action.HireCheapWorker(Worker.valueOf(jsonObject.getString(JsonProperties.WORKER)))),
    HIRE_SECOND_WORKER(Action.HireSecondWorker.class, (jsonObject, game) -> new Action.HireSecondWorker(Worker.valueOf(jsonObject.getString(JsonProperties.WORKER)))),
    HIRE_WORKER(Action.HireWorker.class, (jsonObject, game) -> new Action.HireWorker(Worker.valueOf(jsonObject.getString(JsonProperties.WORKER)))),
    MAX_CERTIFICATES(Action.MaxCertificates.class, ((jsonObject, game) -> new Action.SingleAuxiliaryAction())),
    MOVE(Action.Move.class, ((jsonObject, game) -> new Action.Move(getJsonStrings(jsonObject, "steps").stream()
            .map(JsonString::getString)
            .map(game.getTrail()::getLocation)
            .collect(Collectors.toList())))),
    MOVE_1_FORWARD(Action.Move1Forward.class, (jsonObject, game) -> new Action.Move1Forward(game.getTrail().getLocation(jsonObject.getString(JsonProperties.TO)))),
    MOVE_2_FORWARD(Action.Move2Forward.class, (jsonObject, game) -> new Action.Move2Forward(game.getTrail().getLocation(jsonObject.getString(JsonProperties.TO)))),
    MOVE_3_FORWARD(Action.Move3Forward.class, (jsonObject, game) -> new Action.Move3Forward(game.getTrail().getLocation(jsonObject.getString(JsonProperties.TO)))),
    MOVE_3_FORWARD_WITHOUT_FEES(Action.Move3ForwardWithoutFees.class, (jsonObject, game) -> new Action.Move3ForwardWithoutFees(game.getTrail().getLocation(jsonObject.getString(JsonProperties.TO)))),
    MOVE_4_FORWARD(Action.Move4Forward.class, (jsonObject, game) -> new Action.Move4Forward(game.getTrail().getLocation(jsonObject.getString(JsonProperties.TO)))),
    MOVE_ENGINE_1_BACKWARDS_TO_GAIN_3_DOLLARS(Action.MoveEngine1BackwardsToGain3Dollars.class, (jsonObject, game) -> new Action.MoveEngine1BackwardsToGain3Dollars(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD(Action.MoveEngine1BackwardsToRemove1Card.class, (jsonObject, game) -> new Action.MoveEngine1BackwardsToRemove1Card(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS(Action.MoveEngine2BackwardsToRemove2Cards.class, (jsonObject, game) -> new Action.MoveEngine2BackwardsToRemove2Cards(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    MOVE_ENGINE_2_OR_3_FORWARD(Action.MoveEngine2Or3Forward.class, (jsonObject, game) -> new Action.MoveEngine2Or3Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    MOVE_ENGINE_AT_LEAST_1_BACKWARDS_AND_GAIN_3_DOLLARS(Action.MoveEngineAtLeast1BackwardsAndGain3Dollars.class, (jsonObject, game) -> new Action.MoveEngineAtLeast1BackwardsAndGain3Dollars(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    MOVE_ENGINE_AT_MOST_2_FORWARD(Action.MoveEngineAtMost2Forward.class, (jsonObject, game) -> new Action.MoveEngineAtMost2Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    MOVE_ENGINE_AT_MOST_3_FORWARD(Action.MoveEngineAtMost3Forward.class, (jsonObject, game) -> new Action.MoveEngineAtMost3Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    MOVE_ENGINE_AT_MOST_4_FORWARD(Action.MoveEngineAtMost4Forward.class, (jsonObject, game) -> new Action.MoveEngineAtMost4Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    MOVE_ENGINE_AT_MOST_5_FORWARD(Action.MoveEngineAtMost5Forward.class, (jsonObject, game) -> new Action.MoveEngineAtMost5Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    MOVE_ENGINE_FORWARD(Action.MoveEngineForward.class, (jsonObject, game) -> new Action.MoveEngineForward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_BUILDINGS_IN_WOODS(Action.MoveEngineForwardUpToNumberOfBuildingsInWoods.class, (jsonObject, game) -> new Action.MoveEngineForwardUpToNumberOfBuildingsInWoods(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    PAY_1_DOLLAR_AND_MOVE_ENGINE_1_BACKWARDS_TO_GAIN_1_CERTIFICATE(Action.Pay1DollarAndMoveEngine1BackwardsToGain1Certificate.class, (jsonObject, game) -> new Action.Pay1DollarAndMoveEngine1BackwardsToGain1Certificate(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    PAY_1_DOLLAR_TO_MOVE_ENGINE_1_FORWARD(Action.Pay1DollarToMoveEngine1Forward.class, (jsonObject, game) -> new Action.Pay1DollarToMoveEngine1Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    PAY_2_DOLLARS_AND_MOVE_ENGINE_2_BACKWARDS_TO_GAIN_2_CERTIFICATES(Action.Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates.class, (jsonObject, game) -> new Action.Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    PAY_2_DOLLARS_TO_MOVE_ENGINE_2_FORWARD(Action.Pay2DollarsToMoveEngine2Forward.class, (jsonObject, game) -> new Action.Pay2DollarsToMoveEngine2Forward(findSpace(game, jsonObject.getJsonObject(JsonProperties.TO)))),
    PLACE_BUILDING(Action.PlaceBuilding.class, (jsonObject, game) -> new Action.PlaceBuilding((Location.BuildingLocation) game.getTrail().getLocation(jsonObject.getString("location")), findPlayerBuilding(game, jsonObject.getString("building")))),
    PLACE_CHEAP_BUILDING(Action.PlaceCheapBuilding.class, (jsonObject, game) -> new Action.PlaceCheapBuilding((Location.BuildingLocation) game.getTrail().getLocation(jsonObject.getString("location")), findPlayerBuilding(game, jsonObject.getString("building")))),
    PLAY_OBJECTIVE_CARD(Action.PlayObjectiveCard.class, (jsonObject, game) -> new Action.PlayObjectiveCard(findObjectiveCardInHand(game.currentPlayerState().getHand(), jsonObject.getJsonObject(JsonProperties.OBJECTIVE_CARD)))),
    REMOVE_1_CARD(Action.Remove1Card.class, (jsonObject, game) -> new Action.Remove1Card(findCardInHand(game.currentPlayerState().getHand(), jsonObject.getJsonObject(JsonProperties.CARD)))),
    REMOVE_2_CARDS(Action.Remove2Cards.class, (jsonObject, game) -> new Action.Remove2Cards(findCardsInHand(game, jsonObject.getJsonArray(JsonProperties.CARDS)))),
    REMOVE_HAZARD(Action.RemoveHazard.class, (jsonObject, game) -> new Action.RemoveHazard(findHazard(game, jsonObject.getJsonObject("hazard")))),
    REMOVE_HAZARD_FOR_5_DOLLARS(Action.RemoveHazardFor5Dollars.class, (jsonObject, game) -> new Action.RemoveHazardFor5Dollars(findHazard(game, jsonObject.getJsonObject("hazard")))),
    REMOVE_HAZARD_FOR_FREE(Action.RemoveHazardForFree.class, (jsonObject, game) -> new Action.RemoveHazardForFree(findHazard(game, jsonObject.getJsonObject("hazard")))),
    SINGLE_AUXILIARY_ACTION(Action.SingleAuxiliaryAction.class, (jsonObject, game) -> new Action.SingleAuxiliaryAction()),
    SINGLE_OR_DOUBLE_AUXILIARY_ACTION(Action.SingleOrDoubleAuxiliaryAction.class, ((jsonObject, game) -> new Action.SingleOrDoubleAuxiliaryAction())),
    TAKE_OBJECTIVE_CARD(Action.TakeObjectiveCard.class, (jsonObject, game) -> new Action.TakeObjectiveCard(findObjectiveCard(game, jsonObject.getJsonObject(JsonProperties.OBJECTIVE_CARD)))),
    TRADE_WITH_INDIANS(Action.TradeWithIndians.class, (jsonObject, game) -> new Action.TradeWithIndians(jsonObject.getInt("reward"))),
    UPGRADE_ANY_STATION_BEHIND_ENGINE(Action.UpgradeAnyStationBehindEngine.class, (jsonObject, game) -> new Action.UpgradeAnyStationBehindEngine(game.getRailroadTrack().getStations().get(jsonObject.getInt("station")))),
    UPGRADE_STATION(Action.UpgradeStation.class, (jsonObject, game) -> new Action.UpgradeStation()),
    USE_ADJACENT_BUILDING(Action.UseAdjacentBuilding.class, (jsonObject, game) -> new Action.UseAdjacentBuilding()),
    CHOOSE_FORESIGHTS(Action.ChooseForesights.class, (jsonObject, game) -> new Action.ChooseForesights(jsonObject.getJsonArray("choices").stream()
            .map(jsonValue -> (JsonNumber) jsonValue)
            .map(JsonNumber::intValue)
            .collect(Collectors.toList()))),
    UNLOCK_BLACK_OR_WHITE(Action.UnlockBlackOrWhite.class, (jsonObject, game) -> new Action.UnlockBlackOrWhite(Unlockable.valueOf(jsonObject.getString("unlock")))),
    UNLOCK_WHITE(Action.UnlockWhite.class, (jsonObject, game) -> new Action.UnlockWhite(Unlockable.valueOf(jsonObject.getString("unlock")))),
    DOWNGRADE_STATION(Action.DowngradeStation.class, (jsonObject, game) -> new Action.DowngradeStation(findStation(game, jsonObject.getInt("station"))));

    private static Station findStation(Game game, int index) {
        return game.getRailroadTrack().getStations().get(index);
    }

    private static ObjectiveCard findObjectiveCard(Game game, JsonObject jsonObject) {
        List<ObjectiveCard.Task> tasks = getJsonStrings(jsonObject, "tasks").stream()
                .map(JsonString::getString)
                .map(ObjectiveCard.Task::valueOf)
                .collect(Collectors.toList());
        int points = jsonObject.getInt("points");

        return game.getObjectiveCards().getAvailable().stream()
                .filter(objectiveCard -> objectiveCard.getPoints() == points)
                .filter(objectiveCard -> objectiveCard.getTasks().size() == tasks.size() && objectiveCard.getTasks().containsAll(tasks))
                .findAny()
                .orElseThrow(() -> new BadRequestException("Objective card not available: " + tasks + " " + points));
    }

    private static Hazard findHazard(Game game, JsonObject jsonObject) {
        HazardType hazardType = HazardType.valueOf(jsonObject.getString("type"));
        return game.getTrail().getHazardLocations(hazardType).stream()
                .flatMap(hazardLocation -> hazardLocation.getHazard().stream())
                .max(Comparator.comparingInt(Hazard::getPoints))
                .orElseThrow(() -> new BadRequestException("Hazard not on trail: " + hazardType));
    }

    private static PlayerBuilding findPlayerBuilding(Game game, String building) {
        return game.currentPlayerState().getBuildings().stream()
                .filter(playerBuilding -> playerBuilding.getName().equals(building))
                .findAny()
                .orElseThrow(() -> new BadRequestException("Building not available: " + building));
    }

    private static Set<Card> findCardsInHand(Game game, JsonArray jsonArray) {
        var remaining = new HashSet<>(game.currentPlayerState().getHand());
        var result = new HashSet<Card>();

        for (var jsonObject : jsonArray.getValuesAs(JsonObject.class)) {
            var card = findCardInHand(remaining, jsonObject);

            if (card != null) {
                result.add(card);
                remaining.remove(card);
            }
        }

        return result;
    }

    private static RailroadTrack.Space findSpace(Game game, JsonObject jsonObject) {
        if (jsonObject.containsKey("number") && jsonObject.get("number").getValueType() == JsonValue.ValueType.NUMBER) {
            int number = jsonObject.getInt("number");
            return game.getRailroadTrack().getSpace(number);
        } else {
            return game.getRailroadTrack().getTurnouts().get(jsonObject.getInt("turnout"));
        }
    }

    private static Card.CattleCard findCattleCardInHand(Collection<Card> hand, JsonObject jsonObject) {
        CattleType type = CattleType.valueOf(jsonObject.getString("type"));
        int points = jsonObject.getInt("points");

        return hand.stream()
                .filter(card -> card instanceof Card.CattleCard)
                .map(card -> (Card.CattleCard) card)
                .filter(cattleCard -> cattleCard.getType() == type)
                .filter(cattleCard -> cattleCard.getPoints() == points)
                .findAny()
                .orElseThrow(() -> new BadRequestException("Cattle card not in hand: " + type + " " + points));
    }

    private static Card findCardInHand(Collection<Card> hand, JsonObject jsonObject) {
        if (jsonObject.containsKey("type")) {
            return findCattleCardInHand(hand, jsonObject);
        } else {
            return findObjectiveCardInHand(hand, jsonObject);
        }
    }

    private static ObjectiveCard findObjectiveCardInHand(Collection<Card> hand, JsonObject jsonObject) {
        Set<ObjectiveCard.Task> tasks = getJsonStrings(jsonObject, "tasks").stream()
                .map(JsonString::getString)
                .map(ObjectiveCard.Task::valueOf)
                .collect(Collectors.toSet());
        int points = jsonObject.getInt("points");

        return hand.stream()
                .filter(card -> card instanceof ObjectiveCard)
                .map(card -> (ObjectiveCard) card)
                .filter(objectiveCard -> objectiveCard.getTasks().size() == tasks.size() && objectiveCard.getTasks().containsAll(tasks))
                .filter(objectiveCard -> objectiveCard.getPoints() == points)
                .findAny()
                .orElseThrow(() -> new BadRequestException("Objective card not in hand: " + tasks + " " + points));
    }

    private static Set<Card.CattleCard> findCattleCards(Game game, JsonArray cattleCards) {
        return cattleCards.stream()
                .map(JsonValue::asJsonObject)
                .map(cattleCardJsonObject -> {
                    CattleType type = CattleType.valueOf(cattleCardJsonObject.getString("type"));
                    int points = cattleCardJsonObject.getInt("points");

                    return game.getCattleMarket().getMarket().stream()
                            .filter(cattleCard -> cattleCard.getType() == type)
                            .filter(cattleCard -> cattleCard.getPoints() == points)
                            .findAny().orElseThrow(() -> new BadRequestException("Cattle card not in market: " + type + " " + points));
                })
                .collect(Collectors.toSet());
    }

    @Getter
    Class<? extends Action> action;

    BiFunction<JsonObject, Game, Action> deserializer;

    public static ActionType of(Class<? extends Action> action) {
        for (ActionType value : values()) {
            if (value.action.equals(action)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported action: " + action);
    }

    public Action toAction(JsonObject jsonObject, Game game) {
        return deserializer.apply(jsonObject, game);
    }

    private static List<JsonString> getJsonStrings(JsonObject jsonObject, String key) {
        JsonArray jsonArray = jsonObject.getJsonArray(key);
        if (jsonArray == null) {
            throw new JsonException("Property missing: " + key);
        }
        return jsonArray.getValuesAs(JsonString.class);
    }

    private static class JsonProperties {
        private static final String WORKER = "worker";
        private static final String CARD = "card";
        private static final String CARDS = "cards";
        private static final String CATTLE_CARD = "cattleCard";
        private static final String CATTLE_CARDS = "cattleCards";
        private static final String OBJECTIVE_CARD = "objectiveCard";
        private static final String CERTIFICATES = "certificates";
        private static final String CITY = "city";
        private static final String CATTLE_TYPE = "cattleType";
        private static final String TO = "to";
    }
}
