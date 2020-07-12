package com.boardgamefiesta.gwt;

import com.boardgamefiesta.json.JsonSerializer;
import lombok.*;

import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ToString
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class KansasCitySupply {

    private final List<Queue<Tile>> drawPiles;

    KansasCitySupply(Random random) {
        this(List.of(
                createDrawPile(createSet1(), random),
                createDrawPile(createSet2(), random),
                createDrawPile(createSet3(), random)));
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("drawPiles", serializer.fromCollection(drawPiles, (drawPile, g) ->
                        serializer.fromCollection(drawPile, Tile::serialize)))
                .build();
    }

    static KansasCitySupply deserialize(JsonObject jsonObject) {
        return builder()
                .drawPiles(jsonObject.getJsonArray("drawPiles").stream()
                        .map(JsonValue::asJsonArray)
                        .map(KansasCitySupply::deserializeDrawPile)
                        .collect(Collectors.toList()))
                .build();
    }

    private static Queue<Tile> deserializeDrawPile(JsonArray jsonArray) {
        return jsonArray.stream().map(JsonValue::asJsonObject)
                .map(Tile::deserialize)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    Tile draw(int drawPileIndex) {
        return drawPiles.get(drawPileIndex).poll();
    }

    private static Queue<Tile> createDrawPile(List<Tile> list, Random random) {
        Collections.shuffle(list, random);
        return new LinkedList<>(list);
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
                .flatMap(type -> IntStream.range(0, 11).mapToObj(i -> new Tile(type)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static ArrayList<Tile> createSet3() {
        return Stream.concat(
                Stream.concat(
                        Arrays.stream(Worker.values()).flatMap(type -> IntStream.range(0, 7).mapToObj(i -> new Tile(type))),
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

        private Tile(Worker worker) {
            this.worker = worker;
            this.hazard = null;
            this.teepee = null;
        }

        private Tile(Hazard hazard) {
            this.hazard = hazard;
            this.worker = null;
            this.teepee = null;
        }

        private Tile(Teepee teepee) {
            this.teepee = teepee;
            this.hazard = null;
            this.worker = null;
        }

        static Tile deserialize(JsonObject jsonObject) {
            var worker = jsonObject.getString("worker");
            if (worker != null) {
                return new Tile(Worker.valueOf(worker));
            }

            var teepee = jsonObject.getString("teepee");
            if (teepee != null) {
                return new Tile(Teepee.valueOf(teepee));
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
}
