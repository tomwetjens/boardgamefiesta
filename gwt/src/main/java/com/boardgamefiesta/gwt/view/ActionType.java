package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.gwt.logic.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.json.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public enum ActionType {

    APPOINT_STATION_MASTER(Action.AppointStationMaster.class),
    BUY_CATTLE(Action.BuyCattle.class),
    DELIVER_TO_CITY(Action.DeliverToCity.class),
    DISCARD_1_BLACK_ANGUS_TO_GAIN_2_CERTIFICATES(Action.Discard1BlackAngusToGain2Certificates.class),
    DISCARD_1_BLACK_ANGUS_TO_GAIN_2_DOLLARS(Action.Discard1BlackAngusToGain2Dollars.class),
    DISCARD_CARD(Action.DiscardCard.class),
    DISCARD_1_CATTLE_CARD_TO_GAIN_3_DOLLARS_AND_ADD_1_OBJECTIVE_CARD_TO_HAND(Action.Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand.class),
    ADD_1_OBJECTIVE_CARD_TO_HAND(Action.Add1ObjectiveCardToHand.class),
    DISCARD_1_CATTLE_CARD_TO_GAIN_1_CERTIFICATE(Action.Discard1CattleCardToGain1Certificate.class),
    DISCARD_1_DUTCH_BELT_TO_GAIN_2_DOLLARS(Action.Discard1DutchBeltToGain2Dollars.class),
    DISCARD_1_DUTCH_BELT_TO_GAIN_3_DOLLARS(Action.Discard1DutchBeltToGain3Dollars.class),
    DISCARD_1_GUERNSEY(Action.Discard1Guernsey.class),
    DISCARD_1_HOLSTEIN_TO_GAIN_10_DOLLARS(Action.Discard1HolsteinToGain10Dollars.class),
    DISCARD_1_JERSEY_TO_GAIN_1_CERTIFICATE(Action.Discard1JerseyToGain1Certificate.class),
    DISCARD_1_JERSEY_TO_GAIN_1_CERTIFICATE_AND_2_DOLLARS(Action.Discard1JerseyToGain1CertificateAnd2Dollars.class),
    DISCARD_1_JERSEY_TO_GAIN_2_CERTIFICATES(Action.Discard1JerseyToGain2Certificates.class),
    DISCARD_1_JERSEY_TO_GAIN_2_DOLLARS(Action.Discard1JerseyToGain2Dollars.class),
    DISCARD_1_JERSEY_TO_GAIN_4_DOLLARS(Action.Discard1JerseyToGain4Dollars.class),
    DISCARD_1_JERSEY_TO_MOVE_ENGINE_1_FORWARD(Action.Discard1JerseyToMoveEngine1Forward.class),
    DISCARD_1_OBJECTIVE_CARD_TO_GAIN_2_CERTIFICATES(Action.Discard1ObjectiveCardToGain2Certificates.class),
    DISCARD_1_GUERNSEY_TO_GAIN_4_DOLLARS(Action.Discard1GuernseyToGain4Dollars.class),
    DISCARD_PAIR_TO_GAIN_3_DOLLARS(Action.DiscardPairToGain3Dollars.class),
    DISCARD_PAIR_TO_GAIN_4_DOLLARS(Action.DiscardPairToGain4Dollars.class),
    DRAW_CARD(Action.DrawCard.class),
    DRAW_2_CARDS(Action.Draw2Cards.class),
    DRAW_3_CARDS(Action.Draw3Cards.class),
    DRAW_4_CARDS(Action.Draw4Cards.class),
    DRAW_5_CARDS(Action.Draw5Cards.class),
    DRAW_6_CARDS(Action.Draw6Cards.class),
    DRAW_2_CATTLE_CARDS(Action.Draw2CattleCards.class),
    EXTRAORDINARY_DELIVERY(Action.ExtraordinaryDelivery.class),
    GAIN_1_CERTIFICATE(Action.Gain1Certificate.class),
    GAIN_1_DOLLAR(Action.Gain1Dollar.class),
    GAIN_2_DOLLARS_PER_BUILDING_IN_WOODS(Action.Gain2DollarsPerBuildingInWoods.class),
    GAIN_1_DOLLAR_PER_ENGINEER(Action.Gain1DollarPerEngineer.class),
    GAIN_2_CERTIFICATES_AND_2_DOLLARS_PER_TEEPEE_PAIR(Action.Gain2CertificatesAnd2DollarsPerTeepeePair.class),
    GAIN_2_DOLLARS(Action.Gain2Dollars.class),
    GAIN_4_DOLLARS(Action.Gain4Dollars.class),
    HIRE_WORKER_MINUS_1(Action.HireWorkerMinus1.class),
    HIRE_WORKER_MINUS_2(Action.HireWorkerMinus2.class),
    HIRE_WORKER_PLUS_2(Action.HireWorkerPlus2.class),
    HIRE_WORKER(Action.HireWorker.class),
    MAX_CERTIFICATES(Action.MaxCertificates.class),
    MOVE(Action.Move.class),
    MOVE_1_FORWARD(Action.Move1Forward.class),
    MOVE_2_FORWARD(Action.Move2Forward.class),
    MOVE_3_FORWARD(Action.Move3Forward.class),
    MOVE_3_FORWARD_WITHOUT_FEES(Action.Move3ForwardWithoutFees.class),
    MOVE_4_FORWARD(Action.Move4Forward.class),
    MOVE_5_FORWARD(Action.Move5Forward.class),
    MOVE_ENGINE_1_FORWARD(Action.MoveEngine1Forward.class),
    MOVE_ENGINE_1_BACKWARDS_TO_GAIN_3_DOLLARS(Action.MoveEngine1BackwardsToGain3Dollars.class),
    MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD(Action.MoveEngine1BackwardsToRemove1Card.class),
    MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS(Action.MoveEngine2BackwardsToRemove2Cards.class),
    MOVE_ENGINE_2_OR_3_FORWARD(Action.MoveEngine2Or3Forward.class),
    MOVE_ENGINE_AT_LEAST_1_BACKWARDS_AND_GAIN_3_DOLLARS(Action.MoveEngineAtLeast1BackwardsAndGain3Dollars.class),
    MOVE_ENGINE_AT_MOST_2_FORWARD(Action.MoveEngineAtMost2Forward.class),
    MOVE_ENGINE_AT_MOST_3_FORWARD(Action.MoveEngineAtMost3Forward.class),
    MOVE_ENGINE_AT_MOST_4_FORWARD(Action.MoveEngineAtMost4Forward.class),
    MOVE_ENGINE_FORWARD(Action.MoveEngineForward.class),
    MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_BUILDINGS_IN_WOODS(Action.MoveEngineForwardUpToNumberOfBuildingsInWoods.class),
    PAY_1_DOLLAR_AND_MOVE_ENGINE_1_BACKWARDS_TO_GAIN_1_CERTIFICATE(Action.Pay1DollarAndMoveEngine1BackwardsToGain1Certificate.class),
    PAY_1_DOLLAR_TO_MOVE_ENGINE_1_FORWARD(Action.Pay1DollarToMoveEngine1Forward.class),
    PAY_2_DOLLARS_AND_MOVE_ENGINE_2_BACKWARDS_TO_GAIN_2_CERTIFICATES(Action.Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates.class),
    PAY_2_DOLLARS_TO_MOVE_ENGINE_2_FORWARD(Action.Pay2DollarsToMoveEngine2Forward.class),
    PLACE_BUILDING(Action.PlaceBuilding.class),
    PLACE_CHEAP_BUILDING(Action.PlaceCheapBuilding.class),
    PLAY_OBJECTIVE_CARD(Action.PlayObjectiveCard.class),
    REMOVE_CARD(Action.RemoveCard.class),
    REMOVE_HAZARD(Action.RemoveHazard.class),
    REMOVE_HAZARD_FOR_5_DOLLARS(Action.RemoveHazardFor5Dollars.class),
    REMOVE_HAZARD_FOR_FREE(Action.RemoveHazardForFree.class),
    SINGLE_AUXILIARY_ACTION(Action.SingleAuxiliaryAction.class),
    SINGLE_OR_DOUBLE_AUXILIARY_ACTION(Action.SingleOrDoubleAuxiliaryAction.class),
    TAKE_OBJECTIVE_CARD(Action.TakeObjectiveCard.class),
    TRADE_WITH_TRIBES(Action.TradeWithTribes.class),
    UPGRADE_ANY_STATION_BEHIND_ENGINE(Action.UpgradeAnyStationBehindEngine.class),
    UPGRADE_STATION(Action.UpgradeStation.class),
    USE_ADJACENT_BUILDING(Action.UseAdjacentBuilding.class),
    CHOOSE_FORESIGHTS(Action.ChooseForesights.class),
    UNLOCK_BLACK_OR_WHITE(Action.UnlockBlackOrWhite.class),
    UNLOCK_WHITE(Action.UnlockWhite.class),
    DOWNGRADE_STATION(Action.DowngradeStation.class);

    @Getter
    Class<? extends Action> action;

    public static ActionType of(Class<? extends Action> action) {
        for (ActionType value : values()) {
            if (value.action.equals(action)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No enum constant for action: " + action);
    }

    public static Action toAction(JsonObject jsonObject, Game game) {
        var type = getEnum(jsonObject, "type", ActionType.class);

        switch (type) {
            case APPOINT_STATION_MASTER:
                return new Action.AppointStationMaster(getEnum(jsonObject, JsonProperties.WORKER, Worker.class));
            case BUY_CATTLE:
                return new Action.BuyCattle(findCattleCards(game, getJsonArray(jsonObject, JsonProperties.CATTLE_CARDS)), getEnum(jsonObject, "costPreference", CattleMarket.CostPreference.class));
            case DELIVER_TO_CITY:
                return new Action.DeliverToCity(getEnum(jsonObject, JsonProperties.CITY, City.class), getInt(jsonObject, JsonProperties.CERTIFICATES));
            case DISCARD_1_BLACK_ANGUS_TO_GAIN_2_CERTIFICATES:
                return new Action.Discard1BlackAngusToGain2Certificates();
            case DISCARD_1_BLACK_ANGUS_TO_GAIN_2_DOLLARS:
                return new Action.Discard1BlackAngusToGain2Dollars();
            case DISCARD_CARD:
                return new Action.DiscardCard(findCardInHand(game.currentPlayerState().getHand(), getJsonObject(jsonObject, JsonProperties.CARD)));
            case DISCARD_1_CATTLE_CARD_TO_GAIN_3_DOLLARS_AND_ADD_1_OBJECTIVE_CARD_TO_HAND:
                return new Action.Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand(getEnum(jsonObject, JsonProperties.CATTLE_TYPE, CattleType.class));
            case ADD_1_OBJECTIVE_CARD_TO_HAND:
                return jsonObject.containsKey(JsonProperties.OBJECTIVE_CARD)
                        ? new Action.Add1ObjectiveCardToHand(findObjectiveCard(game, getJsonObject(jsonObject, JsonProperties.OBJECTIVE_CARD)))
                        : new Action.Add1ObjectiveCardToHand();
            case DISCARD_1_CATTLE_CARD_TO_GAIN_1_CERTIFICATE:
                return new Action.Discard1CattleCardToGain1Certificate(getEnum(jsonObject, JsonProperties.CATTLE_TYPE, CattleType.class));
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
                return new Action.Discard1ObjectiveCardToGain2Certificates(findObjectiveCardInHand(game.currentPlayerState().getHand(), getJsonObject(jsonObject, JsonProperties.OBJECTIVE_CARD)));
            case DISCARD_1_GUERNSEY_TO_GAIN_4_DOLLARS:
                return new Action.Discard1GuernseyToGain4Dollars();
            case DISCARD_PAIR_TO_GAIN_3_DOLLARS:
                return new Action.DiscardPairToGain3Dollars(getEnum(jsonObject, JsonProperties.CATTLE_TYPE, CattleType.class));
            case DISCARD_PAIR_TO_GAIN_4_DOLLARS:
                return new Action.DiscardPairToGain4Dollars(getEnum(jsonObject, JsonProperties.CATTLE_TYPE, CattleType.class));
            case DRAW_CARD:
                return new Action.DrawCard();
            case DRAW_2_CARDS:
                return new Action.Draw2Cards();
            case DRAW_3_CARDS:
                return new Action.Draw3Cards();
            case DRAW_4_CARDS:
                return new Action.Draw4Cards();
            case DRAW_5_CARDS:
                return new Action.Draw5Cards();
            case DRAW_6_CARDS:
                return new Action.Draw6Cards();
            case DRAW_2_CATTLE_CARDS:
                return new Action.Draw2CattleCards();
            case EXTRAORDINARY_DELIVERY:
                return new Action.ExtraordinaryDelivery(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)), getEnum(jsonObject, JsonProperties.CITY, City.class));
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
            case HIRE_WORKER_MINUS_1:
                return new Action.HireWorkerMinus1(getInt(jsonObject, JsonProperties.ROW), getEnum(jsonObject, JsonProperties.WORKER, Worker.class));
            case HIRE_WORKER_MINUS_2:
                return new Action.HireWorkerMinus2(getInt(jsonObject, JsonProperties.ROW), getEnum(jsonObject, JsonProperties.WORKER, Worker.class));
            case HIRE_WORKER_PLUS_2:
                return new Action.HireWorkerPlus2(getInt(jsonObject, JsonProperties.ROW), getEnum(jsonObject, JsonProperties.WORKER, Worker.class));
            case HIRE_WORKER:
                return new Action.HireWorker(getInt(jsonObject, JsonProperties.ROW), getEnum(jsonObject, JsonProperties.WORKER, Worker.class));
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
            case MOVE_5_FORWARD:
                return new Action.Move5Forward(getSteps(jsonObject, game));
            case MOVE_ENGINE_1_FORWARD:
                return new Action.MoveEngine1Forward(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case MOVE_ENGINE_1_BACKWARDS_TO_GAIN_3_DOLLARS:
                return new Action.MoveEngine1BackwardsToGain3Dollars(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD:
                return new Action.MoveEngine1BackwardsToRemove1Card(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS:
                return new Action.MoveEngine2BackwardsToRemove2Cards(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case MOVE_ENGINE_2_OR_3_FORWARD:
                return new Action.MoveEngine2Or3Forward(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case MOVE_ENGINE_AT_LEAST_1_BACKWARDS_AND_GAIN_3_DOLLARS:
                return new Action.MoveEngineAtLeast1BackwardsAndGain3Dollars(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case MOVE_ENGINE_AT_MOST_2_FORWARD:
                return new Action.MoveEngineAtMost2Forward(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case MOVE_ENGINE_AT_MOST_3_FORWARD:
                return new Action.MoveEngineAtMost3Forward(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case MOVE_ENGINE_AT_MOST_4_FORWARD:
                return new Action.MoveEngineAtMost4Forward(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case MOVE_ENGINE_FORWARD:
                return new Action.MoveEngineForward(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_BUILDINGS_IN_WOODS:
                return new Action.MoveEngineForwardUpToNumberOfBuildingsInWoods(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case PAY_1_DOLLAR_AND_MOVE_ENGINE_1_BACKWARDS_TO_GAIN_1_CERTIFICATE:
                return new Action.Pay1DollarAndMoveEngine1BackwardsToGain1Certificate(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case PAY_1_DOLLAR_TO_MOVE_ENGINE_1_FORWARD:
                return new Action.Pay1DollarToMoveEngine1Forward(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case PAY_2_DOLLARS_AND_MOVE_ENGINE_2_BACKWARDS_TO_GAIN_2_CERTIFICATES:
                return new Action.Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case PAY_2_DOLLARS_TO_MOVE_ENGINE_2_FORWARD:
                return new Action.Pay2DollarsToMoveEngine2Forward(findSpace(game, getJsonObject(jsonObject, JsonProperties.TO)));
            case PLACE_BUILDING:
                return new Action.PlaceBuilding((Location.BuildingLocation) game.getTrail().getLocation(getString(jsonObject, JsonProperties.LOCATION)), findPlayerBuilding(game, getString(jsonObject, JsonProperties.BUILDING)));
            case PLACE_CHEAP_BUILDING:
                return new Action.PlaceCheapBuilding((Location.BuildingLocation) game.getTrail().getLocation(getString(jsonObject, JsonProperties.LOCATION)), findPlayerBuilding(game, getString(jsonObject, JsonProperties.BUILDING)));
            case PLAY_OBJECTIVE_CARD:
                return new Action.PlayObjectiveCard(findObjectiveCardInHand(game.currentPlayerState().getHand(), getJsonObject(jsonObject, JsonProperties.OBJECTIVE_CARD)));
            case REMOVE_CARD:
                return new Action.RemoveCard(findCardInHand(game.currentPlayerState().getHand(), getJsonObject(jsonObject, JsonProperties.CARD)));
            case REMOVE_HAZARD:
                return new Action.RemoveHazard((Location.HazardLocation) game.getTrail().getLocation(getString(jsonObject, JsonProperties.LOCATION)));
            case REMOVE_HAZARD_FOR_5_DOLLARS:
                return new Action.RemoveHazardFor5Dollars((Location.HazardLocation) game.getTrail().getLocation(getString(jsonObject, JsonProperties.LOCATION)));
            case REMOVE_HAZARD_FOR_FREE:
                return new Action.RemoveHazardForFree((Location.HazardLocation) game.getTrail().getLocation(getString(jsonObject, JsonProperties.LOCATION)));
            case SINGLE_AUXILIARY_ACTION:
                return new Action.SingleAuxiliaryAction();
            case SINGLE_OR_DOUBLE_AUXILIARY_ACTION:
                return new Action.SingleOrDoubleAuxiliaryAction();
            case TAKE_OBJECTIVE_CARD:
                return jsonObject.containsKey(JsonProperties.OBJECTIVE_CARD)
                        ? new Action.TakeObjectiveCard(findObjectiveCard(game, getJsonObject(jsonObject, JsonProperties.OBJECTIVE_CARD)))
                        : new Action.TakeObjectiveCard();
            case TRADE_WITH_TRIBES:
                return new Action.TradeWithTribes(getInt(jsonObject, JsonProperties.REWARD));
            case UPGRADE_ANY_STATION_BEHIND_ENGINE:
                return new Action.UpgradeAnyStationBehindEngine(game.getRailroadTrack().getStations().get(getInt(jsonObject, JsonProperties.STATION)));
            case UPGRADE_STATION:
                return new Action.UpgradeStation();
            case USE_ADJACENT_BUILDING:
                return new Action.UseAdjacentBuilding(game.getTrail()
                        .getBuildingLocation(getString(jsonObject, JsonProperties.LOCATION))
                        .orElseThrow(() -> new JsonException("No such location")));
            case CHOOSE_FORESIGHTS:
                return new Action.ChooseForesights(getJsonArray(jsonObject, JsonProperties.CHOICES).stream()
                        .map(jsonValue -> (JsonNumber) jsonValue)
                        .map(JsonNumber::intValue)
                        .collect(Collectors.toList()));
            case UNLOCK_BLACK_OR_WHITE:
                return new Action.UnlockBlackOrWhite(getEnum(jsonObject, JsonProperties.UNLOCK, Unlockable.class));
            case UNLOCK_WHITE:
                return new Action.UnlockWhite(getEnum(jsonObject, JsonProperties.UNLOCK, Unlockable.class));
            case DOWNGRADE_STATION:
                return new Action.DowngradeStation(findStation(game, getInt(jsonObject, JsonProperties.STATION)));
            default:
                return null;
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
        int points = getInt(jsonObject, JsonProperties.POINTS);

        return game.getObjectiveCards().getAvailable().stream()
                .filter(objectiveCard -> objectiveCard.getPoints() == points)
                .filter(objectiveCard -> objectiveCard.getTasks().size() == tasks.size() && objectiveCard.getTasks().containsAll(tasks))
                .findAny()
                .orElseThrow(() -> new JsonException("Objective card not available"));
    }

    private static PlayerBuilding findPlayerBuilding(Game game, String building) {
        return game.currentPlayerState().getBuildings().stream()
                .filter(playerBuilding -> playerBuilding.getName().equals(building))
                .findAny()
                .orElseThrow(() -> new JsonException("Building not available"));
    }

    private static RailroadTrack.Space findSpace(Game game, JsonObject jsonObject) {
        if (jsonObject.containsKey(JsonProperties.NUMBER) && jsonObject.get(JsonProperties.NUMBER).getValueType() == JsonValue.ValueType.NUMBER) {
            int number = getInt(jsonObject, JsonProperties.NUMBER);
            return game.getRailroadTrack().getSpace(number);
        } else {
            return game.getRailroadTrack().getTurnouts().get(getInt(jsonObject, JsonProperties.TURNOUT));
        }
    }

    private static Card.CattleCard findCattleCardInHand(Collection<Card> hand, JsonObject jsonObject) {
        CattleType type = getEnum(jsonObject, JsonProperties.TYPE, CattleType.class);

        return hand.stream()
                .filter(card -> card instanceof Card.CattleCard)
                .map(card -> (Card.CattleCard) card)
                .filter(cattleCard -> cattleCard.getType() == type)
                .findAny()
                .orElseThrow(() -> new JsonException("Cattle card not in hand"));
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
        int points = getInt(jsonObject, JsonProperties.POINTS);

        return hand.stream()
                .filter(card -> card instanceof ObjectiveCard)
                .map(card -> (ObjectiveCard) card)
                .filter(objectiveCard -> objectiveCard.getPoints() == points)
                .filter(objectiveCard -> containsExactlyInAnyOrder(objectiveCard.getTasks(), tasks))
                .findAny()
                .orElseThrow(() -> new JsonException("Objective card not in hand"));
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

    private static List<Card.CattleCard> findCattleCards(Game game, JsonArray cattleCards) {
        var available = new HashSet<>(game.getCattleMarket().getMarket());

        return cattleCards.stream()
                .map(JsonValue::asJsonObject)
                .map(jsonObject -> {
                    CattleType type = getEnum(jsonObject, JsonProperties.TYPE, CattleType.class);
                    int points = getInt(jsonObject, JsonProperties.POINTS);

                    var card = available.stream()
                            .filter(cattleCard -> cattleCard.getType() == type)
                            .filter(cattleCard -> cattleCard.getPoints() == points)
                            .findAny()
                            .orElseThrow(() -> new JsonException("Cattle card not available"));

                    available.remove(card);

                    return card;
                })
                .collect(Collectors.toList());
    }

    private static String getString(JsonObject jsonObject, String key) {
        JsonValue jsonValue = getValue(jsonObject, key);

        if (jsonValue.getValueType() != JsonValue.ValueType.STRING) {
            throw new JsonException("Property '" + key + "' expected to be string, but was: " + jsonValue.getValueType() + " in JSON object: " + jsonObject);
        }

        return ((JsonString) jsonValue).getString();
    }

    private static int getInt(JsonObject jsonObject, String key) {
        JsonValue jsonValue = getValue(jsonObject, key);

        if (jsonValue.getValueType() != JsonValue.ValueType.NUMBER) {
            throw new JsonException("Property '" + key + "' expected to be number, but was: " + jsonValue.getValueType() + " in JSON object: " + jsonObject);
        }

        return ((JsonNumber) jsonValue).intValue();
    }

    private static <E extends Enum<E>> E getEnum(JsonObject jsonObject, String key, Class<E> enumType) {
        try {
            return Enum.valueOf(enumType, getString(jsonObject, key));
        } catch (IllegalArgumentException e) {
            throw new JsonException("Property '" + key + "' invalid in JSON object: " + jsonObject);
        }
    }

    private static List<JsonString> getJsonStrings(JsonObject jsonObject, String key) {
        return getJsonArray(jsonObject, key).getValuesAs(JsonString.class);
    }

    private static JsonArray getJsonArray(JsonObject jsonObject, String key) {
        JsonValue jsonValue = getValue(jsonObject, key);

        if (jsonValue.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new JsonException("Property '" + key + "' expected to be array, but was: " + jsonValue.getValueType() + " in JSON object: " + jsonObject);
        }

        return jsonValue.asJsonArray();
    }

    private static JsonObject getJsonObject(JsonObject jsonObject, String key) {
        JsonValue jsonValue = getValue(jsonObject, key);

        if (jsonValue.getValueType() != JsonValue.ValueType.OBJECT) {
            throw new JsonException("Property '" + key + "' expected to be object, but was: " + jsonValue.getValueType() + " in JSON object: " + jsonObject);
        }

        return jsonValue.asJsonObject();
    }

    private static JsonValue getValue(JsonObject jsonObject, String key) {
        var jsonValue = jsonObject.get(key);
        if (jsonValue == null) {
            throw new JsonException("Property '" + key + "' missing in JSON object: " + jsonObject);
        }
        return jsonValue;
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
        private static final String ROW = "row";
    }

}
