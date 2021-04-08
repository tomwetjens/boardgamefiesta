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

    Score adjustScore(Score score, Game game, PlayerState playerState) {
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

    void start(Game game, Random random) {
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

        EASY(List.of(City.WICHITA), List.of(City.CHICAGO)),
        MEDIUM(List.of(City.COLORADO_SPRINGS), List.of(City.CHICAGO, City.CLEVELAND)),
        HARD(List.of(City.ALBUQUERQUE), List.of(City.CLEVELAND)),
        VERY_HARD(List.of(City.ALBUQUERQUE), List.of(City.CLEVELAND));

        List<City> startCities;
        List<City> startCitiesRailsToTheNorth;

        List<City> getStartCities(Game game) {
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

    public void execute(Game game, Random random) {
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
        } else {
            game.endTurn(player, random);
        }
    }

    private City calculateDelivery(Game game, Player player) {
        var startCities = difficulty.getStartCities(game);
        var highestStartCity = startCities.get(startCities.size() - 1);

        return startCities.stream()
                .filter(city -> !game.getRailroadTrack().hasMadeDelivery(player, city))
                .findFirst()
                .orElseGet(() -> game.getRailroadTrack().possibleDeliveries(player, Integer.MAX_VALUE, 0)
                        .stream()
                        .map(RailroadTrack.PossibleDelivery::getCity)
                        .filter(city -> city.getValue() > highestStartCity.getValue())
                        .min(Comparator.comparingInt(City::getValue)
                                // Both SF and NYC are 18, so first do NYC and then do SF
                                .thenComparing(city -> city == City.NEW_YORK_CITY ? 1 : 0))
                        .orElse(City.SAN_FRANCISCO));
    }

    private ObjectiveCard randomObjectiveCard(Game game, Random random) {
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

        private static int numberOfSpecializedWorkers(Game game, Player player) {
            var playerState = game.playerState(player);
            return playerState.getNumberOfWorkers(playerState.getAutomaState().orElseThrow().specialization);
        }

        private static int highestNumberOfBuildingsAmongPlayers(Game game) {
            return game.getPlayers().stream()
                    .mapToInt(player -> game.getTrail().getBuildings(player).size())
                    .max().orElse(0);
        }

        BiFunction<Game, Player, Integer> steps;

        @Getter
        GarthActionLogic logic;

        int getSteps(Game game, Player player) {
            return steps.apply(game, player);
        }

        void perform(Garth garth, Game game, Player player, Random random) {
            logic.accept(garth, game, player, random);
        }

        @FunctionalInterface
        private interface GarthActionLogic {
            void accept(Garth garth, Game game, Player player, Random random);
        }
    }

    private void drawCattleCardsAndBuyCattleCardsIfSpecializedInCowboys(Game game, Player player, Random random) {
        if (specialization == Worker.COWBOY) {
            drawCattleCards(game);

            var remainingCowboys = game.playerState(player).getNumberOfCowboys() - 1;

            if (remainingCowboys > 0) {
                buyCattleCards(game, player, remainingCowboys);
            }
        }
    }

    private void buyCattleCardsIfSpecializedInCowboys(Game game, Player player, Random random) {
        if (specialization == Worker.COWBOY) {
            buyCattleCards(game, player, random);
        }
    }

    private void drawCattleCards(Game game) {
        game.getCattleMarket().draw();
        game.getCattleMarket().draw();

        game.fireActionEvent(Action.Draw2CattleCards.class, Collections.emptyList());
    }

    private void placeBuildingIfSpecializedInCraftsmen(Game game, Player player, Random random) {
        if (specialization == Worker.CRAFTSMAN) {
            placeBuilding(game, player);
        }
    }

    private void hireEngineerAndMoveEngineForwardIfSpecializedInEngineers(Game game, Player player, Random random) {
        if (specialization == Worker.ENGINEER) {
            hireEngineer(game, player);

            moveEngineForward(game, player);
        }
    }

    private void hireEngineer(Game game, Player player) {
        var playerState = game.playerState(player);

        if (playerState.hasMaxWorkers(Worker.ENGINEER)) {
            return;
        }

        game.getJobMarket().getRows().stream()
                .limit(game.getJobMarket().getCurrentRowIndex())
                .filter(row -> row.getWorkers().contains(Worker.ENGINEER))
                .min(Comparator.comparingInt(JobMarket.Row::getCost))
                .ifPresent(row -> {
                    game.getJobMarket().takeWorker(game.getJobMarket().getRows().indexOf(row), Worker.ENGINEER);
                    playerState.addWorker(Worker.ENGINEER);

                    game.fireActionEvent(Action.HireWorker.class, List.of(Worker.ENGINEER.name(), Integer.toString(row.getCost())));

                    redetermineSpecialization(playerState);
                });
    }

    private void moveEngineForwardIfSpecializedInEngineers(Game game, Player player, Random random) {
        if (specialization == Worker.ENGINEER) {
            moveEngineForward(game, player);
        }
    }

    private void placeBranchletAndPlaceBuilding(Game game, Player player, Random random) {
        if (game.isRailsToTheNorth()) {
            placeBranchlet(game, player, random);
        }

        placeBuilding(game, player);
    }

    private void placeBuilding(Game game, Player player) {
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
                                        .filter(playerBuilding -> playerBuilding.getPlayer() == player)
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

    private void buyCattleCards(Game game, Player player, Random random) {
        buyCattleCards(game, player, game.playerState(player).getNumberOfCowboys());
    }

    private void buyCattleCards(Game game, Player player, int numberOfCowboys) {
        var playerState = game.playerState(player);

        game.getCattleMarket().possibleBuys(numberOfCowboys, 8)
                .flatMap(possibleBuy -> bestCattleCards(game, possibleBuy).stream())
                .max(Comparator.comparingInt(Buy::getPoints))
                .ifPresent(buy -> {
                    game.getCattleMarket().buy(buy.getCard(), buy.getSecondCard(), buy.getCowboys(), buy.getDollars());

                    playerState.gainCard(buy.getCard());

                    if (buy.getSecondCard() != null) {
                        playerState.gainCard(buy.getSecondCard());

                        game.fireActionEvent("BUY_2_CATTLE", List.of(
                                Integer.toString(buy.getDollars()),
                                buy.getCard().getType().name(),
                                Integer.toString(buy.getCard().getPoints()),
                                buy.getSecondCard().getType().name(),
                                Integer.toString(buy.getSecondCard().getPoints()),
                                Integer.toString(buy.getCowboys())));
                    } else {
                        game.fireActionEvent("BUY_CATTLE", List.of(
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

    private Optional<Buy> bestCattleCards(Game game, CattleMarket.PossibleBuy possibleBuy) {
        return game.getCattleMarket().getMarket().stream()
                .filter(cattleCard -> cattleCard.getType().getValue() == possibleBuy.getBreedingValue())
                .max(Comparator.comparingInt(Card.CattleCard::getPoints))
                .map(card -> possibleBuy.isPair()
                        ? game.getCattleMarket().getMarket().stream()
                        .filter(cattleCard -> cattleCard != card)
                        .filter(cattleCard -> cattleCard.getType().getValue() == possibleBuy.getBreedingValue())
                        .max(Comparator.comparingInt(Card.CattleCard::getPoints))
                        .map(secondCard -> new Buy(card, secondCard, card.getPoints() + secondCard.getPoints(), possibleBuy.getCowboys(), possibleBuy.getDollars()))
                        .orElseThrow()
                        : new Buy(card, null, card.getPoints(), possibleBuy.getCowboys(), possibleBuy.getDollars()));
    }

    private void removeHighestHazardOfTypeWithMostHazards(Game game, Player player, Random random) {
        mostNumerousHazardType(game)
                .flatMap(mostNumerousHazardType -> highestPointsHazard(game, mostNumerousHazardType))
                .ifPresent(hazardLocation -> {
                    var hazard = hazardLocation.removeHazard();
                    game.playerState(player).addHazard(hazard);
                    game.fireActionEvent(Action.RemoveHazard.class, List.of(hazard.getType().name(), Integer.toString(hazard.getPoints()), Integer.toString(0)));
                });
    }

    private Optional<HazardType> mostNumerousHazardType(Game game) {
        return Arrays.stream(HazardType.values())
                .flatMap(hazardType -> game.getTrail().getHazardLocations(hazardType).stream())
                .flatMap(hazardLocation -> hazardLocation.getHazard().stream())
                .collect(Collectors.groupingBy(Hazard::getType, Collectors.counting())).entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey);
    }

    private Optional<Location.HazardLocation> highestPointsHazard(Game game, HazardType mostNumerousHazardType) {
        return game.getTrail().getHazardLocations(mostNumerousHazardType).stream()
                .max(Comparator.comparingInt(hazardLocation -> hazardLocation.getHazard()
                        .map(Hazard::getPoints)
                        .orElse(0)));
    }

    private void removeHighestValueTeepee(Game game, Player player, Random random) {
        highestValueTeepee(game)
                .ifPresent(teepeeLocation -> {
                    var teepee = teepeeLocation.removeTeepee();
                    game.playerState(player).addTeepee(teepee);
                    game.fireActionEvent(Action.TradeWithTribes.class, List.of(Integer.toString(teepeeLocation.getReward())));
                });
    }

    private Optional<Location.TeepeeLocation> highestValueTeepee(Game game) {
        return game.getTrail().getTeepeeLocations().stream()
                .filter(teepeeLocation -> !teepeeLocation.isEmpty())
                .max(Comparator.comparing(Location.TeepeeLocation::getReward));
    }

    private void placeBranchletAndTakeObjectiveCard(Game game, Player player, Random random) {
        if (game.isRailsToTheNorth()) {
            placeBranchlet(game, player, random);
        }

        takeObjectiveCard(game, player);
    }

    private void takeObjectiveCard(Game game, Player player) {
        var objectiveCards = game.getObjectiveCards();

        if (!objectiveCards.isEmpty()) {
            var objectiveCard = objectiveCards.getAvailable().iterator().next();
            objectiveCards.remove(objectiveCard);

            game.playerState(player).gainCard(objectiveCard);
        }
    }

    private void placeBranchletAndMoveEngineForward(Game game, Player player, Random random) {
        if (game.isRailsToTheNorth()) {
            placeBranchlet(game, player, random);
        }

        moveEngineForward(game, player);
    }

    private void moveEngineForward(Game game, Player player) {
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

                    game.fireActionEvent(Action.MoveEngineForward.class, Collections.emptyList());

                    upgradeIfPossible(game, player, to);

                    if (to == railroadTrack.getEnd()) {
                        //If his train reaches the end of the track and there are any open stations that he has not
                        // already upgraded, he will move back to the earliest such station and place a disk there now.
                        moveEngineAtLeast1Backwards(game, player);
                    }
                });
    }

    private void moveEngineAtLeast1Backwards(Game game, Player player) {
        var railroadTrack = game.getRailroadTrack();

        railroadTrack.reachableSpacesBackwards(railroadTrack.currentSpace(player), 1, Integer.MAX_VALUE)
                .stream()
                .min(Comparator.<RailroadTrack.Space, Integer>comparing(space -> space.isTurnout()
                        && !railroadTrack.hasUpgraded(((RailroadTrack.Space.Turnout) space).getStation(), player) ? 0 : 1)
                        .thenComparing(RailroadTrack.Space::getName))
                .ifPresent(to -> {
                    railroadTrack.moveEngineBackwards(player, to, 1, Integer.MAX_VALUE);

                    game.fireActionEvent(Action.MoveEngineAtLeast1BackwardsAndGain3Dollars.class, Collections.emptyList());

                    upgradeIfPossible(game, player, to);
                });
    }

    private void upgradeIfPossible(Game game, Player player, RailroadTrack.Space space) {
        var railroadTrack = game.getRailroadTrack();

        if (space.isTurnout()) {
            Station station = ((RailroadTrack.Space.Turnout) space).getStation();

            if (!railroadTrack.hasUpgraded(station, player)) {
                railroadTrack.upgradeStation(game, station);

                game.fireActionEvent(Action.UpgradeStation.class, Collections.emptyList());

                railroadTrack.getStationMaster(station)
                        .ifPresent(stationMaster -> {
                            var worker = Worker.ENGINEER;
                            railroadTrack.appointStationMaster(game, station, worker);

                            game.fireActionEvent(Action.AppointStationMaster.class, List.of(worker.name(), stationMaster.name()));
                        });
            }
        }
    }

    private void placeBranchlet(Game game, Player player, Random random) {
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

    private void hireCheapestWorkerOfAnyType(Game game, Player player, Random random) {
        var playerState = game.playerState(player);

        game.getJobMarket().getRows().stream()
                .limit(game.getJobMarket().getCurrentRowIndex())
                .filter(row -> !row.getWorkers().isEmpty())
                .filter(row -> row.getWorkers().stream().anyMatch(worker -> !playerState.hasMaxWorkers(worker)))
                .min(Comparator.comparingInt(JobMarket.Row::getCost))
                .ifPresent(cheapestRowWithWorkers -> {
                    Worker worker;
                    if (cheapestRowWithWorkers.getWorkers().contains(specialization)
                            && !playerState.hasMaxWorkers(specialization)) {
                        worker = specialization;
                    } else {
                        worker = cheapestRowWithWorkers.getWorkers().stream()
                                .filter(w -> !playerState.hasMaxWorkers(w))
                                .findFirst()
                                .orElseThrow();
                    }

                    game.getJobMarket().takeWorker(game.getJobMarket().getRows().indexOf(cheapestRowWithWorkers), worker);
                    playerState.addWorker(worker);

                    game.fireActionEvent(Action.HireWorker.class, List.of(worker.name(), Integer.toString(cheapestRowWithWorkers.getCost())));

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

    private void hireSpecializedOrMostNumerous(Game game, Player player, Random random) {
        var playerState = game.playerState(player);

        game.getJobMarket().getRows().stream()
                .limit(game.getJobMarket().getCurrentRowIndex())
                .filter(row -> row.getWorkers().contains(specialization) && !playerState.hasMaxWorkers(specialization))
                .min(Comparator.comparingInt(JobMarket.Row::getCost))
                .ifPresentOrElse(cheapestRowWithSpecialization -> {
                    game.getJobMarket().takeWorker(game.getJobMarket().getRows().indexOf(cheapestRowWithSpecialization), specialization);
                    playerState.gainWorker(specialization, game);

                    game.fireActionEvent(Action.HireWorker.class, List.of(specialization.name(), Integer.toString(cheapestRowWithSpecialization.getCost())));

                    redetermineSpecialization(playerState);
                }, () -> {
                    var workerCounts = game.getJobMarket().getRows().stream()
                            .limit(game.getJobMarket().getCurrentRowIndex())
                            .map(JobMarket.Row::getWorkers)
                            .flatMap(Collection::stream)
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

                    workerCounts.entrySet().stream()
                            .max(Comparator.comparingLong(Map.Entry::getValue))
                            .map(Map.Entry::getKey)
                            .ifPresent(mostNumerousWorker -> {
                                if (!playerState.hasMaxWorkers(mostNumerousWorker)) {
                                    var cheapestRow = game.getJobMarket().getRows().stream()
                                            .limit(game.getJobMarket().getCurrentRowIndex())
                                            .filter(row -> row.getWorkers().contains(mostNumerousWorker))
                                            .min(Comparator.comparingInt(JobMarket.Row::getCost))
                                            .orElseThrow();

                                    game.getJobMarket().takeWorker(game.getJobMarket().getRows().indexOf(cheapestRow), mostNumerousWorker);
                                    playerState.gainWorker(mostNumerousWorker, game);

                                    game.fireActionEvent(Action.HireWorker.class, List.of(mostNumerousWorker.name(), Integer.toString(cheapestRow.getCost())));

                                    redetermineSpecialization(playerState);
                                }
                            });
                });
    }

    private Unlockable randomWhiteDisc(PlayerState playerState, Game game) {
        return Arrays.stream(Unlockable.values())
                .filter(unlockable -> unlockable.getDiscColor() == DiscColor.WHITE)
                .filter(unlockable -> playerState.canUnlock(unlockable, game))
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS));
    }

    private Unlockable randomBlackOrWhiteDisc(PlayerState playerState, Game game) {
        return Arrays.stream(Unlockable.values())
                .filter(unlockable -> playerState.canUnlock(unlockable, game))
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS));
    }

    private Bid lowestBidPossible(Game game) {
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

    private List<Location> calculateMove(Game game, int steps) {
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
