package com.boardgamefiesta.powergrid.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Auction {

    @Getter
    private PowerPlant powerPlant;
    private Integer bid;

    private List<Player> biddingPlayers;

    static Auction start(PowerPlant powerPlant, List<Player> biddingPlayers) {
        return new Auction(powerPlant, powerPlant.getCost(), new ArrayList<>(biddingPlayers));
    }

    static Auction deserialize(JsonObject jsonObject, Map<String, Player> playerMap) {
        return new Auction(
                PowerPlant.valueOf(jsonObject.getString("powerPlant")),
                jsonObject.getInt("bid"),
                jsonObject.getJsonArray("biddingPlayers").stream()
                        .map(JsonString.class::cast)
                        .map(JsonString::getString)
                        .map(playerMap::get)
                        .collect(Collectors.toList()));
    }

    JsonObjectBuilder serialize(JsonBuilderFactory jsonBuilderFactory) {
        var jsonSerializer = JsonSerializer.forFactory(jsonBuilderFactory);
        return jsonBuilderFactory.createObjectBuilder()
                .add("biddingPlayers", jsonSerializer.fromStrings(biddingPlayers, Player::getName))
                .add("powerPlant", powerPlant.name())
                .add("bid", bid);
    }

    Player getNextBiddingPlayer(Player currentPlayer) {
        return biddingPlayers.get((biddingPlayers.indexOf(currentPlayer) + 1) % biddingPlayers.size());
    }

    boolean isBiddingPlayer(Player player) {
        return biddingPlayers.contains(player);
    }

    void placeBid(int bid) {
        if (bid < getMinPlaceBid()) {
            throw new PowerGridException(PowerGridError.BID_TOO_LOW);
        }
        this.bid = bid;
    }

    void passBid(Player passingPlayer) {
        if (!biddingPlayers.remove(passingPlayer)) {
            throw new PowerGridException(PowerGridError.NOT_BIDDING_PLAYER);
        }
    }

    boolean isEnded() {
        return biddingPlayers.size() == 1;
    }

    public Optional<Integer> getBid() {
        return Optional.ofNullable(bid);
    }

    public int getMinPlaceBid() {
        return bid == null ? powerPlant.getCost() : bid + 1;
    }
}
