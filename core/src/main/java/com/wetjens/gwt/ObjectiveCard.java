package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;

@Getter
public class ObjectiveCard extends Card {

    PossibleAction possibleAction;
    List<Task> tasks;
    int points;
    int penalty;

    ObjectiveCard(PossibleAction possibleAction, List<Task> tasks, int points, int penalty) {
        this.possibleAction = possibleAction;
        this.tasks = tasks;
        this.points = points;
        this.penalty = penalty;
    }

    static Queue<ObjectiveCard> randomDeck(Random random) {
        List<ObjectiveCard> deck = new ArrayList<>(createSet());
        Collections.shuffle(deck, random);
        return new LinkedList<>(deck);
    }

    static int score(Set<ObjectiveCard> required, Set<ObjectiveCard> optional, Game game, Player player) {
        Counts counts = counts(game, player);

        Set<ObjectiveCard> objectiveCards = Stream.concat(required.stream(), optional.stream()).collect(Collectors.toSet());

        return score(objectiveCards, required, counts);
    }

    private static int score(Set<ObjectiveCard> objectiveCards, Set<ObjectiveCard> required, Counts counts) {
        return objectiveCards.stream()
                .mapToInt(objectiveCard -> score(objectiveCard, required.contains(objectiveCard), counts) + score(remove(objectiveCards, objectiveCard), required, counts))
                .max()
                .orElse(0);
    }

    private static int score(ObjectiveCard objectiveCard, boolean required, Counts counts) {
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
            Map<Task, Integer> result = new EnumMap<Task, Integer>(counts);
            result.compute(task, (k, v) -> v - 1);
            return new Counts(result);
        }

        boolean isNegative() {
            return counts.values().stream().anyMatch(v -> v < 0);
        }
    }

    private static Collection<ObjectiveCard> createSet() {
        return Arrays.asList(
                new ObjectiveCard(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(Task.BUILDING, Task.BLUE_TEEPEE, Task.BLUE_TEEPEE), 3, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(Task.BUILDING, Task.GREEN_TEEPEE, Task.BLUE_TEEPEE), 3, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(Task.BREEDING_VALUE_4, Task.HAZARD, Task.HAZARD), 3, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(Task.STATION, Task.STATION, Task.HAZARD), 3, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(Task.BREEDING_VALUE_3, Task.BREEDING_VALUE_3, Task.BREEDING_VALUE_3, Task.BUILDING), 4, 2),

                new ObjectiveCard(PossibleAction.optional(Action.SingleOrDoubleAuxiliaryAction.class), Collections.singletonList(Task.SAN_FRANCISCO), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.SingleOrDoubleAuxiliaryAction.class), Collections.singletonList(Task.SAN_FRANCISCO), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.SingleOrDoubleAuxiliaryAction.class), Collections.singletonList(Task.SAN_FRANCISCO), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.SingleOrDoubleAuxiliaryAction.class), Collections.singletonList(Task.SAN_FRANCISCO), 5, 3),

                new ObjectiveCard(PossibleAction.optional(Action.DrawCardsThenDiscardCards.upTo(3)), Arrays.asList(Task.BUILDING, Task.BUILDING, Task.HAZARD), 3, 2),
                new ObjectiveCard(PossibleAction.optional(Action.DrawCardsThenDiscardCards.upTo(3)), Arrays.asList(Task.STATION, Task.GREEN_TEEPEE, Task.BLUE_TEEPEE), 3, 2),
                new ObjectiveCard(PossibleAction.optional(Action.DrawCardsThenDiscardCards.upTo(3)), Arrays.asList(Task.BREEDING_VALUE_5, Task.HAZARD), 3, 2),
                new ObjectiveCard(PossibleAction.optional(Action.DrawCardsThenDiscardCards.upTo(3)), Arrays.asList(Task.STATION, Task.GREEN_TEEPEE, Task.GREEN_TEEPEE), 3, 2),
                new ObjectiveCard(PossibleAction.optional(Action.DrawCardsThenDiscardCards.upTo(3)), Arrays.asList(Task.BREEDING_VALUE_3, Task.BREEDING_VALUE_3, Task.BREEDING_VALUE_3, Task.STATION), 4, 2),

                new ObjectiveCard(PossibleAction.optional(Action.MoveEngineAtMost2Forward.class), Arrays.asList(Task.BREEDING_VALUE_4, Task.BREEDING_VALUE_4, Task.STATION, Task.GREEN_TEEPEE), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.MoveEngineAtMost2Forward.class), Arrays.asList(Task.BREEDING_VALUE_3, Task.BREEDING_VALUE_4, Task.BREEDING_VALUE_5), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.MoveEngineAtMost2Forward.class), Arrays.asList(Task.BUILDING, Task.BUILDING, Task.GREEN_TEEPEE, Task.GREEN_TEEPEE), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.MoveEngineAtMost3Forward.class), Arrays.asList(Task.BUILDING, Task.BLUE_TEEPEE, Task.HAZARD, Task.HAZARD), 5, 3),
                new ObjectiveCard(PossibleAction.optional(Action.MoveEngineAtMost3Forward.class), Arrays.asList(Task.STATION, Task.STATION, Task.HAZARD, Task.HAZARD), 5, 3),

                new ObjectiveCard(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(Task.BUILDING, Task.BUILDING, Task.HAZARD, Task.HAZARD), 5, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(Task.STATION, Task.STATION, Task.BLUE_TEEPEE, Task.BLUE_TEEPEE), 5, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(Task.BREEDING_VALUE_3, Task.BREEDING_VALUE_4, Task.BREEDING_VALUE_5), 5, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(Task.BREEDING_VALUE_3, Task.BREEDING_VALUE_4, Task.HAZARD, Task.HAZARD), 5, 2),
                new ObjectiveCard(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(Task.STATION, Task.STATION, Task.BUILDING, Task.BUILDING), 5, 2)
        );
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
