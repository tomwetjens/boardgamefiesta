package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.repository.JsonSerializer;
import com.boardgamefiesta.api.domain.Player;
import lombok.*;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Not a @Value because each instance is unique
@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class ObjectiveCard extends Card {

    PossibleAction possibleAction;
    List<Task> tasks;
    int points;
    int penalty;

    Optional<PossibleAction> getPossibleAction() {
        return Optional.ofNullable(possibleAction);
    }

    @Override
    JsonObject serialize(JsonBuilderFactory factory) {
        return factory.createObjectBuilder()
                .add("possibleAction", possibleAction != null ? possibleAction.serialize(factory) : null)
                .add("tasks", JsonSerializer.forFactory(factory).fromStrings(tasks.stream().map(Task::name)))
                .add("points", points)
                .add("penalty", penalty)
                .build();
    }

    static ObjectiveCard deserialize(JsonObject jsonObject) {
        var possibleAction = jsonObject.getJsonObject("possibleAction");
        return new ObjectiveCard(
                possibleAction != null ? PossibleAction.deserialize(possibleAction) : null,
                jsonObject.getJsonArray("tasks").getValuesAs(JsonString::getString).stream().map(Task::valueOf).collect(Collectors.toList()),
                jsonObject.getInt("points"),
                jsonObject.getInt("penalty"));
    }

    public Optional<Class<? extends Action>> getAction() {
        return possibleAction != null ? possibleAction.getPossibleActions().stream().findFirst() : Optional.empty();
    }

    static int score(Set<ObjectiveCard> required, Set<ObjectiveCard> optional, Game game, Player player) {
        Counts counts = counts(game, player);

        Set<ObjectiveCard> objectiveCards = Stream.concat(required.stream(), optional.stream()).collect(Collectors.toSet());

        return scoreCards(objectiveCards, required, counts);
    }

    private static int scoreCards(Set<ObjectiveCard> objectiveCards, Set<ObjectiveCard> required, Counts counts) {
        return objectiveCards.stream()
                .mapToInt(objectiveCard -> {
                    Counts remaining = counts.subtract(objectiveCard.getTasks());

                    int score;
                    if (remaining.isNegative()) {
                        score = required.contains(objectiveCard) ? objectiveCard.getPenalty() : 0;
                    } else {
                        score = objectiveCard.getPoints();
                    }
                    return score + scoreCards(remove(objectiveCards, objectiveCard), required, remaining);
                })
                .max()
                .orElse(0);
    }

    private static Counts counts(Game game, Player player) {
        var playerState = game.playerState(player);

        var teepees = playerState.getTeepees();
        var greenTeepees = (int) teepees.stream().filter(teepee -> teepee == Teepee.GREEN).count();

        return Counts.builder()
                .count(Task.BUILDING, game.getTrail().numberOfBuildings(player))
                .count(Task.GREEN_TEEPEE, greenTeepees)
                .count(Task.BLUE_TEEPEE, teepees.size() - greenTeepees)
                .count(Task.HAZARD, playerState.getHazards().size())
                .count(Task.STATION, game.getRailroadTrack().numberOfUpgradedStations(player))
                .count(Task.BREEDING_VALUE_3, playerState.numberOfCattleCards(3))
                .count(Task.BREEDING_VALUE_4, playerState.numberOfCattleCards(4))
                .count(Task.BREEDING_VALUE_5, playerState.numberOfCattleCards(5))
                .count(Task.SAN_FRANCISCO, game.getRailroadTrack().numberOfDeliveries(player, City.SAN_FRANCISCO))
                .build();
    }

    private static <T> Set<T> remove(Set<T> set, T elem) {
        return set.stream().filter(b -> b != elem).collect(Collectors.toSet());
    }

    @Value
    @Builder
    private static class Counts {
        @Singular
        Map<Task, Integer> counts;

        Counts subtract(Task task) {
            Map<Task, Integer> result = new EnumMap<>(counts);
            result.compute(task, (k, v) -> (v != null ? v : 0) - 1);
            return new Counts(result);
        }

        boolean isNegative() {
            return counts.values().stream().anyMatch(v -> v < 0);
        }

        public Counts subtract(List<Task> tasks) {
            Counts results = this;
            for (Task task : tasks) {
                results = results.subtract(task);
            }
            return results;
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
        SAN_FRANCISCO
    }

}
