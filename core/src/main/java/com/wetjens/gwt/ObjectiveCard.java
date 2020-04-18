package com.wetjens.gwt;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import lombok.Value;

// Not a @Value because each instance is unique
@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class ObjectiveCard extends Card {

    private static final long serialVersionUID = 1L;

    PossibleAction possibleAction;
    List<Task> tasks;
    int points;
    int penalty;

    Optional<PossibleAction> getPossibleAction() {
        return Optional.ofNullable(possibleAction);
    }

    public Optional<Class<? extends Action>> getAction() {
        return possibleAction != null ? Optional.of(possibleAction.getPossibleActions().iterator().next()) : Optional.empty();
    }

    static int score(Set<ObjectiveCard> required, Set<ObjectiveCard> optional, Game game, Player player) {
        Counts counts = counts(game, player);

        Set<ObjectiveCard> objectiveCards = Stream.concat(required.stream(), optional.stream()).collect(Collectors.toSet());

        return scoreCards(objectiveCards, required, counts);
    }

    private static int scoreCards(Set<ObjectiveCard> objectiveCards, Set<ObjectiveCard> required, Counts counts) {
        return objectiveCards.stream()
                .mapToInt(objectiveCard -> scoreCard(objectiveCard, required.contains(objectiveCard), counts)
                        + scoreCards(remove(objectiveCards, objectiveCard), required, counts))
                .max()
                .orElse(0);
    }

    private static int scoreCard(ObjectiveCard objectiveCard, boolean required, Counts counts) {
        Counts c = counts;

        for (Task task : objectiveCard.getTasks()) {
            c = c.subtract(task);
            if (c.isNegative()) {
                break;
            }
        }

        if (c.isNegative()) {
            return required ? objectiveCard.getPenalty() : 0;
        }
        return objectiveCard.getPoints();
    }

    private static Counts counts(Game game, Player player) {
        List<Teepee> teepees = game.playerState(player).getTeepees();
        int greenTeepees = (int) teepees.stream().filter(teepee -> teepee == Teepee.GREEN).count();

        return Counts.builder()
                .count(Task.GREEN_TEEPEE, greenTeepees)
                .count(Task.BLUE_TEEPEE, teepees.size() - greenTeepees)
                .count(Task.SAN_FRANCISCO, game.getRailroadTrack().numberOfDeliveries(player, City.SAN_FRANCISCO))
                .count(Task.STATION, game.getRailroadTrack().numberOfUpgradedStations(player))
                .count(Task.BUILDING, game.getTrail().numberOfBuildings(player))
                .count(Task.BREEDING_VALUE_3, game.playerState(player).numberOfCattleCards(3))
                .count(Task.BREEDING_VALUE_4, game.playerState(player).numberOfCattleCards(4))
                .count(Task.BREEDING_VALUE_5, game.playerState(player).numberOfCattleCards(5))
                .build();
    }

    private static <T> Set<T> remove(Set<T> set, T elem) {
        return set.stream().filter(b -> b != elem).collect(Collectors.toSet());
    }

    @Value
    @Builder
    private static final class Counts {
        @Singular
        Map<Task, Integer> counts;

        Counts subtract(Task task) {
            Map<Task, Integer> result = new EnumMap<>(counts);
            result.compute(task, (k, v) -> v - 1);
            return new Counts(result);
        }

        boolean isNegative() {
            return counts.values().stream().anyMatch(v -> v < 0);
        }
    }

    public enum Task {
        BUILDING,
        GREEN_TEEPEE,
        BLUE_TEEPEE,
        HAZARD,
        STATION,
        BREEDING_VALUE_3,
        BREEDING_VALUE_4,
        BREEDING_VALUE_5,
        SAN_FRANCISCO;
    }

}
