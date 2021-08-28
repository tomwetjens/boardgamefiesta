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
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.json.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Not a @Value because each instance is unique
@Getter
@FieldDefaults(makeFinal = true)
@ToString
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class ObjectiveCard extends Card {

    // Some objective cards, like AUX_SF, can occur multiple times in the deck and in a player's hand,
    // therefore we cannot use the enum as-is in those sets
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    enum Type {
        START_34B(null, Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_4, ObjectiveCard.Task.BUILDING), 3, 0),
        START_SSG(null, Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.STATION, ObjectiveCard.Task.GREEN_TEEPEE), 3, 0),
        START_BBH(null, Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.HAZARD), 3, 0),
        START_BLHH(null, Arrays.asList(ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 3, 0),

        GAIN2_BBLBL(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.BLUE_TEEPEE), 3, 2),
        GAIN2_BGBL(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.GREEN_TEEPEE, ObjectiveCard.Task.BLUE_TEEPEE), 3, 2),
        GAIN2_4HH(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_4, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 3, 2),
        GAIN2_SSH(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.STATION, ObjectiveCard.Task.HAZARD), 3, 2),
        GAIN2_333B(PossibleAction.optional(Action.Gain2Dollars.class), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BUILDING), 4, 2),

        AUX_SF(PossibleAction.optional(Action.SingleOrDoubleAuxiliaryAction.class), Collections.singletonList(ObjectiveCard.Task.SAN_FRANCISCO), 5, 3),

        DRAW_BBH(PossibleAction.optional(PossibleAction.choice(Action.DrawCard.class, Action.Draw2Cards.class, Action.Draw3Cards.class)), Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.HAZARD), 3, 2),
        DRAW_SGBL(PossibleAction.optional(PossibleAction.choice(Action.DrawCard.class, Action.Draw2Cards.class, Action.Draw3Cards.class)), Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.GREEN_TEEPEE, ObjectiveCard.Task.BLUE_TEEPEE), 3, 2),
        DRAW_5H(PossibleAction.optional(PossibleAction.choice(Action.DrawCard.class, Action.Draw2Cards.class, Action.Draw3Cards.class)), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_5, ObjectiveCard.Task.HAZARD), 3, 2),
        DRAW_SGG(PossibleAction.optional(PossibleAction.choice(Action.DrawCard.class, Action.Draw2Cards.class, Action.Draw3Cards.class)), Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.GREEN_TEEPEE, ObjectiveCard.Task.GREEN_TEEPEE), 3, 2),
        DRAW_333S(PossibleAction.optional(PossibleAction.choice(Action.DrawCard.class, Action.Draw2Cards.class, Action.Draw3Cards.class)), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.STATION), 4, 2),

        ENGINE_44SG(PossibleAction.optional(Action.MoveEngineAtMost2Forward.class), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_4, ObjectiveCard.Task.BREEDING_VALUE_4, ObjectiveCard.Task.STATION, ObjectiveCard.Task.GREEN_TEEPEE), 5, 3),
        ENGINE_345(PossibleAction.optional(Action.MoveEngineAtMost2Forward.class), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_4, ObjectiveCard.Task.BREEDING_VALUE_5), 5, 3),
        ENGINE_BBGG(PossibleAction.optional(Action.MoveEngineAtMost2Forward.class), Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.GREEN_TEEPEE, ObjectiveCard.Task.GREEN_TEEPEE), 5, 3),
        // Note: these two cards have move engine 3 instead of 2!
        ENGINE_BBLHH(PossibleAction.optional(Action.MoveEngineAtMost3Forward.class), Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 5, 3),
        ENGINE_SSHH(PossibleAction.optional(Action.MoveEngineAtMost3Forward.class), Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.STATION, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 5, 3),

        MOVE_BBHH(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 5, 2),
        MOVE_SSBLBL(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.STATION, ObjectiveCard.Task.BLUE_TEEPEE, ObjectiveCard.Task.BLUE_TEEPEE), 5, 2),
        MOVE_345(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_4, ObjectiveCard.Task.BREEDING_VALUE_5), 5, 2),
        MOVE_34HH(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(ObjectiveCard.Task.BREEDING_VALUE_3, ObjectiveCard.Task.BREEDING_VALUE_4, ObjectiveCard.Task.HAZARD, ObjectiveCard.Task.HAZARD), 5, 2),
        MOVE_SSBB(PossibleAction.optional(Action.Move3ForwardWithoutFees.class), Arrays.asList(ObjectiveCard.Task.STATION, ObjectiveCard.Task.STATION, ObjectiveCard.Task.BUILDING, ObjectiveCard.Task.BUILDING), 5, 2);

        PossibleAction possibleAction;
        List<Task> tasks;
        int points;
        int penalty;
    }

    static final List<ObjectiveCard> STARTING_CARDS = Arrays.asList(
            new ObjectiveCard(Type.START_34B),
            new ObjectiveCard(Type.START_BBH),
            new ObjectiveCard(Type.START_BLHH),
            new ObjectiveCard(Type.START_SSG));

    Type type;

    Optional<PossibleAction> getPossibleAction() {
        return Optional.ofNullable(type.possibleAction);
    }

    @Override
    JsonValue serialize(JsonBuilderFactory factory) {
        return Json.createValue(type.name());
    }

    static ObjectiveCard deserialize(JsonValue jsonValue) {
        // For backwards compatibility
        // Deprecated
        if (jsonValue.getValueType() == JsonValue.ValueType.OBJECT) {
            var jsonObject = jsonValue.asJsonObject();

            var possibleActionValue = jsonObject.get("possibleAction");

            var possibleAction = possibleActionValue != null && possibleActionValue != JsonValue.NULL ? PossibleAction.deserialize(possibleActionValue.asJsonObject()) : null;

            var typePrefix = possibleAction == null ? "START_"
                    : possibleAction.canPerform(Action.Gain2Dollars.class) ? "GAIN2_"
                    : possibleAction.canPerform(Action.DrawCard.class) ? "DRAW_"
                    : possibleAction.canPerform(Action.Move3ForwardWithoutFees.class) ? "MOVE_"
                    : possibleAction.canPerform(Action.SingleOrDoubleAuxiliaryAction.class) ? "AUX_"
                    : "ENGINE_";

            var taskLetters = jsonObject.getJsonArray("tasks").getValuesAs(JsonString::getString).stream()
                    .map(Task::valueOf)
                    .map(task -> {
                        switch (task) {
                            case HAZARD:
                                return "H";
                            case BUILDING:
                                return "B";
                            case STATION:
                                return "S";
                            case BLUE_TEEPEE:
                                return "BL";
                            case GREEN_TEEPEE:
                                return "G";
                            case BREEDING_VALUE_3:
                                return "3";
                            case BREEDING_VALUE_4:
                                return "4";
                            case BREEDING_VALUE_5:
                                return "5";
                            case SAN_FRANCISCO:
                                return "SF";
                            default:
                                throw new IllegalArgumentException("unsupported task: " + task);
                        }
                    })
                    .collect(Collectors.joining());

            return new ObjectiveCard(Type.valueOf(typePrefix + taskLetters));
        }

        return new ObjectiveCard(Type.valueOf(((JsonString) jsonValue).getString()));
    }

    public Set<Class<? extends Action>> getPossibleActions() {
        return type.possibleAction != null ? type.possibleAction.getPossibleActions() : Collections.emptySet();
    }

    static Score score(Set<ObjectiveCard> committed, Set<ObjectiveCard> uncommitted, GWT game, Player player, boolean committedPairs3Points) {
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

    public int getPenalty() {
        return type.getPenalty();
    }

    public int getPoints() {
        return type.getPoints();
    }

    public List<Task> getTasks() {
        return type.getTasks();
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
            if (committed.contains(head)) {
                return Stream.concat(
                        // Normal case, committing to it
                        scoreCards(tail, committed, remaining, score.add(head, head.getPoints())),
                        // Or see what happens when taking the penalty
                        scoreCards(tail, committed, counts, score.add(head, -head.getPenalty())));
            } else {
                // If the player has the "3 points per pair of committed objective cards" station master tile,
                // it could make sense to commit to failed objective cards, if the penalty is less than the points for a pair
                return Stream.concat(
                        // Normal case, skipping it
                        scoreCards(tail, committed, counts, score),
                        // Or see what happens when committing to it
                        scoreCards(tail, committed, remaining, score.add(head, head.getPoints())));
            }
        }
    }

    private static Counts counts(GWT game, Player player) {
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
                playerState.numberOfCattleCards(EnumSet.of(CattleType.AYRSHIRE, CattleType.BROWN_SWISS, CattleType.HOLSTEIN)),
                playerState.numberOfCattleCards(EnumSet.of(CattleType.WEST_HIGHLAND)),
                playerState.numberOfCattleCards(EnumSet.of(CattleType.TEXAS_LONGHORN)),
                game.getRailroadTrack().numberOfDeliveries(player, City.SAN_FRANCISCO));
    }

    @AllArgsConstructor
    @ToString
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
