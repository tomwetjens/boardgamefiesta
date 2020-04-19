package com.wetjens.gwt;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
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
            CattleMarket cattleMarket = createCattleMarketWithAllCards();

            Set<CattleMarket.PossibleBuy> possibleBuys = cattleMarket.possibleBuys(numberOfCowboys, balance);

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
            CattleMarket cattleMarket = createCattleMarketWithAllCards();
            Set<CattleMarket.PossibleBuy> possibleBuys = cattleMarket.possibleBuys(numberOfCowboys, MAX_COST);

            assertBasics(possibleBuys, cattleMarket, numberOfCowboys, MAX_COST);

            if (numberOfCowboys == 0) {
                assertThat(possibleBuys).isEmpty();
            }

            if (numberOfCowboys >= 1) {
                assertThat(possibleBuys).contains(
                        new CattleMarket.PossibleBuy(singletonList(3), 6, 1),
                        new CattleMarket.PossibleBuy(singletonList(4), MAX_COST, 1));

                if (numberOfCowboys == 1) {
                    assertThat(possibleBuys).hasSize(2);
                }
            }

            if (numberOfCowboys >= 2) {
                assertThat(possibleBuys).contains(
                        new CattleMarket.PossibleBuy(singletonList(3), 3, 2),
                        new CattleMarket.PossibleBuy(singletonList(5), MAX_COST, 2));

                if (numberOfCowboys == 2) {
                    assertThat(possibleBuys).hasSize(4);
                }
            }

            if (numberOfCowboys >= 3) {
                assertThat(possibleBuys).contains(
                        new CattleMarket.PossibleBuy(asList(3, 3), 5, 3),
                        new CattleMarket.PossibleBuy(singletonList(4), 6, 3));

                if (numberOfCowboys == 3) {
                    assertThat(possibleBuys).hasSize(6);
                }
            }

            if (numberOfCowboys >= 4) {
                assertThat(possibleBuys).contains(
                        new CattleMarket.PossibleBuy(singletonList(5), 6, 4));

                if (numberOfCowboys == 4) {
                    assertThat(possibleBuys).hasSize(7);
                }
            }

            if (numberOfCowboys >= 5) {
                assertThat(possibleBuys).contains(
                        new CattleMarket.PossibleBuy(asList(4, 4), 8, 5));

                assertThat(possibleBuys).hasSize(8);
            }
        }

        @Test
        void pair3s() {
            CattleMarket cattleMarket = createCattleMarketWithAllCards();
            Set<CattleMarket.PossibleBuy> possibleBuys = cattleMarket.possibleBuys(3, 6);
            assertThat(possibleBuys).contains(new CattleMarket.PossibleBuy(Arrays.asList(3, 3), 5, 3));
        }

        private void assertBasics(Set<CattleMarket.PossibleBuy> possibleBuys, CattleMarket cattleMarket, int numberOfCowboys, int balance) {
            if (numberOfCowboys > 1) {
                Set<CattleMarket.PossibleBuy> oneLess = cattleMarket.possibleBuys(numberOfCowboys - 1, balance);

                Set<CattleMarket.PossibleBuy> diff = new HashSet<>(possibleBuys);
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

        List<Card.CattleCard> holsteins = createCards(6, CattleType.HOLSTEIN);
        List<Card.CattleCard> westHighlands = createCards(6, CattleType.WEST_HIGHLAND);
        List<Card.CattleCard> texasLonghorns = createCards(4, CattleType.TEXAS_LONGHORN);

        @ParameterizedTest
        @MethodSource("costCombinations")
        void cost(Collection<Card.CattleCard> cattleCards, int numberOfCowboys, Object expectedCostOrMessage) {
            CattleMarket cattleMarket = createCattleMarket(Stream.of(
                    holsteins.stream(),
                    westHighlands.stream(),
                    texasLonghorns.stream()
            ).flatMap(Function.identity()).collect(Collectors.toSet()));

            if (expectedCostOrMessage instanceof Integer) {
                assertThat(cattleMarket.cost(cattleCards, numberOfCowboys)).isEqualTo(expectedCostOrMessage);
            } else {
                assertThatThrownBy(() -> cattleMarket.cost(cattleCards, numberOfCowboys)).hasMessage((String) expectedCostOrMessage);
            }
        }

        private Stream<Arguments> costCombinations() {
            return Stream.of(
                    // Single 3s
                    Arguments.of(holsteins.subList(0, 1), 0, "Not enough cowboys"),
                    Arguments.of(holsteins.subList(0, 1), 1, 6),
                    Arguments.of(holsteins.subList(0, 1), 2, 3),
                    Arguments.of(holsteins.subList(0, 1), 3, 3),
                    Arguments.of(holsteins.subList(0, 1), 4, 3),
                    Arguments.of(holsteins.subList(0, 1), 5, 3),
                    Arguments.of(holsteins.subList(0, 1), 6, 3),

                    // Pair of 3s
                    Arguments.of(holsteins.subList(0, 2), 1, "Not enough cowboys"),
                    Arguments.of(holsteins.subList(0, 2), 2, 12),
                    Arguments.of(holsteins.subList(0, 2), 3, 5),

                    // Pair of 3s and single 3
                    Arguments.of(holsteins.subList(0, 3), 2, "Not enough cowboys"),
                    Arguments.of(holsteins.subList(0, 3), 3, 18),
                    Arguments.of(holsteins.subList(0, 3), 4, 11),
                    Arguments.of(holsteins.subList(0, 3), 5, 8),
                    Arguments.of(holsteins.subList(0, 3), 6, 8),

                    // Two pairs of 3s
                    Arguments.of(holsteins.subList(0, 4), 3, "Not enough cowboys"),
                    Arguments.of(holsteins.subList(0, 4), 4, 24),
                    Arguments.of(holsteins.subList(0, 4), 5, 17),
                    Arguments.of(holsteins.subList(0, 4), 6, 10),

                    // Two pairs of 3s and single 3
                    Arguments.of(holsteins.subList(0, 5), 4, "Not enough cowboys"),
                    Arguments.of(holsteins.subList(0, 5), 5, 30),
                    Arguments.of(holsteins.subList(0, 5), 6, 23),

                    // Three pairs of 3s
                    Arguments.of(holsteins.subList(0, 6), 5, "Not enough cowboys"),
                    Arguments.of(holsteins.subList(0, 6), 6, 36),

                    // Single 4s
                    Arguments.of(westHighlands.subList(0, 1), 0, "Not enough cowboys"),
                    Arguments.of(westHighlands.subList(0, 1), 1, 12),
                    Arguments.of(westHighlands.subList(0, 1), 2, 12),
                    Arguments.of(westHighlands.subList(0, 1), 3, 6),
                    Arguments.of(westHighlands.subList(0, 1), 4, 6),
                    Arguments.of(westHighlands.subList(0, 1), 5, 6),
                    Arguments.of(westHighlands.subList(0, 1), 6, 6),

                    // Single 5s
                    Arguments.of(texasLonghorns.subList(0, 1), 1, "Not enough cowboys"),
                    Arguments.of(texasLonghorns.subList(0, 1), 2, 12),
                    Arguments.of(texasLonghorns.subList(0, 1), 3, 12),
                    Arguments.of(texasLonghorns.subList(0, 1), 4, 6),
                    Arguments.of(texasLonghorns.subList(0, 1), 5, 6),
                    Arguments.of(texasLonghorns.subList(0, 1), 6, 6),

                    // Two 5s
                    Arguments.of(texasLonghorns.subList(0, 2), 3, "Not enough cowboys"),
                    Arguments.of(texasLonghorns.subList(0, 2), 4, 24),
                    Arguments.of(texasLonghorns.subList(0, 2), 5, 24),
                    Arguments.of(texasLonghorns.subList(0, 2), 6, 18),

                    // Three 5s
                    Arguments.of(texasLonghorns.subList(0, 3), 5, "Not enough cowboys"),
                    Arguments.of(texasLonghorns.subList(0, 3), 6, 36),

                    // Four 5s
                    Arguments.of(texasLonghorns.subList(0, 4), 6, "Not enough cowboys"),

                    // One of each
                    Arguments.of(asList(holsteins.get(0), westHighlands.get(0), texasLonghorns.get(0)), 3, "Not enough cowboys"),
                    Arguments.of(asList(holsteins.get(0), westHighlands.get(0), texasLonghorns.get(0)), 4, 30),
                    Arguments.of(asList(holsteins.get(0), westHighlands.get(0), texasLonghorns.get(0)), 5, 27),
                    Arguments.of(asList(holsteins.get(0), westHighlands.get(0), texasLonghorns.get(0)), 6, 24)
            );
        }
    }

    private CattleMarket createCattleMarket(Set<Card.CattleCard> cattleCards) {
        return CattleMarket.builder()
                .market(cattleCards)
                .build();
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
