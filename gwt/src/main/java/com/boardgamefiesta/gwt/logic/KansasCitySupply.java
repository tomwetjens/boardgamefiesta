package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.*;

import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Original:
 * <p>
 * '2-tiles':
 * 33 workers
 * <p>
 * '3-tiles':
 * 21 workers
 */
@ToString
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KansasCitySupply {

    private final List<DrawPile> drawPiles;

    static KansasCitySupply original(Random random) {
        var drawPile1 = DrawPile.fromSet(createSet1(), random);
        var drawPile2 = DrawPile.fromSet(createSet2(), random);
        var drawPile3 = DrawPile.fromSet(createSet3(), random);

        return new KansasCitySupply(List.of(
                drawPile1,
                drawPile2,
                drawPile3));
    }

    /**
     * Variant to approximate 4P ratios, courtesy of Fernando Moritz.
     * Alternative:           https://boardgamegeek.com/thread/1665842/less-tiles-when-playing-2-or-3-players
     * <p>
     * With 4 players, just use the normal rules.
     * - set 1: 9 green teepees, 8 blue teepees, 18 hazards
     * - set 2: 33 workers
     * - set 3: 21 workers, 2 green teepees, 3 blue teepees
     * <p>
     * With 3 players, remove X tiles from the game (before the setup):
     * - set 1: -2 green teepees, -1 blue teepee, -1 of each hazard tile (3g)
     * - set 2: -3 of each worker (-9)
     * - set 3: -1 blue teepee, -1 of each worker (-3)
     * <p>
     * With 2 players, remove X tiles from the game (before the setup):
     * - set 1: -3 green teepees, -3 blue teepees, -2 of each hazard tile (2,4VP)
     * - set 2: -5 of each worker (-15)
     * - set 3: -1 blue teepee, -1 green teepee, -3 of each worker (-9)
     * <p>
     * This variant should roughly preserve the ratios between the different tile types and the ratio between
     * the tiles that will be used during the game and the ones remaining at the end.
     */
    static KansasCitySupply balanced(int playerCount, Random random) {
        var kansasCitySupply = original(random);

        var drawPile1 = kansasCitySupply.drawPiles.get(0);
        var drawPile2 = kansasCitySupply.drawPiles.get(1);
        var drawPile3 = kansasCitySupply.drawPiles.get(2);

        if (playerCount == 3) {
            drawPile1.removeTeepees(Teepee.BLUE, 1);
            drawPile1.removeTeepees(Teepee.GREEN, 2);
            drawPile1.removeHazardsOfEachTypeWithPointsAndHand(1, 3, Hand.GREEN);

            drawPile2.removeWorkersOfEachType(3);

            drawPile3.removeTeepees(Teepee.BLUE, 1);
            drawPile3.removeWorkersOfEachType(1);
        } else if (playerCount == 2) {
            drawPile1.removeTeepees(Teepee.GREEN, 3);
            drawPile1.removeTeepees(Teepee.BLUE, 3);
            drawPile1.removeHazardOfEachTypeWithPoints(1, 2);
            drawPile1.removeHazardOfEachTypeWithPoints(1, 4);

            drawPile2.removeWorkersOfEachType(5);

            drawPile3.removeTeepees(Teepee.BLUE, 1);
            drawPile3.removeTeepees(Teepee.GREEN, 1);
            drawPile3.removeWorkersOfEachType(3);
        }

        return kansasCitySupply;
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("drawPiles", serializer.fromCollection(drawPiles, DrawPile::serialize))
                .build();
    }

    static KansasCitySupply deserialize(JsonObject jsonObject) {
        return builder()
                .drawPiles(jsonObject.getJsonArray("drawPiles").stream()
                        .map(JsonValue::asJsonArray)
                        .map(DrawPile::deserialize)
                        .collect(Collectors.toList()))
                .build();
    }

    Optional<Tile> draw(int drawPileIndex) {
        return drawPiles.get(drawPileIndex).draw();
    }

    private static ArrayList<Tile> createSet1() {
        return Stream.concat(
                Stream.concat(
                        IntStream.range(0, 9).mapToObj(i -> new Tile(Teepee.GREEN)),
                        IntStream.range(0, 8).mapToObj(i -> new Tile(Teepee.BLUE))),
                Arrays.stream(HazardType.values())
                        .flatMap(type -> Stream.of(
                                new Hazard(type, Hand.BLACK, 3),
                                new Hazard(type, Hand.BLACK, 2),
                                new Hazard(type, Hand.GREEN, 4),
                                new Hazard(type, Hand.GREEN, 4),
                                new Hazard(type, Hand.GREEN, 3),
                                new Hazard(type, Hand.GREEN, 2)))
                        .map(Tile::new))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static ArrayList<Tile> createSet2() {
        return Arrays.stream(Worker.values())
                .flatMap(worker -> IntStream.range(0, 11).mapToObj(i -> new Tile(worker)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static ArrayList<Tile> createSet3() {
        return Stream.concat(
                Stream.concat(
                        Arrays.stream(Worker.values())
                                .flatMap(worker -> IntStream.range(0, 7).mapToObj(i -> new Tile(worker))),
                        IntStream.range(0, 2).mapToObj(i -> new Tile(Teepee.GREEN))),
                IntStream.range(0, 3).mapToObj(i -> new Tile(Teepee.BLUE)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Getter
    @ToString
    public static final class Tile {

        private final Worker worker;
        private final Hazard hazard;
        private final Teepee teepee;

        public Tile(Worker worker) {
            this.worker = worker;
            this.hazard = null;
            this.teepee = null;
        }

        Tile(Hazard hazard) {
            this.hazard = hazard;
            this.worker = null;
            this.teepee = null;
        }

        Tile(Teepee teepee) {
            this.teepee = teepee;
            this.hazard = null;
            this.worker = null;
        }

        static Tile deserialize(JsonObject jsonObject) {
            if (jsonObject.containsKey("worker")) {
                return new Tile(Worker.valueOf(jsonObject.getString("worker")));
            }

            if (jsonObject.containsKey("teepee")) {
                return new Tile(Teepee.valueOf(jsonObject.getString("teepee")));
            }

            return new Tile(Hazard.deserialize(jsonObject.getJsonObject("hazard")));
        }

        JsonObject serialize(JsonBuilderFactory factory) {
            if (worker != null) {
                return factory.createObjectBuilder().add("worker", worker.name()).build();
            } else if (teepee != null) {
                return factory.createObjectBuilder().add("teepee", teepee.name()).build();
            } else if (hazard != null) {
                return factory.createObjectBuilder().add("hazard", hazard.serialize(factory)).build();
            }
            throw new IllegalStateException("nothing to serialize");
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class DrawPile {

        private final Queue<Tile> tiles;

        private static DrawPile deserialize(JsonArray jsonArray) {
            return new DrawPile(jsonArray.stream().map(JsonValue::asJsonObject)
                    .map(Tile::deserialize)
                    .collect(Collectors.toCollection(LinkedList::new)));
        }

        private static DrawPile fromSet(List<Tile> list, Random random) {
            Collections.shuffle(list, random);
            return new DrawPile(new LinkedList<>(list));
        }

        private JsonValue serialize(JsonBuilderFactory jsonBuilderFactory) {
            return JsonSerializer.forFactory(jsonBuilderFactory).fromCollection(tiles, Tile::serialize);
        }

        private Optional<Tile> draw() {
            return Optional.ofNullable(tiles.poll());
        }

        private void removeTeepees(Teepee teepee, int amount) {
            tiles.removeAll(tiles.stream()
                    .filter(tile -> tile.getTeepee() == teepee)
                    .limit(amount)
                    .collect(Collectors.toList()));
        }

        private void removeWorkers(Worker worker, int amount) {
            tiles.removeAll(tiles.stream()
                    .filter(tile -> tile.getWorker() == worker)
                    .limit(amount)
                    .collect(Collectors.toList()));
        }

        private void removeHazards(HazardType hazardType, int amount) {
            tiles.removeAll(tiles.stream()
                    .filter(tile -> tile.getHazard() != null && tile.getHazard().getType() == hazardType)
                    .limit(amount)
                    .collect(Collectors.toList()));
        }

        private void removeHazardsOfEachType(int amount) {
            for (var hazardType : HazardType.values()) {
                removeHazards(hazardType, amount);
            }
        }

        private void removeWorkersOfEachType(int amount) {
            for (var worker : Worker.values()) {
                removeWorkers(worker, amount);
            }
        }

        private void removeHazardsOfEachTypeWithPointsAndHand(int amount, int points, Hand hand) {
            for (var hazardType : HazardType.values()) {
                tiles.removeAll(tiles.stream()
                        .filter(tile -> tile.getHazard() != null && tile.getHazard().getType() == hazardType
                                && tile.getHazard().getPoints() == points
                                && tile.getHazard().getHand() == hand)
                        .limit(amount)
                        .collect(Collectors.toList()));
            }
        }

        private void removeHazardOfEachTypeWithPoints(int amount, int points) {
            for (var hazardType : HazardType.values()) {
                tiles.removeAll(tiles.stream()
                        .filter(tile -> tile.getHazard() != null && tile.getHazard().getType() == hazardType
                                && tile.getHazard().getPoints() == points)
                        .limit(amount)
                        .collect(Collectors.toList()));
            }
        }
    }
}
