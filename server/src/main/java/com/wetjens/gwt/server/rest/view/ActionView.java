package com.wetjens.gwt.server.rest.view;

import com.wetjens.gwt.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.json.*;
import javax.ws.rs.BadRequestException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@AllArgsConstructor
public enum ActionView {

    APPOINT_STATION_MASTER(Action.AppointStationMaster.class, (jsonObject, game) -> new Action.AppointStationMaster(Worker.valueOf(jsonObject.getString("worker")))),
    BUY_CATTLE(Action.BuyCattle.class, (jsonObject, game) -> new Action.BuyCattle(findCattleCards(game, jsonObject.getJsonArray("cattleCards")))),
    DELIVER_TO_CITY(Action.DeliverToCity.class, (jsonObject, game) -> new Action.DeliverToCity(City.valueOf(jsonObject.getString("city")), jsonObject.getInt("certificates"), Unlockable.valueOf(jsonObject.getString("unlock")))),
    DISCARD_1_BLACK_ANGUS_TO_GAIN_2_CERTIFICATES(Action.Discard1BlackAngusToGain2Certificates.class, (jsonObject, game) -> new Action.Discard1BlackAngusToGain2Certificates()),
    DISCARD_1_BLACK_ANGUS_TO_GAIN_2_DOLLARS(Action.Discard1BlackAngusToGain2Dollars.class, (jsonObject, game) -> new Action.Discard1BlackAngusToGain2Dollars()),
    DISCARD_1_CARD(Action.DiscardCards.Discard1Card.class, ((jsonObject, game) -> new Action.DiscardCards.Discard1Card(findCard(game, jsonObject.getJsonObject("card"))))),
    DISCARD_1_CATTLE_CARD_TO_GAIN_3_DOLLARS_AND_ADD_1_OBJECTIVE_CARD_TO_HAND(Action.Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand.class, (((jsonObject, game) -> new Action.Discard1CattleCardToGain3DollarsAndAdd1ObjectiveCardToHand(findCattleCard(game, jsonObject.getJsonObject("cattleCard")), findObjectiveCardInHand(game, jsonObject.getJsonObject("objectiveCard")))))),
    DISCARD_1_DUTCH_BELT_TO_GAIN_2_DOLLARS(Action.Discard1DutchBeltToGain2Dollars.class, (jsonObject, game) -> new Action.Discard1DutchBeltToGain2Dollars()),
    DISCARD_1_DUTCH_BELT_TO_GAIN_3_DOLLARS(Action.Discard1DutchBeltToGain3Dollars.class, (jsonObject, game) -> new Action.Discard1DutchBeltToGain3Dollars()),
    DISCARD_1_GUERNSEY(Action.Discard1Guernsey.class, ((jsonObject, game) -> new Action.Discard1Guernsey())),
    DISCARD_1_HOLSTEIN_TO_GAIN_10_DOLLARS(Action.Discard1HolsteinToGain10Dollars.class, (jsonObject, game) -> new Action.Discard1HolsteinToGain10Dollars()),
    DISCARD_1_JERSEY_TO_GAIN_1_CERTIFICATE(Action.Discard1JerseyToGain1Certificate.class, (jsonObject, game) -> new Action.Discard1JerseyToGain1Certificate()),
    DISCARD_1_JERSEY_TO_GAIN_2_CERTIFICATES(Action.Discard1JerseyToGain2Certificates.class, (jsonObject, game) -> new Action.Discard1JerseyToGain2Certificates()),
    DISCARD_1_JERSEY_TO_GAIN_2_DOLLARS(Action.Discard1JerseyToGain2Dollars.class, (jsonObject, game) -> new Action.Discard1JerseyToGain2Dollars()),
    DISCARD_1_JERSEY_TO_GAIN_4_DOLLARS(Action.Discard1JerseyToGain4Dollars.class, (jsonObject, game) -> new Action.Discard1JerseyToGain4Dollars()),
    DISCARD_1_JERSEY_TO_MOVE_ENGINE_1_FORWARD(Action.Discard1JerseyToMoveEngine1Forward.class, (jsonObject, game) -> new Action.Discard1JerseyToMoveEngine1Forward(findSpace(game, jsonObject.getJsonObject("to")))),
    DISCARD_1_OBJECTIVE_CARD_TO_GAIN_2_CERTIFICATES(Action.Discard1ObjectiveCardToGain2Certificates.class, (jsonObject, game) -> new Action.Discard1ObjectiveCardToGain2Certificates(findObjectiveCardInHand(game, jsonObject.getJsonObject("objectiveCard")))),
    DISCARD_2_CARDS(Action.DiscardCards.Discard2Cards.class, (jsonObject, game) -> new Action.DiscardCards.Discard2Cards(findCards(game, jsonObject.getJsonArray("cards")))),
    DISCARD_2_GUERNSEY_TO_GAIN_4_DOLLARS(Action.Discard2GuernseyToGain4Dollars.class, (jsonObject, game) -> new Action.Discard2GuernseyToGain4Dollars()),
    DISCARD_3_CARDS(Action.DiscardCards.Discard3Cards.class, (jsonObject, game) -> new Action.DiscardCards.Discard3Cards(findCards(game, jsonObject.getJsonArray("cards")))),
    DISCARD_PAIR_TO_GAIN_3_DOLLARS(Action.DiscardPairToGain3Dollars.class, (jsonObject, game) -> new Action.DiscardPairToGain3Dollars(CattleType.valueOf(jsonObject.getString("cattleType")))),
    DISCARD_PAIR_TO_GAIN_4_DOLLARS(Action.DiscardPairToGain4Dollars.class, (jsonObject, game) -> new Action.DiscardPairToGain4Dollars(CattleType.valueOf(jsonObject.getString("cattleType")))),
    DRAW_1_CARD_THEN_DISCARD_1_CARD(Action.DrawCardsThenDiscardCards.Draw1CardThenDiscard1Card.class, (jsonObject, game) -> new Action.DrawCardsThenDiscardCards.Draw1CardThenDiscard1Card()),
    DRAW_2_CARDS_THEN_DISCARD_2_CARDS(Action.DrawCardsThenDiscardCards.Draw2CardsThenDiscard2Cards.class, (jsonObject, game) -> new Action.DrawCardsThenDiscardCards.Draw2CardsThenDiscard2Cards()),
    DRAW_2_CATTLE_CARDS(Action.Draw2CattleCards.class, ((jsonObject, game) -> new Action.SingleAuxiliaryAction())),
    DRAW_CARDS_UP_TO_NUMBER_OF_COWBOYS_THEN_DISCARD_CARDS(Action.DrawCardsUpToNumberOfCowboysThenDiscardCards.class, (jsonObject, game) -> new Action.DrawCardsUpToNumberOfCowboysThenDiscardCards()),
    DRAW_UP_TO_1_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo1CardsThenDiscardCards.class, (jsonObject, game) -> new Action.DrawCardsThenDiscardCards.DrawUpTo1CardsThenDiscardCards(jsonObject.getInt("amount"))),
    DRAW_UP_TO_2_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo2CardsThenDiscardCards.class, (jsonObject, game) -> new Action.DrawCardsThenDiscardCards.DrawUpTo2CardsThenDiscardCards(jsonObject.getInt("amount"))),
    DRAW_UP_TO_3_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo3CardsThenDiscardCards.class, (jsonObject, game) -> new Action.DrawCardsThenDiscardCards.DrawUpTo3CardsThenDiscardCards(jsonObject.getInt("amount"))),
    DRAW_UP_TO_4_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo4CardsThenDiscardCards.class, (jsonObject, game) -> new Action.DrawCardsThenDiscardCards.DrawUpTo4CardsThenDiscardCards(jsonObject.getInt("amount"))),
    DRAW_UP_TO_5_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo5CardsThenDiscardCards.class, (jsonObject, game) -> new Action.DrawCardsThenDiscardCards.DrawUpTo5CardsThenDiscardCards(jsonObject.getInt("amount"))),
    DRAW_UP_TO_6_CARDS_THEN_DISCARD_CARDS(Action.DrawCardsThenDiscardCards.DrawUpTo6CardsThenDiscardCards.class, (jsonObject, game) -> new Action.DrawCardsThenDiscardCards.DrawUpTo6CardsThenDiscardCards(jsonObject.getInt("amount"))),
    EXTRAORDINARY_DELIVERY(Action.ExtraordinaryDelivery.class, (jsonObject, game) -> new Action.ExtraordinaryDelivery(findSpace(game, jsonObject.getJsonObject("to")), City.valueOf(jsonObject.getString("city")), Unlockable.valueOf(jsonObject.getString("unlock")))),
    GAIN_1_CERTIFICATE(Action.Gain1Certificate.class, ((jsonObject, game) -> new Action.Gain1Certificate())),
    GAIN_1_DOLLAR(Action.Gain1Dollar.class, ((jsonObject, game) -> new Action.Gain1Dollar())),
    GAIN_1_DOLLAR_PER_BUILDING_IN_WOODS(Action.Gain1DollarPerBuildingInWoods.class, ((jsonObject, game) -> new Action.Gain1DollarPerBuildingInWoods())),
    GAIN_1_DOLLAR_PER_ENGINEER(Action.Gain1DollarPerEngineer.class, ((jsonObject, game) -> new Action.Gain1DollarPerEngineer())),
    GAIN_2_CERTIFICATES_AND_2_DOLLARS_PER_TEEPEE_PAIR(Action.Gain2CertificatesAnd2DollarsPerTeepeePair.class, ((jsonObject, game) -> new Action.Gain2CertificatesAnd2DollarsPerTeepeePair())),
    GAIN_2_DOLLARS(Action.Gain2Dollars.class, ((jsonObject, game) -> new Action.Gain2Dollars())),
    GAIN_4_DOLLARS(Action.Gain4Dollars.class, ((jsonObject, game) -> new Action.Gain4Dollars())),
    HIRE_CHEAP_WORKER(Action.HireCheapWorker.class, (jsonObject, game) -> new Action.HireCheapWorker(Worker.valueOf(jsonObject.getString("worker")))),
    HIRE_SECOND_WORKER(Action.HireSecondWorker.class, (jsonObject, game) -> new Action.HireSecondWorker(Worker.valueOf(jsonObject.getString("worker")))),
    HIRE_WORKER(Action.HireWorker.class, (jsonObject, game) -> new Action.HireWorker(Worker.valueOf(jsonObject.getString("worker")))),
    MAX_CERTIFICATES(Action.MaxCertificates.class, ((jsonObject, game) -> new Action.SingleAuxiliaryAction())),
    MOVE(Action.Move.class, ((jsonObject, game) -> new Action.Move(getJsonStrings(jsonObject, "steps").stream()
            .map(JsonString::getString)
            .map(game.getTrail()::getLocation)
            .collect(Collectors.toList())))),
    MOVE_1_FORWARD(Action.Move1Forward.class, (jsonObject, game) -> new Action.Move1Forward(game.getTrail().getLocation(jsonObject.getString("to")))),
    MOVE_2_FORWARD(Action.Move2Forward.class, (jsonObject, game) -> new Action.Move2Forward(game.getTrail().getLocation(jsonObject.getString("to")))),
    MOVE_3_FORWARD(Action.Move3Forward.class, (jsonObject, game) -> new Action.Move3Forward(game.getTrail().getLocation(jsonObject.getString("to")))),
    MOVE_3_FORWARD_WITHOUT_FEES(Action.Move3ForwardWithoutFees.class, (jsonObject, game) -> new Action.Move3ForwardWithoutFees(game.getTrail().getLocation(jsonObject.getString("to")))),
    MOVE_4_FORWARD(Action.Move4Forward.class, (jsonObject, game) -> new Action.Move4Forward(game.getTrail().getLocation(jsonObject.getString("to")))),
    MOVE_ENGINE_1_BACKWARDS_TO_GAIN_3_DOLLARS(Action.MoveEngine1BackwardsToGain3Dollars.class, (jsonObject, game) -> new Action.MoveEngine1BackwardsToGain3Dollars(findSpace(game, jsonObject.getJsonObject("to")))),
    MOVE_ENGINE_1_BACKWARDS_TO_REMOVE_1_CARD(Action.MoveEngine1BackwardsToRemove1Card.class, (jsonObject, game) -> new Action.MoveEngine1BackwardsToRemove1Card(findSpace(game, jsonObject.getJsonObject("to")))),
    MOVE_ENGINE_2_BACKWARDS_TO_REMOVE_2_CARDS(Action.MoveEngine2BackwardsToRemove2Cards.class, (jsonObject, game) -> new Action.MoveEngine2BackwardsToRemove2Cards(findSpace(game, jsonObject.getJsonObject("to")))),
    MOVE_ENGINE_2_OR_3_FORWARD(Action.MoveEngine2Or3Forward.class, (jsonObject, game) -> new Action.MoveEngine2Or3Forward(findSpace(game, jsonObject.getJsonObject("to")))),
    MOVE_ENGINE_AT_LEAST_1_BACKWARDS_AND_GAIN_3_DOLLARS(Action.MoveEngineAtLeast1BackwardsAndGain3Dollars.class, (jsonObject, game) -> new Action.MoveEngineAtLeast1BackwardsAndGain3Dollars(findSpace(game, jsonObject.getJsonObject("to")))),
    MOVE_ENGINE_AT_MOST_2_FORWARD(Action.MoveEngineAtMost2Forward.class, (jsonObject, game) -> new Action.MoveEngineAtMost2Forward(findSpace(game, jsonObject.getJsonObject("to")))),
    MOVE_ENGINE_AT_MOST_3_FORWARD(Action.MoveEngineAtMost3Forward.class, (jsonObject, game) -> new Action.MoveEngineAtMost3Forward(findSpace(game, jsonObject.getJsonObject("to")))),
    MOVE_ENGINE_AT_MOST_4_FORWARD(Action.MoveEngineAtMost4Forward.class, (jsonObject, game) -> new Action.MoveEngineAtMost4Forward(findSpace(game, jsonObject.getJsonObject("to")))),
    MOVE_ENGINE_AT_MOST_5_FORWARD(Action.MoveEngineAtMost5Forward.class, (jsonObject, game) -> new Action.MoveEngineAtMost5Forward(findSpace(game, jsonObject.getJsonObject("to")))),
    MOVE_ENGINE_FORWARD(Action.MoveEngineForward.class, (jsonObject, game) -> new Action.MoveEngineForward(findSpace(game, jsonObject.getJsonObject("to")))),
    MOVE_ENGINE_FORWARD_UP_TO_NUMBER_OF_BUILDINGS_IN_WOODS(Action.MoveEngineForwardUpToNumberOfBuildingsInWoods.class, (jsonObject, game) -> new Action.MoveEngineForwardUpToNumberOfBuildingsInWoods(findSpace(game, jsonObject.getJsonObject("to")))),
    PAY_1_DOLLAR_AND_MOVE_ENGINE_1_BACKWARDS_TO_GAIN_1_CERTIFICATE(Action.Pay1DollarAndMoveEngine1BackwardsToGain1Certificate.class, (jsonObject, game) -> new Action.Pay1DollarAndMoveEngine1BackwardsToGain1Certificate(findSpace(game, jsonObject.getJsonObject("to")))),
    PAY_1_DOLLAR_TO_MOVE_ENGINE_1_FORWARD(Action.Pay1DollarToMoveEngine1Forward.class, (jsonObject, game) -> new Action.Pay1DollarToMoveEngine1Forward(findSpace(game, jsonObject.getJsonObject("to")))),
    PAY_2_DOLLARS_AND_MOVE_ENGINE_2_BACKWARDS_TO_GAIN_2_CERTIFICATES(Action.Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates.class, (jsonObject, game) -> new Action.Pay2DollarsAndMoveEngine2BackwardsToGain2Certificates(findSpace(game, jsonObject.getJsonObject("to")))),
    PAY_2_DOLLARS_TO_MOVE_ENGINE_2_FORWARD(Action.Pay2DollarsToMoveEngine2Forward.class, ((jsonObject, game) -> new Action.Pay2DollarsToMoveEngine2Forward(findSpace(game, jsonObject.getJsonObject("to"))))),
    PLACE_BUILDING(Action.PlaceBuilding.class, (jsonObject, game) -> new Action.PlaceBuilding((Location.BuildingLocation) game.getTrail().getLocation(jsonObject.getString("location")), findPlayerBuilding(game, jsonObject.getString("building")))),
    PLACE_CHEAP_BUILDING(Action.PlaceCheapBuilding.class, (jsonObject, game) -> new Action.PlaceCheapBuilding((Location.BuildingLocation) game.getTrail().getLocation(jsonObject.getString("location")), findPlayerBuilding(game, jsonObject.getString("building")))),
    PLAY_OBJECTIVE_CARD(Action.PlayObjectiveCard.class, (jsonObject, game) -> new Action.PlayObjectiveCard(findObjectiveCardInHand(game, jsonObject.getJsonObject("objectiveCard")))),
    REMOVE_1_CARD(Action.Remove1Card.class, (jsonObject, game) -> new Action.Remove1Card(findCard(game, jsonObject.getJsonObject("card")))),
    REMOVE_2_CARDS(Action.Remove2Cards.class, (jsonObject, game) -> new Action.Remove2Cards(findCards(game, jsonObject.getJsonArray("cards")))),
    REMOVE_HAZARD(Action.RemoveHazard.class, (jsonObject, game) -> new Action.RemoveHazard(findHazard(game, jsonObject.getJsonObject("hazard")))),
    REMOVE_HAZARD_FOR_5_DOLLARS(Action.RemoveHazardFor5Dollars.class, (jsonObject, game) -> new Action.RemoveHazardFor5Dollars(findHazard(game, jsonObject.getJsonObject("hazard")))),
    REMOVE_HAZARD_FOR_FREE(Action.RemoveHazardForFree.class, (jsonObject, game) -> new Action.RemoveHazardForFree(findHazard(game, jsonObject.getJsonObject("hazard")))),
    SINGLE_AUXILIARY_ACTION(Action.SingleAuxiliaryAction.class, ((jsonObject, game) -> new Action.SingleAuxiliaryAction())),
    SINGLE_OR_DOUBLE_AUXILIARY_ACTION(Action.SingleOrDoubleAuxiliaryAction.class, ((jsonObject, game) -> new Action.SingleOrDoubleAuxiliaryAction())),
    TAKE_OBJECTIVE_CARD(Action.TakeObjectiveCard.class, (jsonObject, game) -> new Action.TakeObjectiveCard(findObjectiveCard(game, jsonObject.getJsonObject("objectiveCard")))),
    TRADE_WITH_INDIANS(Action.TradeWithIndians.class, (jsonObject, game) -> new Action.TradeWithIndians(jsonObject.getInt("cost"))),
    UPGRADE_ANY_STATION_BEHIND_ENGINE(Action.UpgradeAnyStationBehindEngine.class, (jsonObject, game) -> new Action.UpgradeAnyStationBehindEngine(game.getRailroadTrack().getStations().get(jsonObject.getInt("station")))),
    UPGRADE_STATION(Action.UpgradeStation.class, ((jsonObject, game) -> new Action.UpgradeStation())),
    USE_ADJACENT_BUILDING(Action.UseAdjacentBuilding.class, (jsonObject, game) -> new Action.UseAdjacentBuilding()),
    CHOOSE_FORESIGHTS(Action.ChooseForesights.class, (jsonObject, game) -> new Action.ChooseForesights(jsonObject.getJsonArray("choices").stream()
            .map(JsonValue::asJsonObject)
            .map(choice -> new Action.ChooseForesights.Choice(choice.getInt("row")))
            .collect(Collectors.toList())));

    private static ObjectiveCard findObjectiveCard(Game game, JsonObject jsonObject) {
        Set<ObjectiveCard.Task> tasks = getJsonStrings(jsonObject, "tasks").stream()
                .map(JsonString::getString)
                .map(ObjectiveCard.Task::valueOf)
                .collect(Collectors.toSet());
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

    private static Set<Card> findCards(Game game, JsonArray jsonArray) {
        return jsonArray.stream()
                .map(JsonValue::asJsonObject)
                .map(jsonObject -> findCard(game, jsonObject))
                .collect(Collectors.toSet());
    }

    private static RailroadTrack.Space findSpace(Game game, JsonObject jsonObject) {
        if (jsonObject.containsKey("number")) {
            int number = jsonObject.getInt("number");
            return game.getRailroadTrack().getSpace(number);
        } else {
            return game.getRailroadTrack().getTurnouts().get(jsonObject.getInt("turnout"));
        }
    }

    private static Card.CattleCard findCattleCard(Game game, JsonObject jsonObject) {
        CattleType type = CattleType.valueOf(jsonObject.getString("type"));
        int points = jsonObject.getInt("points");

        return game.currentPlayerState().getHand().stream()
                .filter(card -> card instanceof Card.CattleCard)
                .map(card -> (Card.CattleCard) card)
                .filter(cattleCard -> cattleCard.getType() == type)
                .filter(cattleCard -> cattleCard.getPoints() == points)
                .findAny()
                .orElseThrow(() -> new BadRequestException("Cattle card not in hand: " + type + " " + points));
    }

    private static Card findCard(Game game, JsonObject jsonObject) {
        if (jsonObject.containsKey("type")) {
            return findCattleCard(game, jsonObject);
        } else {
            return findObjectiveCardInHand(game, jsonObject);
        }
    }

    private static ObjectiveCard findObjectiveCardInHand(Game game, JsonObject jsonObject) {
        Set<ObjectiveCard.Task> tasks = getJsonStrings(jsonObject, "tasks").stream()
                .map(JsonString::getString)
                .map(ObjectiveCard.Task::valueOf)
                .collect(Collectors.toSet());
        int points = jsonObject.getInt("points");

        return game.currentPlayerState().getHand().stream()
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

    static ActionView of(Class<? extends Action> action) {
        for (ActionView value : values()) {
            if (value.action.equals(action)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported action: " + action);
    }

    private static List<JsonString> getJsonStrings(JsonObject jsonObject, String key) {
        JsonArray jsonArray = jsonObject.getJsonArray(key);
        if (jsonArray == null) {
            throw new JsonException("Property missing: " + key);
        }
        return jsonArray.getValuesAs(JsonString.class);
    }

    public Action deserialize(JsonObject jsonObject, Game game) {
        return deserializer.apply(jsonObject, game);
    }
}
