package com.wetjens.gwt;

import com.wetjens.gwt.api.Player;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class PossibleMove {

    Location from;
    List<Location> steps;
    int cost;
    Map<Player, Integer> playerFees;

    static PossibleMove fromTo(@NonNull Location from, @NonNull List<Location> steps, @NonNull Player player, int balance, int playerCount) {
        return new PossibleMove(from, steps,
                calculateCost(steps, player, balance, playerCount),
                calculatePlayerFees(steps, player, balance, playerCount));
    }

    static PossibleMove firstMove(@NonNull Location to) {
        return new PossibleMove(null, Collections.singletonList(to), 0, Collections.emptyMap());
    }

    private static int calculateCost(@NonNull List<Location> steps, @NonNull Player player, int balance, int playerCount) {
        return Math.min(balance, steps.stream()
                .filter(location -> !(location instanceof Location.BuildingLocation) || ((Location.BuildingLocation) location).getBuilding()
                        .filter(building -> building instanceof PlayerBuilding)
                        .map(building -> (PlayerBuilding) building)
                        .map(playerBuilding -> playerBuilding.getPlayer() != player)
                        .orElse(false))
                .mapToInt(location -> location.getHand().getFee(playerCount))
                .sum());
    }

    private static Map<Player, Integer> calculatePlayerFees(List<Location> steps, Player player, int balance, int playerCount) {
        var remainingBalance = new AtomicInteger(balance);
        return steps.stream()
                .filter(location -> location instanceof Location.BuildingLocation)
                .map(location -> (Location.BuildingLocation) location)
                .flatMap(buildingLocation -> buildingLocation.getBuilding().stream())
                .filter(building -> building instanceof PlayerBuilding)
                .map(building -> (PlayerBuilding) building)
                .filter(playerBuilding -> playerBuilding.getPlayer() != player)
                .filter(playerBuilding -> playerBuilding.getHand() != Hand.NONE)
                .map(playerBuilding -> {
                    var fee = Math.min(remainingBalance.get(), playerBuilding.getHand().getFee(playerCount));

                    remainingBalance.getAndAdd(-fee);

                    return new PlayerFee(playerBuilding.getPlayer(), fee);
                })
                .filter(playerFee -> playerFee.getFee() > 0)
                .collect(Collectors.groupingBy(PlayerFee::getPlayer)).entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().mapToInt(PlayerFee::getFee).sum()));
    }

    public Optional<Location> getFrom() {
        return Optional.ofNullable(from);
    }

    @Value
    private static class PlayerFee {
        Player player;
        int fee;
    }
}
