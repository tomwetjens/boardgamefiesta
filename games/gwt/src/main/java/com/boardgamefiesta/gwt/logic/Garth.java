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

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.*;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Garth {

    @NonNull
    private final Player player;
    @NonNull
    private final Queue<GarthAction> drawStack;
    private final List<GarthAction> discardPile;
    private final Difficulty difficulty;

    @Getter
    private Worker specialization;

    Score adjustScore(Score score, GWT game, PlayerState playerState) {
        return score.set(ScoreCategory.DOLLARS, 0)
                .set(ScoreCategory.EXTRA_STEP_POINTS, 0)
                .set(ScoreCategory.STATION_MASTERS, 0)
                .set(ScoreCategory.OBJECTIVE_CARDS, Stream.concat(
                                playerState.getCommittedObjectives().stream(),
                                playerState.getOptionalObjectives())
                        .mapToInt(ObjectiveCard::getPoints)
                        .sum())
                .set(ScoreCategory.CITIES, score.getCategories().getOrDefault(ScoreCategory.CITIES, 0) - game.getRailroadTrack().scoreSanFrancisco(player, playerState)
                        + game.getRailroadTrack().numberOfDeliveries(player, City.SAN_FRANCISCO) * 6);
    }

    void start(GWT game, Random random) {
        var playerState = game.playerState(player);

        switch (difficulty) {
            case MEDIUM:
                playerState.addWorker(specialization);
                break;

            case HARD:
                var startWorker = randomWorker(random);
                playerState.addWorker(startWorker);

                var secondStartWorker = randomDifferentWorker(random, startWorker);
                playerState.addWorker(secondStartWorker);

                // Do not remove all these workers from the supply
                // because there will be too little to trigger the end of the game
                break;

            case VERY_HARD:
                playerState.addWorker(Worker.COWBOY);
                playerState.addWorker(Worker.CRAFTSMAN);
                playerState.addWorker(Worker.ENGINEER);

                var fourthStartWorker = randomWorker(random);
                playerState.addWorker(fourthStartWorker);

                // Do not remove all these workers from the supply
                // because there will be too little to trigger the end of the game

                break;
        }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public enum Difficulty {

        EASY(List.of(City.WICHITA, City.ST_LOUIS), List.of(City.CHICAGO)),
        MEDIUM(List.of(City.COLORADO_SPRINGS, City.BLOOMINGTON), List.of(City.CHICAGO, City.CLEVELAND)),
        HARD(List.of(City.ALBUQUERQUE, City.CHICAGO_2), List.of(City.CLEVELAND)),
        VERY_HARD(List.of(City.ALBUQUERQUE, City.CHICAGO_2), List.of(City.CLEVELAND));

        List<City> startCities;
        List<City> startCitiesRailsToTheNorth;

        List<City> getStartCities(GWT game) {
            return game.isRailsToTheNorth() ? startCitiesRailsToTheNorth : startCities;
        }
    }

    public static Garth create(@NonNull PlayerState playerState, Random random, Difficulty difficulty) {
        var deck = new ArrayList<>(Arrays.asList(GarthAction.values()));
        Collections.shuffle(deck, random);
        var drawStack = new LinkedList<>(deck);

        var specialization = randomWorker(random);

        return new Garth(playerState.getPlayer(), drawStack, new LinkedList<>(), difficulty, specialization);
    }

    private static Worker randomDifferentWorker(Random random, Worker differentThan) {
        Worker worker;
        do {
            worker = randomWorker(random);
        } while (worker == differentThan);
        return worker;
    }

    private static Worker randomWorker(Random random) {
        var workers = Worker.values();
        return workers[random.nextInt(workers.length)];
    }

    JsonObjectBuilder serialize(JsonBuilderFactory factory) {
        return factory.createObjectBuilder()
                .add("drawStack", JsonSerializer.forFactory(factory).fromStrings(drawStack, GarthAction::name))
                .add("discardPile", JsonSerializer.forFactory(factory).fromStrings(discardPile, GarthAction::name))
                .add("difficulty", difficulty.name())
                .add("specialization", specialization.name());
    }

    static Garth deserialize(Player player, JsonObject jsonObject) {
        return new Garth(
                player,
                jsonObject.getJsonArray("drawStack").stream()
                        .map(JsonString.class::cast)
                        .map(JsonString::getString)
                        .map(GarthAction::valueOf)
                        .collect(Collectors.toCollection(LinkedList::new)),
                jsonObject.getJsonArray("discardPile").stream()
                        .map(JsonString.class::cast)
                        .map(JsonString::getString)
                        .map(GarthAction::valueOf)
                        .collect(Collectors.toCollection(LinkedList::new)),
                Difficulty.valueOf(jsonObject.getString("difficulty")),
                Worker.valueOf(jsonObject.getString("specialization")));
    }

    public void execute(GWT game, Random random) {
        var playerState = game.playerState(player);
        var possibleActions = game.possibleActions();

        if (possibleActions.contains(Action.PlaceBid.class)) {
            if (game.canSkip()) {
                game.skip(random);
            } else {
                game.perform(new Action.PlaceBid(lowestBidPossible(game)), random);
            }
        } else if (possibleActions.contains(Action.Move.class)) {
            if (drawStack.isEmpty()) {
                Collections.shuffle(discardPile, random);
                drawStack.addAll(discardPile);

                discardPile.clear();
            }
            var action = drawStack.poll();

            game.perform(new Action.Move(calculateMove(game, action.getSteps(game, player))), random);

            action.perform(this, game, player, random);

            if (!game.getTrail().atKansasCity(player)) {
                // Garth ignores all normal location actions
                game.getActionStack().clear();
            }

            discardPile.add(action);
        } else if (possibleActions.contains(Action.ChooseForesight1.class)) {
            game.perform(new Action.ChooseForesight1(chooseForesight(game.getForesights().choices(0), random)), random);
        } else if (possibleActions.contains(Action.ChooseForesight2.class)) {
            game.perform(new Action.ChooseForesight2(chooseForesight(game.getForesights().choices(1), random)), random);
        } else if (possibleActions.contains(Action.ChooseForesight3.class)) {
            game.perform(new Action.ChooseForesight3(chooseForesight(game.getForesights().choices(2), random)), random);
        } else if (possibleActions.contains(Action.DeliverToCity.class)) {
            game.perform(new Action.DeliverToCity(calculateDelivery(game, player), 0), random);
        } else if (possibleActions.contains(Action.UnlockWhite.class)) {
            game.perform(new Action.UnlockWhite(randomWhiteDisc(playerState, game)), random);
        } else if (possibleActions.contains(Action.UnlockBlackOrWhite.class)) {
            game.perform(new Action.UnlockBlackOrWhite(randomBlackOrWhiteDisc(playerState, game)), random);
        } else if (possibleActions.contains(Action.TakeObjectiveCard.class) && !game.getObjectiveCards().getAvailable().isEmpty()) {
            var objectiveCard = randomObjectiveCard(game, random);
            game.perform(new Action.TakeObjectiveCard(objectiveCard), random);
        } else if (possibleActions.contains(Action.TakeBonusStationMaster.class)) {
            var bonusStationMasters = new ArrayList<>(game.getRailroadTrack().getBonusStationMasters());
            game.perform(new Action.TakeBonusStationMaster(bonusStationMasters.get(random.nextInt(bonusStationMasters.size()))), random);
        } else if (possibleActions.contains(Action.GainExchangeToken.class)) {
            game.perform(new Action.GainExchangeToken(), random);
        } else {
            game.endTurn(player, random);
        }
    }

    private City calculateDelivery(GWT game, Player player) {
        var startCities = difficulty.getStartCities(game);
        var highestStartCity = startCities.get(startCities.size() - 1);

        return startCities.stream()
                .filter(city -> !game.getRailroadTrack().hasMadeDelivery(player, city))
                .filter(city -> game.getRailroadTrack().isAccessible(player, city))
                .findFirst()
                .orElseGet(() -> game.getRailroadTrack().possibleDeliveries(player, Integer.MAX_VALUE, 0)
                        .stream()
                        .map(RailroadTrack.PossibleDelivery::getCity)
                        .filter(city -> city.getValue() > highestStartCity.getValue())
                        .min(Comparator.comparingInt(City::getValue)
                                // Both SF and NYC are 18, so first do NYC and then do SF
                                .thenComparing(city -> city == City.NEW_YORK_CITY ? 1 : 0))
                        .orElse(game.getEdition() == GWT.Edition.SECOND ? City.NEW_YORK_CITY : City.SAN_FRANCISCO));
    }

    private ObjectiveCard randomObjectiveCard(GWT game, Random random) {
        var objectiveCards = new ArrayList<ObjectiveCard>(game.getObjectiveCards().getAvailable());
        return objectiveCards.get(random.nextInt(objectiveCards.size()));
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private enum GarthAction {

        GARTH_1((game, player) -> game.getPlayers().size() == 4 ? 3 : 1, Garth::hireCheapestWorkerOfAnyType),

        GARTH_2((game, player) -> 1, Garth::placeBranchletAndMoveEngineForward),

        GARTH_3((game, player) -> 1, Garth::placeBranchletAndTakeObjectiveCard),

        GARTH_4((game, player) -> game.getPlayers().size() == 2 ? 1 : 3, Garth::removeHighestValueTeepee),

        GARTH_5((game, player) -> game.getPlayers().size() == 2 ? 1 : 3, Garth::removeHighestHazardOfTypeWithMostHazards),

        GARTH_6((game, player) -> game.getPlayers().size() == 4 ? 3 : 1, Garth::hireSpecializedOrMostNumerous),

        GARTH_7((game, player) -> 2, Garth::placeBranchletAndMoveEngineForward),

        GARTH_8((game, player) -> 2, Garth::buyCattleCards),

        GARTH_9((game, player) -> 2, Garth::placeBranchletAndPlaceBuilding),

        GARTH_10((game, player) -> Math.max(1, highestNumberOfBuildingsAmongPlayers(game)), Garth::placeBuildingIfSpecializedInCraftsmen),

        GARTH_11((game, player) -> Math.max(1, highestNumberOfBuildingsAmongPlayers(game)), Garth::hireEngineerAndMoveEngineForwardIfSpecializedInEngineers),

        GARTH_12((game, player) -> game.playerState(player).getNumberOfCowboys(), Garth::drawCattleCardsAndBuyCattleCardsIfSpecializedInCowboys),

        GARTH_13((game, player) -> game.playerState(player).getNumberOfCowboys(), Garth::buyCattleCardsIfSpecializedInCowboys),

        GARTH_14(GarthAction::numberOfSpecializedWorkers, Garth::moveEngineForwardIfSpecializedInEngineers),

        GARTH_15(GarthAction::numberOfSpecializedWorkers, Garth::placeBuildingIfSpecializedInCraftsmen);

        private static int numberOfSpecializedWorkers(GWT game, Player player) {
            var playerState = game.playerState(player);
            return playerState.getNumberOfWorkers(playerState.getAutomaState().orElseThrow().specialization);
        }

        private static int highestNumberOfBuildingsAmongPlayers(GWT game) {
            return game.getPlayers().stream()
                    .mapToInt(player -> game.getTrail().getBuildings(player).size())
                    .max().orElse(0);
        }

        BiFunction<GWT, Player, Integer> steps;

        @Getter
        GarthActionLogic logic;

        int getSteps(GWT game, Player player) {
            return steps.apply(game, player);
        }

        void perform(Garth garth, GWT game, Player player, Random random) {
            logic.accept(garth, game, player, random);
        }

        @FunctionalInterface
        private interface GarthActionLogic {
            void accept(Garth garth, GWT game, Player player, Random random);
        }
    }

    private void drawCattleCardsAndBuyCattleCardsIfSpecializedInCowboys(GWT game, Player player, Random random) {
        if (specialization == Worker.COWBOY) {
            drawCattleCards(game);

            var remainingCowboys = game.playerState(player).getNumberOfCowboys() - 1;

            if (remainingCowboys > 0) {
                buyCattleCards(game, player, remainingCowboys);
            }
        }
    }

    private void buyCattleCardsIfSpecializedInCowboys(GWT game, Player player, Random random) {
        if (specialization == Worker.COWBOY) {
            buyCattleCards(game, player, random);
        }
    }

    private void drawCattleCards(GWT game) {
        game.getCattleMarket().draw();
        game.getCattleMarket().draw();

        game.fireActionEvent(Action.Draw2CattleCards.class, Collections.emptyList());
    }

    private void placeBuildingIfSpecializedInCraftsmen(GWT game, Player player, Random random) {
        if (specialization == Worker.CRAFTSMAN) {
            placeBuilding(game, player);
        }
    }

    private void hireEngineerAndMoveEngineForwardIfSpecializedInEngineers(GWT game, Player player, Random random) {
        if (specialization == Worker.ENGINEER) {
            hireEngineer(game, player);

            moveEngineForward(game, player);
        }
    }

    private void hireEngineer(GWT game, Player player) {
        var playerState = game.playerState(player);

        if (playerState.hasMaxWorkers(Worker.ENGINEER)) {
            return;
        }

        var jobMarket = game.getJobMarket();

        jobMarket.getCheapestRow(EnumSet.of(Worker.ENGINEER))
                .ifPresent(rowIndex -> {
                    jobMarket.takeWorker(rowIndex, Worker.ENGINEER);
                    playerState.addWorker(Worker.ENGINEER);

                    var cost = JobMarket.getCost(rowIndex);
                    game.fireActionEvent(Action.HireWorker.class, List.of(Worker.ENGINEER.name(), Integer.toString(cost)));

                    redetermineSpecialization(playerState);
                });
    }

    private void moveEngineForwardIfSpecializedInEngineers(GWT game, Player player, Random random) {
        if (specialization == Worker.ENGINEER) {
            moveEngineForward(game, player);
        }
    }

    private void placeBranchletAndPlaceBuilding(GWT game, Player player, Random random) {
        if (game.isRailsToTheNorth()) {
            placeBranchlet(game, player, random);
        }

        placeBuilding(game, player);
    }

    private void placeBuilding(GWT game, Player player) {
        var playerCount = game.getPlayers().size();
        var playerState = game.playerState(player);

        var newBuildLocation = firstEmptyNonRiskBuildingLocation(game.getTrail().getCurrentLocation(game.getNextPlayer())
                .orElse(game.getTrail().getStart()));

        var options = playerState.getBuildings().stream()
                .filter(building -> building.getCraftsmen() <= playerState.getNumberOfCraftsmen())
                .flatMap(building -> Stream.concat(
                        // Build a new building on an empty location
                        newBuildLocation.map(location -> new BuildOption(building.getCraftsmen(), building, location)).stream(),

                        // All possible upgrades of existing buildings with this building
                        game.getTrail().getBuildingLocations().stream()
                                .filter(buildingLocation -> buildingLocation.getBuilding()
                                        .filter(otherBuilding -> otherBuilding instanceof PlayerBuilding)
                                        .map(PlayerBuilding.class::cast)
                                        .filter(playerBuilding -> player.equals(playerBuilding.getPlayer()))
                                        .map(playerBuilding -> playerBuilding.getCraftsmen() < building.getCraftsmen())
                                        .orElse(false))
                                .map(buildingLocation -> new BuildOption(building.getCraftsmen() - buildingLocation.getBuilding()
                                        .map(PlayerBuilding.class::cast)
                                        .map(PlayerBuilding::getCraftsmen)
                                        .orElse(0), building, buildingLocation))
                ))
                .collect(Collectors.toList());

        options.stream()
                // Pick one of the options, maximize use of craftsmen
                .max(Comparator.comparingInt(BuildOption::getCraftsmen)
                        // Upgrade highest value building
                        .thenComparing(build -> build.getLocation().getBuilding()
                                .map(PlayerBuilding.class::cast)
                                .map(PlayerBuilding::getPoints)
                                .orElse(0))
                        // Maximize overall fees
                        .thenComparing(build -> build.getBuilding().getHand().getFee(playerCount) -
                                build.getLocation().getBuilding()
                                        .map(PlayerBuilding.class::cast)
                                        .map(PlayerBuilding::getHand)
                                        .map(hand -> hand.getFee(playerCount))
                                        .orElse(0)))
                .ifPresent(option -> {
                    var building = option.getBuilding();
                    var location = option.getLocation();

                    game.fireActionEvent(Action.PlaceBuilding.class, List.of(building.getName(), location.getName(), Integer.toString(0)));

                    playerState.removeBuilding(building);
                    location.placeBuilding(building);
                });
    }

    private Optional<Location.BuildingLocation> firstEmptyNonRiskBuildingLocation(Location location) {
        Queue<Location> unvisited = new LinkedList<>();
        unvisited.add(location);

        Location current;
        while ((current = unvisited.poll()) != null) {
            if (current.isEmpty() && current instanceof Location.BuildingLocation) {
                return Optional.of((Location.BuildingLocation) current);
            }
            unvisited.addAll(current.getNext());
        }

        return Optional.empty();
    }

    @Value
    private static class BuildOption {
        int craftsmen;
        PlayerBuilding building;
        Location.BuildingLocation location;
    }

    private void buyCattleCards(GWT game, Player player, Random random) {
        buyCattleCards(game, player, game.playerState(player).getNumberOfCowboys());
    }

    private void buyCattleCards(GWT game, Player player, int numberOfCowboys) {
        var playerState = game.playerState(player);

        game.getCattleMarket().possibleBuys(numberOfCowboys, 8)
                .flatMap(possibleBuy -> bestCattleCards(game, possibleBuy).stream())
                .max(Comparator.comparingInt(Buy::getPoints))
                .ifPresent(buy -> {
                    game.getCattleMarket().buy(buy.getCard(), buy.getSecondCard(), buy.getCowboys(), buy.getDollars());

                    playerState.gainCard(buy.getCard());

                    if (buy.getSecondCard() != null) {
                        playerState.gainCard(buy.getSecondCard());

                        game.fireEvent(player, GWTEvent.Type.BUY_2_CATTLE, List.of(
                                Integer.toString(buy.getDollars()),
                                buy.getCard().getType().name(),
                                Integer.toString(buy.getCard().getPoints()),
                                buy.getSecondCard().getType().name(),
                                Integer.toString(buy.getSecondCard().getPoints()),
                                Integer.toString(buy.getCowboys())));
                    } else {
                        game.fireEvent(player, GWTEvent.Type.BUY_CATTLE, List.of(
                                Integer.toString(buy.getDollars()),
                                buy.getCard().getType().name(),
                                Integer.toString(buy.getCard().getPoints()),
                                Integer.toString(buy.getCowboys())));
                    }
                });
    }

    @Value
    private static class Buy {
        Card.CattleCard card;
        Card.CattleCard secondCard;
        int points;
        int cowboys;
        int dollars;
    }

    private Optional<Buy> bestCattleCards(GWT game, CattleMarket.PossibleBuy possibleBuy) {
        return game.getCattleMarket().getMarket().stream()
                .filter(cattleCard -> cattleCard.getValue() == possibleBuy.getBreedingValue())
                .max(Comparator.comparingInt(Card.CattleCard::getPoints))
                .map(card -> possibleBuy.isPair()
                        ? game.getCattleMarket().getMarket().stream()
                        .filter(cattleCard -> cattleCard != card)
                        .filter(cattleCard -> cattleCard.getValue() == possibleBuy.getBreedingValue())
                        .max(Comparator.comparingInt(Card.CattleCard::getPoints))
                        .map(secondCard -> new Buy(card, secondCard, card.getPoints() + secondCard.getPoints(), possibleBuy.getCowboys(), possibleBuy.getDollars()))
                        .orElseThrow()
                        : new Buy(card, null, card.getPoints(), possibleBuy.getCowboys(), possibleBuy.getDollars()));
    }

    private void removeHighestHazardOfTypeWithMostHazards(GWT game, Player player, Random random) {
        mostNumerousHazardType(game)
                .flatMap(mostNumerousHazardType -> highestPointsHazard(game, mostNumerousHazardType))
                .ifPresent(hazardLocation -> {
                    var hazard = hazardLocation.removeHazard();
                    game.playerState(player).addHazard(hazard);
                    game.fireActionEvent(Action.RemoveHazard.class, List.of(hazard.getType().name(), Integer.toString(hazard.getPoints()), Integer.toString(0)));
                });
    }

    private Optional<HazardType> mostNumerousHazardType(GWT game) {
        return Arrays.stream(HazardType.values())
                .flatMap(hazardType -> game.getTrail().getHazardLocations(hazardType).stream())
                .flatMap(hazardLocation -> hazardLocation.getHazard().stream())
                .collect(Collectors.groupingBy(Hazard::getType, Collectors.counting())).entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey);
    }

    private Optional<Location.HazardLocation> highestPointsHazard(GWT game, HazardType mostNumerousHazardType) {
        return game.getTrail().getHazardLocations(mostNumerousHazardType).stream()
                .max(Comparator.comparingInt(hazardLocation -> hazardLocation.getHazard()
                        .map(Hazard::getPoints)
                        .orElse(0)));
    }

    private void removeHighestValueTeepee(GWT game, Player player, Random random) {
        highestValueTeepee(game)
                .ifPresent(teepeeLocation -> {
                    var teepee = teepeeLocation.removeTeepee();
                    game.playerState(player).addTeepee(teepee);
                    game.fireActionEvent(Action.TradeWithTribes.class, List.of(Integer.toString(teepeeLocation.getReward())));
                });
    }

    private Optional<Location.TeepeeLocation> highestValueTeepee(GWT game) {
        return game.getTrail().getTeepeeLocations().stream()
                .filter(teepeeLocation -> !teepeeLocation.isEmpty())
                .max(Comparator.comparing(Location.TeepeeLocation::getReward));
    }

    private void placeBranchletAndTakeObjectiveCard(GWT game, Player player, Random random) {
        if (game.isRailsToTheNorth()) {
            placeBranchlet(game, player, random);
        }

        takeObjectiveCard(game, player);
    }

    private void takeObjectiveCard(GWT game, Player player) {
        var objectiveCards = game.getObjectiveCards();

        if (!objectiveCards.isEmpty()) {
            var objectiveCard = objectiveCards.getAvailable().iterator().next();
            objectiveCards.remove(objectiveCard);

            game.playerState(player).gainCard(objectiveCard);
        }
    }

    private void placeBranchletAndMoveEngineForward(GWT game, Player player, Random random) {
        if (game.isRailsToTheNorth()) {
            placeBranchlet(game, player, random);
        }

        moveEngineForward(game, player);
    }

    private void moveEngineForward(GWT game, Player player) {
        var atMost = game.playerState(player).getNumberOfEngineers();
        var railroadTrack = game.getRailroadTrack();

        railroadTrack.reachableSpacesForward(railroadTrack.currentSpace(player), 1, atMost)
                .stream()
                // Prefer stations for upgrading, go as far as possible
                .max(Comparator.<RailroadTrack.Space, Integer>comparing(space -> space.isTurnout()
                                && !railroadTrack.hasUpgraded(((RailroadTrack.Space.Turnout) space).getStation(), player) ? 1 : 0)
                        .thenComparing(RailroadTrack.Space::getName))
                .ifPresent(to -> {
                    railroadTrack.moveEngineForward(player, to, 1, atMost);

                    game.fireEvent(player, GWTEvent.Type.MOVE_ENGINE_FORWARD, List.of(to.getName()));

                    upgradeIfPossible(game, player, to);

                    if (to == railroadTrack.getEnd()) {
                        //If his train reaches the end of the track and there are any open stations that he has not
                        // already upgraded, he will move back to the earliest such station and place a disk there now.
                        moveEngineAtLeast1Backwards(game, player);
                    }
                });
    }

    private void moveEngineAtLeast1Backwards(GWT game, Player player) {
        var railroadTrack = game.getRailroadTrack();

        railroadTrack.reachableSpacesBackwards(railroadTrack.currentSpace(player), 1, Integer.MAX_VALUE)
                .stream()
                .min(Comparator.<RailroadTrack.Space, Integer>comparing(space -> space.isTurnout()
                                && !railroadTrack.hasUpgraded(((RailroadTrack.Space.Turnout) space).getStation(), player) ? 0 : 1)
                        .thenComparing(RailroadTrack.Space::getName))
                .ifPresent(to -> {
                    railroadTrack.moveEngineBackwards(player, to, 1, Integer.MAX_VALUE);

                    game.fireEvent(player, GWTEvent.Type.MOVE_ENGINE_AT_LEAST_1_BACKWARDS_AND_GAIN_3_DOLLARS, List.of(to.getName()));

                    upgradeIfPossible(game, player, to);
                });
    }

    private void upgradeIfPossible(GWT game, Player player, RailroadTrack.Space space) {
        var railroadTrack = game.getRailroadTrack();

        if (space.isTurnout()) {
            Station station = ((RailroadTrack.Space.Turnout) space).getStation();

            if (!railroadTrack.hasUpgraded(station, player)) {
                railroadTrack.upgradeStation(game, station);

                game.fireEvent(player, GWTEvent.Type.UPGRADE_STATION, List.of(Integer.toString(station.getCost()), Integer.toString(station.getPoints())));

                railroadTrack.getStationMaster(station)
                        .ifPresent(stationMaster -> {
                            var worker = Worker.ENGINEER;
                            railroadTrack.appointStationMaster(game, station, worker);

                            game.fireActionEvent(Action.AppointStationMaster.class, List.of(worker.name(), stationMaster.name()));
                        });
            }
        }
    }

    private void placeBranchlet(GWT game, Player player, Random random) {
        var railroadTrack = game.getRailroadTrack();
        var playerState = game.playerState(player);

        if (playerState.getBranchlets() == 0) {
            return;
        }

        var possibleTowns = railroadTrack.possibleTowns(player)
                .collect(Collectors.toList());

        if (possibleTowns.isEmpty()) {
            return;
        }

        var possibleTownsMap = possibleTowns.stream()
                .collect(Collectors.toMap(RailroadTrack.Town::getName, Function.identity()));

        RailroadTrack.Town town = null;
        if (difficulty == Difficulty.EASY || difficulty == Difficulty.MEDIUM) {
            if (possibleTowns.contains(RailroadTrack.GREEN_STATION_TOWN)) {
                town = RailroadTrack.GREEN_STATION_TOWN;
            } else if (possibleTowns.contains(RailroadTrack.MEMPHIS)) {
                town = RailroadTrack.MEMPHIS;
            } else if (possibleTownsMap.containsKey("42")) {
                town = possibleTownsMap.get("42");
            } else if (possibleTowns.contains(RailroadTrack.RED_STATION_TOWN)) {
                town = RailroadTrack.RED_STATION_TOWN;
            } else if (possibleTownsMap.containsKey("50")) {
                town = possibleTownsMap.get("50");
            } else if (possibleTowns.contains(RailroadTrack.GREEN_BAY)) {
                town = RailroadTrack.GREEN_BAY;
            } else if (possibleTowns.contains(RailroadTrack.MILWAUKEE)) {
                town = RailroadTrack.MILWAUKEE;
            } else if (possibleTownsMap.containsKey("51")) {
                town = possibleTownsMap.get("51");
            } else if (possibleTownsMap.containsKey("54")) {
                town = possibleTownsMap.get("54");
            } else if (possibleTowns.contains(RailroadTrack.TORONTO)) {
                town = RailroadTrack.TORONTO;
            } else if (possibleTowns.contains(RailroadTrack.MINNEAPOLIS)) {
                town = RailroadTrack.MINNEAPOLIS;
            } else if (possibleTownsMap.containsKey("53")) {
                town = possibleTownsMap.get("53");
            } else if (possibleTowns.contains(RailroadTrack.MONTREAL)) {
                town = RailroadTrack.MONTREAL;
            }
        } else {
            if (possibleTowns.contains(RailroadTrack.GREEN_STATION_TOWN)) {
                town = RailroadTrack.GREEN_STATION_TOWN;
            } else if (possibleTowns.contains(RailroadTrack.MEMPHIS)) {
                town = RailroadTrack.MEMPHIS;
            } else if (possibleTownsMap.containsKey("42")) {
                town = possibleTownsMap.get("42");
            } else if (possibleTowns.contains(RailroadTrack.MONTREAL)) {
                town = RailroadTrack.MONTREAL;
            } else if (possibleTownsMap.containsKey("54")) {
                town = possibleTownsMap.get("54");
            } else if (possibleTowns.contains(RailroadTrack.TORONTO)) {
                town = RailroadTrack.TORONTO;
            } else if (possibleTownsMap.containsKey("51")) {
                town = possibleTownsMap.get("51");
            } else if (possibleTownsMap.containsKey("53")) {
                town = possibleTownsMap.get("53");
            } else if (possibleTowns.contains(RailroadTrack.MINNEAPOLIS)) {
                town = RailroadTrack.MINNEAPOLIS;
            } else if (possibleTowns.contains(RailroadTrack.GREEN_BAY)) {
                town = RailroadTrack.GREEN_BAY;
            } else if (possibleTowns.contains(RailroadTrack.RED_STATION_TOWN)) {
                town = RailroadTrack.RED_STATION_TOWN;
            } else if (possibleTownsMap.containsKey("50")) {
                town = possibleTownsMap.get("50");
            } else if (possibleTowns.contains(RailroadTrack.MILWAUKEE)) {
                town = RailroadTrack.MILWAUKEE;
            }
        }

        if (town != null) {
            var townName = town.getName();

            game.fireActionEvent(Action.PlaceBranchlet.class, List.of(townName));

            railroadTrack.placeBranchlet(game, town);
            playerState.removeBranchlet();

            railroadTrack.getStation(town)
                    .ifPresent(station -> {
                        if (!railroadTrack.hasUpgraded(station, player)) {
                            railroadTrack.upgradeStation(game, station);

                            game.fireActionEvent(Action.UpgradeStationTown.class, List.of(townName, Integer.toString(station.getCost())));
                        }
                    });
        }
    }

    private void hireCheapestWorkerOfAnyType(GWT game, Player player, Random random) {
        var playerState = game.playerState(player);

        var jobMarket = game.getJobMarket();
        var workersThatCanBeHired = playerState.getWorkersThatCanBeHired();

        jobMarket.getCheapestRow(workersThatCanBeHired)
                .ifPresent(rowIndex -> {
                    var row = jobMarket.getRow(rowIndex);

                    Worker worker;
                    if (row.contains(specialization)
                            && !playerState.hasMaxWorkers(specialization)) {
                        worker = specialization;
                    } else {
                        worker = row.stream()
                                .filter(w -> !playerState.hasMaxWorkers(w))
                                .findFirst()
                                .orElseThrow();
                    }

                    jobMarket.takeWorker(rowIndex, worker);
                    playerState.addWorker(worker);

                    var cost = JobMarket.getCost(rowIndex);
                    game.fireActionEvent(Action.HireWorker.class, List.of(worker.name(), Integer.toString(cost)));

                    redetermineSpecialization(playerState);
                });
    }

    private void redetermineSpecialization(PlayerState playerState) {
        Arrays.stream(Worker.values())
                .filter(worker -> worker != specialization
                        && playerState.getNumberOfWorkers(worker) > playerState.getNumberOfWorkers(specialization))
                .findFirst()
                .ifPresent(worker -> specialization = worker);
    }

    private void hireSpecializedOrMostNumerous(GWT game, Player player, Random random) {
        var playerState = game.playerState(player);

        var jobMarket = game.getJobMarket();

        jobMarket.getCheapestRow(EnumSet.of(specialization))
                .filter(rowIndex -> !playerState.hasMaxWorkers(specialization))
                .ifPresentOrElse(rowIndex -> {
                    jobMarket.takeWorker(rowIndex, specialization);
                    playerState.gainWorker(specialization, game);

                    var cost = JobMarket.getCost(rowIndex);
                    game.fireActionEvent(Action.HireWorker.class, List.of(specialization.name(), Integer.toString(cost)));

                    redetermineSpecialization(playerState);
                }, () -> {
                    jobMarket.getMostNumerous()
                            .ifPresent(worker -> {
                                if (!playerState.hasMaxWorkers(worker)) {
                                    var rowIndex = jobMarket.getCheapestRow(EnumSet.of(worker)).orElseThrow();

                                    jobMarket.takeWorker(rowIndex, worker);
                                    playerState.gainWorker(worker, game);

                                    var cost = JobMarket.getCost(rowIndex);
                                    game.fireActionEvent(Action.HireWorker.class, List.of(worker.name(), Integer.toString(cost)));

                                    redetermineSpecialization(playerState);
                                }
                            });
                });
    }

    private Unlockable randomWhiteDisc(PlayerState playerState, GWT game) {
        return Arrays.stream(Unlockable.values())
                .filter(unlockable -> unlockable.getDiscColor() == DiscColor.WHITE)
                .filter(unlockable -> playerState.canUnlock(unlockable, game))
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS));
    }

    private Unlockable randomBlackOrWhiteDisc(PlayerState playerState, GWT game) {
        return Arrays.stream(Unlockable.values())
                .filter(unlockable -> playerState.canUnlock(unlockable, game))
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS));
    }

    private Bid lowestBidPossible(GWT game) {
        var bids = game.getPlayers().stream()
                .map(game::playerState)
                .map(PlayerState::getBid)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        return IntStream.range(0, game.getPlayerOrder().size())
                .filter(position -> bids.stream().noneMatch(bid -> bid.getPosition() == position))
                .mapToObj(position -> new Bid(position, 0))
                .findFirst()
                .orElseGet(() -> bids.stream()
                        .max(Comparator.comparingInt(Bid::getPoints))
                        .map(bid -> new Bid(bid.getPosition(), bid.getPoints() + 1))
                        .orElse(new Bid(0, 0)));
    }

    private List<Location> calculateMove(GWT game, int steps) {
        if (game.getTrail().getCurrentLocation(player).isEmpty()) {
            // Start of trail
            return List.of(game.getTrail().getLocation("A"));
        }
        return game.possibleMoves(player, steps, true).stream()
                .min(Comparator.comparingInt(PossibleMove::getNumberOfSteps).reversed()
                        .thenComparingInt(PossibleMove::getCost))
                .map(PossibleMove::getSteps)
                .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS));
    }

    private int chooseForesight(List<KansasCitySupply.Tile> choices, Random random) {
        var index = random.nextInt(choices.size());
        if (choices.get(index) != null) {
            return index;
        }
        // Pick the other one (could be empty as well)
        return (index + 1) % 2;
    }


}
