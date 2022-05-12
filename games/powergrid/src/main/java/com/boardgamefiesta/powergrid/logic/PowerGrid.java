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

package com.boardgamefiesta.powergrid.logic;

import com.boardgamefiesta.api.domain.EventListener;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.State;
import com.boardgamefiesta.api.domain.Stats;
import com.boardgamefiesta.api.repository.JsonDeserializer;
import com.boardgamefiesta.api.repository.JsonSerializer;
import com.boardgamefiesta.powergrid.logic.map.Area;
import com.boardgamefiesta.powergrid.logic.map.City;
import com.boardgamefiesta.powergrid.logic.map.NetworkMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PowerGrid implements State {

    private static final List<Integer> PAYOUTS = List.of(10, 22, 33, 44, 54, 64, 73, 82, 90, 98, 105, 112, 118, 124, 129, 134, 138, 142, 145, 148, 150);

    @Getter
    private final NetworkMap map;
    @Getter
    private final Set<Area> areas;
    @Getter
    private final List<Player> players;
    @Getter
    private final List<Player> playerOrder;
    @Getter
    private final ResourceMarket resourceMarket;
    @Getter
    private final PowerPlantMarket powerPlantMarket;
    @Getter
    private final Map<City, List<Player>> cities;
    @Getter
    private final Map<Player, PlayerState> playerStates;

    @Getter
    private int step;
    @Getter
    private int round;
    @Getter
    private Phase phase;
    @Getter
    private Player currentPlayer;

    private List<Player> auctioningPlayers;
    private Set<Player> producingPlayers;

    private Auction auction;

    public static PowerGrid start(@NonNull Set<Player> players, @NonNull NetworkMap map, @NonNull Set<Area> areas, EventListener eventListener, @NonNull Random random) {
        if (players.size() < 2) {
            throw new PowerGridException(PowerGridError.NOT_ENOUGH_PLAYERS);
        }

        if (players.size() > 5) {
            throw new PowerGridException(PowerGridError.TOO_MANY_PLAYERS);
        }

        if (areas.size() != players.size()) {
            throw new PowerGridException(PowerGridError.INVALID_NUMBER_OF_AREAS);
        }

        map.validateAreas(areas);

        var playerOrder = new ArrayList<>(players);
        Collections.shuffle(playerOrder, random);

        var auctioningPlayers = new ArrayList<>(playerOrder);

        var powerGrid = new PowerGrid(map, areas,
                new ArrayList<>(playerOrder),
                playerOrder,
                ResourceMarket.create(),
                PowerPlantMarket.create(players.size(), random),
                new HashMap<>(),
                players.stream().collect(Collectors.toMap(Function.identity(), PlayerState::create)),
                1,
                1,
                Phase.AUCTION,
                auctioningPlayers.get(0),
                auctioningPlayers,
                null,
                null);

        if (eventListener != null) {
            powerGrid.addEventListener(eventListener);
        }

        return powerGrid;
    }

    public static PowerGrid deserialize(JsonValue jsonValue) {
        var jsonObject = jsonValue.asJsonObject();

        var map = NetworkMap.GERMANY;

        var areas = jsonObject.getJsonArray("areas").stream()
                .map(JsonString.class::cast)
                .map(JsonString::getString)
                .map(map::getArea)
                .collect(Collectors.toSet());

        var players = jsonObject.getJsonArray("players").stream()
                .map(JsonValue::asJsonObject)
                .map(Player::deserialize)
                .collect(Collectors.toList());

        var playerMap = players.stream().collect(Collectors.toMap(Player::getName, Function.identity()));

        var playerOrder = jsonObject.getJsonArray("playerOrder").stream()
                .map(JsonString.class::cast)
                .map(JsonString::getString)
                .map(playerMap::get)
                .collect(Collectors.toList());

        var resourceMarket = ResourceMarket.deserialize(jsonObject.getJsonObject("resourceMarket"));
        var powerPlantMarket = PowerPlantMarket.deserialize(jsonObject.getJsonObject("powerPlantMarket"));

        var cities = JsonDeserializer.forObject(jsonObject.getJsonObject("cities"))
                .asMap(map::getCity, jv -> jv.asJsonArray().stream()
                        .map(JsonString.class::cast)
                        .map(JsonString::getString)
                        .map(playerMap::get)
                        .collect(Collectors.toList()));

        var playerStates = JsonDeserializer.forObject(jsonObject.getJsonObject("playerStates"))
                .asObjectMap(playerMap::get, PlayerState::deserialize);

        var step = jsonObject.getInt("step");
        var round = jsonObject.getInt("round");
        var phase = Phase.valueOf(jsonObject.getString("phase"));

        var currentPlayer = playerMap.get(jsonObject.getString("currentPlayer"));

        var auctioningPlayers = phase == Phase.AUCTION
                ? jsonObject.getJsonArray("auctioningPlayers").stream()
                .map(JsonString.class::cast)
                .map(JsonString::getString)
                .map(playerMap::get)
                .collect(Collectors.toList()) : null;

        var auction = phase == Phase.AUCTION && jsonObject.containsKey("auction")
                ? Auction.deserialize(jsonObject.getJsonObject("auction"), playerMap)
                : null;

        var producingPlayers = phase == Phase.BUREAUCRACY
                ? jsonObject.getJsonArray("producingPlayers").stream()
                .map(JsonString.class::cast)
                .map(JsonString::getString)
                .map(playerMap::get)
                .collect(Collectors.toSet()) : null;

        return new PowerGrid(
                map,
                areas,
                players,
                playerOrder,
                resourceMarket,
                powerPlantMarket,
                cities,
                playerStates,
                step,
                round,
                phase,
                currentPlayer,
                auctioningPlayers,
                producingPlayers,
                auction
        );
    }

    public JsonObject serialize(JsonBuilderFactory jsonBuilderFactory) {
        var jsonSerializer = JsonSerializer.forFactory(jsonBuilderFactory);

        var jsonObjectBuilder = jsonBuilderFactory.createObjectBuilder()
                .add("map", map.getName())
                .add("areas", jsonSerializer.fromStrings(areas, Area::getName))
                .add("players", jsonSerializer.fromCollection(players, Player::serialize))
                .add("playerOrder", jsonSerializer.fromStrings(playerOrder, Player::getName))
                .add("resourceMarket", resourceMarket.serialize(jsonBuilderFactory))
                .add("powerPlantMarket", powerPlantMarket.serialize(jsonBuilderFactory))
                .add("cities", jsonSerializer.fromMap(cities, City::getName, players -> jsonSerializer.fromStrings(players, Player::getName)))
                .add("playerStates", jsonSerializer.fromMap(playerStates, Player::getName, PlayerState::serialize))
                .add("step", step)
                .add("round", round)
                .add("phase", phase.name())
                .add("currentPlayer", currentPlayer != null ? currentPlayer.getName() : null);

        switch (phase) {
            case AUCTION:
                jsonObjectBuilder.add("auctioningPlayers", jsonSerializer.fromStrings(auctioningPlayers, Player::getName));

                if (isAuctionInProgress()) {
                    jsonObjectBuilder.add("auction", auction.serialize(jsonBuilderFactory));
                }
                break;

            case BUREAUCRACY:
                jsonObjectBuilder.add("producingPlayers", jsonSerializer.fromStrings(producingPlayers, Player::getName));
                break;
        }

        return jsonObjectBuilder.build();
    }

    public void buyResource(@NonNull ResourceType resourceType, int amount) {

    }

    public void startAuction(@NonNull PowerPlant powerPlant) {
        if (phase != Phase.AUCTION) {
            throw new PowerGridException(PowerGridError.NOT_AUCTION_PHASE);
        }

        if (isAuctionInProgress()) {
            throw new PowerGridException(PowerGridError.AUCTION_IN_PROGRESS);
        }

        if (!powerPlantMarket.getActual().contains(powerPlant)) {
            throw new PowerGridException(PowerGridError.POWER_PLANT_NOT_AVAILABLE);
        }

        auction = Auction.start(powerPlant, auctioningPlayers);
        if (auctioningPlayers.size() > 1) {
            nextBiddingPlayer();
        } else {
            passPlaceBid();
        }
    }

    private boolean isAuctionInProgress() {
        return auction != null;
    }

    private void nextBiddingPlayer() {
        do {
            currentPlayer = auction.getNextBiddingPlayer(currentPlayer);

            if (playerStates.get(currentPlayer).getBalance() < auction.getMinPlaceBid()) {
                // Auto pass
                passPlaceBid();
            }
        } while (isAuctionInProgress() && !auction.isBiddingPlayer(currentPlayer));
    }

    private void nextAuctioningPlayer() {
        do {
            currentPlayer = auctioningPlayers.get((auctioningPlayers.indexOf(currentPlayer) + 1) % auctioningPlayers.size());

            if (!canStartAuction(currentPlayer)) {
                auctioningPlayers.remove(currentPlayer);
            }
        } while (!canStartAuction(currentPlayer) && !auctioningPlayers.isEmpty());

        if (auctioningPlayers.isEmpty()) {
            phase = Phase.RESOURCES;
            currentPlayer = playerOrder.get(playerOrder.size() - 1);
        }
    }

    private boolean canStartAuction(Player player) {
        return powerPlantMarket.getActual().stream().anyMatch(powerPlant ->
                powerPlant.getCost() <= playerStates.get(player).getBalance());
    }

    public void placeBid(int bid) {
        if (phase != Phase.AUCTION) {
            throw new PowerGridException(PowerGridError.NOT_AUCTION_PHASE);
        }

        if (!isAuctionInProgress()) {
            throw new PowerGridException(PowerGridError.NO_AUCTION_IN_PROGRESS);
        }

        if (bid > playerStates.get(currentPlayer).getBalance()) {
            throw new PowerGridException(PowerGridError.BALANCE_TOO_LOW);
        }

        auction.placeBid(bid);
        nextBiddingPlayer();
    }

    @Override
    public void perform(@NonNull Player player, @NonNull com.boardgamefiesta.api.domain.Action action, @NonNull Random random) {
        perform(player, (Action) action, random);
    }

    public void perform(@NonNull Player player, @NonNull Action action, @NonNull Random random) {
        if (!currentPlayer.equals(player) && !producingPlayers.contains(player)) {
            throw new PowerGridException(PowerGridError.NOT_PLAYERS_TURN);
        }

        action.perform(this, player, random);
    }

    @Override
    public void addEventListener(EventListener eventListener) {

    }

    @Override
    public void removeEventListener(EventListener eventListener) {

    }

    @Override
    public void endTurn(Player player, @NonNull Random random) {

    }

    @Override
    public void forceEndTurn(Player player, @NonNull Random random) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getScore(Player player) {
        // TODO
        return 0;
    }

    @Override
    public List<Player> getRanking() {
        return null;
    }

    @Override
    public boolean isEnded() {
        return false;
    }

    @Override
    public boolean canUndo() {
        return false;
    }

    @Override
    public Set<Player> getCurrentPlayers() {
        return phase != Phase.BUREAUCRACY ? Collections.singleton(currentPlayer) : Collections.unmodifiableSet(producingPlayers);
    }

    @Override
    public Optional<Integer> getTurn(Player player) {
        // TODO
        return Optional.empty();
    }

    @Override
    public int getProgress() {
        // TODO
        return 0;
    }

    @Override
    public void leave(Player player, Random random) {

    }

    @Override
    public Stats stats(Player player) {
        return null;
    }

    @Override
    public void skip(@NonNull Player player, @NonNull Random random) {
        switch (phase) {
            case AUCTION:
                if (!isAuctionInProgress()) {
                    passStartAuction();
                } else {
                    passPlaceBid();
                }
                break;
            case RESOURCES:
                passBuyResources();
                break;
            case BUILD:
                passConnectCity();
                break;
            case BUREAUCRACY:
                passProducePower();
                break;
        }
    }

    public void passPlaceBid() {
        if (phase != Phase.AUCTION) {
            throw new PowerGridException(PowerGridError.NOT_AUCTION_PHASE);
        }

        if (!isAuctionInProgress()) {
            throw new PowerGridException(PowerGridError.NO_AUCTION_IN_PROGRESS);
        }

        var passingPlayer = currentPlayer;
        nextBiddingPlayer();
        auction.passBid(passingPlayer);

        if (auction.isEnded()) {
            var playerState = playerStates.get(currentPlayer);
            playerState.pay(auction.getBid().orElseThrow());
            playerState.addPowerPlant(auction.getPowerPlant());

            auctioningPlayers.remove(currentPlayer);

            auction = null;

            nextAuctioningPlayer();
        }
    }

    public Set<Class<? extends Action>> getActions(Player player) {
        switch (phase) {
            case AUCTION:
                if (currentPlayer.equals(player)) {
                    if (!isAuctionInProgress()) {
                        if (auctioningPlayers.contains(currentPlayer)) {
                            return Collections.singleton(com.boardgamefiesta.powergrid.logic.Action.StartAuction.class);
                        } else {
                            return Collections.singleton(com.boardgamefiesta.powergrid.logic.Action.RemovePowerPlant.class);
                        }
                    } else {
                        return Collections.singleton(com.boardgamefiesta.powergrid.logic.Action.PlaceBid.class);
                    }
                }
            case RESOURCES:
                if (currentPlayer.equals(player)) {
                    return Collections.singleton(com.boardgamefiesta.powergrid.logic.Action.BuyResource.class);
                }
            case BUILD:
                if (currentPlayer.equals(player)) {
                    return Collections.singleton(com.boardgamefiesta.powergrid.logic.Action.ConnectCity.class);
                }
            case BUREAUCRACY:
                if (producingPlayers.contains(player)) {
                    return Collections.singleton(com.boardgamefiesta.powergrid.logic.Action.ProducePower.class);
                }
        }

        return Collections.emptySet();
    }

    public void passStartAuction() {
        if (phase != Phase.AUCTION) {
            throw new PowerGridException(PowerGridError.NOT_AUCTION_PHASE);
        }

        if (isAuctionInProgress()) {
            throw new PowerGridException(PowerGridError.AUCTION_IN_PROGRESS);
        }

        if (playerStates.get(currentPlayer).getPowerPlants().isEmpty()) {
            throw new PowerGridException(PowerGridError.MUST_START_AUCTION);
        }

        nextAuctioningPlayer();
        auctioningPlayers.remove(currentPlayer);

        if (auctioningPlayers.isEmpty()) {
            phase = Phase.RESOURCES;
            startPlayerReverseOrder();
        }
    }

    private void startPlayerReverseOrder() {
        currentPlayer = playerOrder.get(playerOrder.size() - 1);
    }

    private void passBuyResources() {
    }

    private void passProducePower() {

    }

    private void passConnectCity() {

    }

    public void producePower(Player player, Map<ResourceType, Integer> resources) {
        if (phase != Phase.BUREAUCRACY) {
            throw new PowerGridException(PowerGridError.NOT_BUREAUCRACY_PHASE);
        }

        if (!producingPlayers.contains(player)) {
            throw new PowerGridException(PowerGridError.ALREADY_PRODUCED_THIS_ROUND);
        }

        var playerState = playerStates.get(player);

        var produced = playerState.producePower(resources);
        var connected = numberOfCities(player);
        var powered = Math.min(produced, connected);

        resources.forEach(playerState::removeResource);
        playerState.earn(PAYOUTS.get(powered));

        producingPlayers.remove(player);

        if (producingPlayers.isEmpty()) {
            nextRound();
        }
    }

    private void nextRound() {
        round++;
        resourceMarket.fillUp(playerOrder.size(), step);
        if (step == 3) {
            powerPlantMarket.removeLowestWithoutReplacement();
        } else {
            // TODO
            //powerPlantMarket.removeHighestFuture(random);
        }
    }

    private int numberOfCities(Player player) {
        return (int) cities.values().stream().filter(players -> players.contains(player)).count();
    }

    public Optional<Auction> getAuction() {
        return Optional.ofNullable(auction);
    }
}