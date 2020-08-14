package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.repository.JsonSerializer;
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

    public Set<Class<? extends Action>> getPossibleActions() {
        return possibleAction != null ? possibleAction.getPossibleActions() : Collections.emptySet();
    }

    static Score score(Set<ObjectiveCard> committed, Set<ObjectiveCard> uncommitted, Game game, Player player, boolean committedPairs3Points) {
        Counts counts = counts(game, player);

        List<ObjectiveCard> objectiveCards = Stream.concat(committed.stream(), uncommitted.stream())
                .sorted(Comparator.comparingInt(ObjectiveCard::getPoints).reversed())
                .collect(Collectors.toList());

        return scoreCards(objectiveCards, committed, counts, Score.EMPTY)
                .max(committedPairs3Points
                        ? Comparator.comparingInt(score -> score.getTotal() + (score.getCommitted().size() / 2) * 3)
                        : Comparator.comparingInt(Score::getTotal))
                .orElse(Score.EMPTY);
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode
    @ToString
    public static class Score {

        static final Score EMPTY = new Score(0, Collections.emptyMap());

        @Getter
        int total;
        Map<ObjectiveCard, Integer> scores;

        private Score add(ObjectiveCard objectiveCard, int score) {
            var map = new HashMap<>(scores);
            map.put(objectiveCard, score);
            return new Score(total + score, map);
        }

        public int getScore(ObjectiveCard objectiveCard) {
            return scores.getOrDefault(objectiveCard, 0);
        }

        public Set<ObjectiveCard> getCommitted() {
            return scores.keySet();
        }
    }

    /**
     * @param objectiveCards sorted by points desc
     */
    private static Stream<Score> scoreCards(List<ObjectiveCard> objectiveCards, Set<ObjectiveCard> committed, Counts counts, Score score) {
        if (objectiveCards.isEmpty()) {
            return Stream.of(score);
        }

        var head = objectiveCards.get(0);
        var tail = objectiveCards.subList(1, objectiveCards.size());

        Counts remaining = counts.subtract(head.getTasks());

        if (remaining.hasNegative()) {
            if (committed.contains(head)) {
                return scoreCards(tail, committed, counts, score.add(head, -head.getPenalty()));
            } else {
                // If the player has the "3 points per pair of committed objective cards" station master tile,
                // it could make sense to commit to failed objective cards, if the penalty is less than the points for a pair
                return Stream.concat(
                        // Normal case, skipping it
                        scoreCards(tail, committed, counts, score),
                        // Or see what happens when committing to it
                        scoreCards(tail, committed, counts, score.add(head, -head.getPenalty())));
            }
        } else {
            return Stream.concat(
                    // Normal case, completed so use it
                    scoreCards(tail, committed, remaining, score.add(head, head.getPoints())),
                    // Or see what happens when using the resources for other cards
                    scoreCards(tail, committed, counts, score));
        }
    }

    private static Counts counts(Game game, Player player) {
        var playerState = game.playerState(player);

        var teepees = playerState.numberOfTeepees();
        var greenTeepees = playerState.numberOfGreenTeepees();
        var blueTeepees = teepees - greenTeepees;

        return new Counts(
                game.getTrail().numberOfBuildings(player),
                greenTeepees,
                blueTeepees,
                playerState.numberOfHazards(),
                game.getRailroadTrack().numberOfUpgradedStations(player),
                playerState.numberOfCattleCards(3),
                playerState.numberOfCattleCards(4),
                playerState.numberOfCattleCards(5),
                game.getRailroadTrack().numberOfDeliveries(player, City.SAN_FRANCISCO));
    }

    @AllArgsConstructor
    private static class Counts {
        int buildings;
        int greenTeepees;
        int blueTeepees;
        int hazards;
        int stations;
        int breedingValue3;
        int breedingValue4;
        int breedingValue5;
        int sanFrancisco;

        boolean hasNegative() {
            return buildings < 0 || greenTeepees < 0 || blueTeepees < 0 || hazards < 0 || stations < 0 || breedingValue3 < 0 || breedingValue4 < 0 || breedingValue5 < 0 || sanFrancisco < 0;
        }

        Counts subtract(List<Task> tasks) {
            Counts result = new Counts(buildings, greenTeepees, blueTeepees, hazards, stations, breedingValue3, breedingValue4, breedingValue5, sanFrancisco);

            for (Task task : tasks) {
                switch (task) {
                    case BUILDING:
                        result.buildings--;
                        break;
                    case GREEN_TEEPEE:
                        result.greenTeepees--;
                        break;
                    case BLUE_TEEPEE:
                        result.blueTeepees--;
                        break;
                    case HAZARD:
                        result.hazards--;
                        break;
                    case STATION:
                        result.stations--;
                        break;
                    case BREEDING_VALUE_3:
                        result.breedingValue3--;
                        break;
                    case BREEDING_VALUE_4:
                        result.breedingValue4--;
                        break;
                    case BREEDING_VALUE_5:
                        result.breedingValue5--;
                        break;
                    case SAN_FRANCISCO:
                        result.sanFrancisco--;
                        break;
                }
            }

            return result;
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
