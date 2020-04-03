package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.ToString;

@ToString
public final class KansasCitySupply {

    private final Queue<Tile>[] drawPiles;

    @SuppressWarnings("unchecked")
    KansasCitySupply(Random random) {
        this.drawPiles = new Queue[3];
        this.drawPiles[0] = createDrawPile(createSet1(), random);
        this.drawPiles[1] = createDrawPile(createSet2(), random);
        this.drawPiles[2] = createDrawPile(createSet3(), random);
    }

    Tile draw(int drawPileIndex) {
        return drawPiles[drawPileIndex].poll();
    }

    private Queue<Tile> createDrawPile(List<Tile> list, Random random) {
        Collections.shuffle(list, random);
        return new LinkedList<>(list);
    }

    private ArrayList<Tile> createSet1() {
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

    private ArrayList<Tile> createSet2() {
        return Arrays.stream(Worker.values())
                .flatMap(type -> IntStream.range(0, 11).mapToObj(i -> new Tile(type)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private ArrayList<Tile> createSet3() {
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
    }
}
