package com.boardgamefiesta.gwt.logic;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CattleMarketTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class PossibleBuys {

        static final int MAX_COST = 12;

        @ParameterizedTest(name = "{0} cowboys ${1}")
        @MethodSource("allCombinationsOfCowboysAndBalance")
        void basics(int numberOfCowboys, int balance) {
            var cattleMarket = createCattleMarketWithAllCards();

            var possibleBuys = cattleMarket.possibleBuys(numberOfCowboys, balance).collect(Collectors.toSet());

            assertBasics(possibleBuys, cattleMarket, numberOfCowboys, balance);
        }

        Stream<Arguments> allCombinationsOfCowboysAndBalance() {
            return IntStream.rangeClosed(1, 6)
                    .boxed()
                    .flatMap(numberOfCowboys -> IntStream.rangeClosed(0, MAX_COST)
                            .mapToObj(balance -> Arguments.of(numberOfCowboys, balance)));
        }

        @ParameterizedTest(name = "{0} cowboys ${1}")
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
        void all(int numberOfCowboys) {
            var cattleMarket = createCattleMarketWithAllCards();
            var possibleBuys = cattleMarket.possibleBuys(numberOfCowboys, MAX_COST).collect(Collectors.toSet());

            assertBasics(possibleBuys, cattleMarket, numberOfCowboys, MAX_COST);

            if (numberOfCowboys == 0) {
                assertThat(possibleBuys).isEmpty();
            }

            if (numberOfCowboys >= 1) {
                assertThat(possibleBuys).contains(
                        new CattleMarket.PossibleBuy(3, false, 6, 1),
                        new CattleMarket.PossibleBuy(4, false, MAX_COST, 1));

                if (numberOfCowboys == 1) {
                    assertThat(possibleBuys).hasSize(2);
                }
            }

            if (numberOfCowboys >= 2) {
                assertThat(possibleBuys).contains(
                        new CattleMarket.PossibleBuy(3, false, 3, 2),
                        new CattleMarket.PossibleBuy(5, false, MAX_COST, 2));

                if (numberOfCowboys == 2) {
                    assertThat(possibleBuys).hasSize(4);
                }
            }

            if (numberOfCowboys >= 3) {
                assertThat(possibleBuys).contains(
                        new CattleMarket.PossibleBuy(3, true, 5, 3),
                        new CattleMarket.PossibleBuy(4, false, 6, 3));

                if (numberOfCowboys == 3) {
                    assertThat(possibleBuys).hasSize(6);
                }
            }

            if (numberOfCowboys >= 4) {
                assertThat(possibleBuys).contains(
                        new CattleMarket.PossibleBuy(5, false, 6, 4));

                if (numberOfCowboys == 4) {
                    assertThat(possibleBuys).hasSize(7);
                }
            }

            if (numberOfCowboys >= 5) {
                assertThat(possibleBuys).contains(
                        new CattleMarket.PossibleBuy(4, true, 8, 5));

                assertThat(possibleBuys).hasSize(8);
            }
        }

        @Test
        void pair3s() {
            CattleMarket cattleMarket = createCattleMarketWithAllCards();
            var possibleBuys = cattleMarket.possibleBuys(3, 6).collect(Collectors.toSet());
            assertThat(possibleBuys).contains(new CattleMarket.PossibleBuy(3, true, 5, 3));
        }

        private void assertBasics(Set<CattleMarket.PossibleBuy> possibleBuys, CattleMarket cattleMarket, int numberOfCowboys, int balance) {
            if (numberOfCowboys > 1) {
                var oneLess = cattleMarket.possibleBuys(numberOfCowboys - 1, balance).collect(Collectors.toSet());

                var diff = new HashSet<>(possibleBuys);
                diff.removeAll(oneLess);

                assertThat(diff).allSatisfy(pb -> assertThat(pb.getCowboysNeeded()).isEqualTo(numberOfCowboys));
            }

            assertThat(possibleBuys).allSatisfy(pb -> assertThat(pb.getCowboysNeeded()).isLessThanOrEqualTo(numberOfCowboys));
            assertThat(possibleBuys).allSatisfy(pb -> assertThat(pb.getCost()).isLessThanOrEqualTo(balance));
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Cost {

        @ParameterizedTest
        @MethodSource("costCombinations")
        void cost(int breedingValue, boolean pair, int numberOfCowboys, Object expectedCostOrMessage) {
            if (expectedCostOrMessage instanceof Integer) {
                assertThat(CattleMarket.cost(breedingValue, pair, numberOfCowboys, Integer.MAX_VALUE, CattleMarket.CostPreference.CHEAPEST).getDollars()).isEqualTo(expectedCostOrMessage);
            } else {
                assertThatThrownBy(() -> CattleMarket.cost(breedingValue, pair, numberOfCowboys, Integer.MAX_VALUE, CattleMarket.CostPreference.CHEAPEST))
                        .hasMessage(GWTError.NOT_ENOUGH_COWBOYS.toString());
            }
        }

        private Stream<Arguments> costCombinations() {
            return Stream.of(
                    // Single 3s
                    Arguments.of(3, false, 0, "Not enough cowboys"),
                    Arguments.of(3, false, 1, 6),
                    Arguments.of(3, false, 2, 3),
                    Arguments.of(3, false, 3, 3),
                    Arguments.of(3, false, 4, 3),
                    Arguments.of(3, false, 5, 3),
                    Arguments.of(3, false, 6, 3),

                    // Pair of 3s
                    Arguments.of(3, true, 1, "Not enough cowboys"),
                    Arguments.of(3, true, 2, 12),
                    Arguments.of(3, true, 3, 5),

                    // Single 4s
                    Arguments.of(4, false, 0, "Not enough cowboys"),
                    Arguments.of(4, false, 1, 12),
                    Arguments.of(4, false, 2, 12),
                    Arguments.of(4, false, 3, 6),
                    Arguments.of(4, false, 4, 6),
                    Arguments.of(4, false, 5, 6),
                    Arguments.of(4, false, 6, 6),

                    // Single 5s
                    Arguments.of(5, false, 1, "Not enough cowboys"),
                    Arguments.of(5, false, 2, 12),
                    Arguments.of(5, false, 3, 12),
                    Arguments.of(5, false, 4, 6),
                    Arguments.of(5, false, 5, 6),
                    Arguments.of(5, false, 6, 6),

                    // Two 5s
                    Arguments.of(5, true, 3, "Not enough cowboys"),
                    Arguments.of(5, true, 4, 24),
                    Arguments.of(5, true, 5, 24),
                    Arguments.of(5, true, 6, 18)
            );
        }
    }

    private CattleMarket createCattleMarket(Set<Card.CattleCard> cattleCards) {
        return new CattleMarket(2, new LinkedList<>(), cattleCards);
    }

    private CattleMarket createCattleMarketWithAllCards() {
        return createCattleMarket(Stream.of(
                // Assuming unlimited money, add enough cattle of each breeding value to simulate all possible buys
                createCards(6, CattleType.HOLSTEIN).stream(),
                createCards(6, CattleType.WEST_HIGHLAND).stream(),
                createCards(3, CattleType.TEXAS_LONGHORN).stream()
        ).flatMap(Function.identity()).collect(Collectors.toSet()));
    }

    private static List<Card.CattleCard> createCards(int count, CattleType cattleType) {
        return IntStream.range(0, count).mapToObj(i -> new Card.CattleCard(cattleType, 3)).collect(Collectors.toList());
    }
}
