package com.tomsboardgames.istanbul.logic;

import java.io.DataInput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public enum BonusCard {
    GAIN_1_GOOD,
    TAKE_5_LIRA,
    SULTAN_2X,
    POST_OFFICE_2X,
    GEMSTONE_DEALER_2X,
    FAMILY_MEMBER_TO_POLICE_STATION,
    MOVE_0,
    MOVE_3_OR_4,
    RETURN_1_ASSISTANT,
    SMALL_MARKET_ANY_GOOD;

    static Collection<BonusCard> createDeck() {
        return Stream.of(
                IntStream.range(0, 4).mapToObj(i -> GAIN_1_GOOD),
                IntStream.range(0, 4).mapToObj(i -> TAKE_5_LIRA),
                IntStream.range(0, 2).mapToObj(i -> SULTAN_2X),
                IntStream.range(0, 2).mapToObj(i -> POST_OFFICE_2X),
                IntStream.range(0, 2).mapToObj(i -> GEMSTONE_DEALER_2X),
                IntStream.range(0, 2).mapToObj(i -> FAMILY_MEMBER_TO_POLICE_STATION),
                IntStream.range(0, 2).mapToObj(i -> MOVE_0),
                IntStream.range(0, 4).mapToObj(i -> MOVE_3_OR_4),
                IntStream.range(0, 2).mapToObj(i -> RETURN_1_ASSISTANT),
                IntStream.range(0, 2).mapToObj(i -> SMALL_MARKET_ANY_GOOD))
                .flatMap(Function.identity())
                .collect(Collectors.toList());

    }

}
