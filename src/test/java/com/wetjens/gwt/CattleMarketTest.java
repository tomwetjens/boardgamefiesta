package com.wetjens.gwt;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import static org.assertj.core.api.Assertions.*;

class CattleMarketTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class PossibleBuys {

        CattleMarket cattleMarket = CattleMarket.builder()
                .market(Stream.of(
                        // Assuming unlimited money, add enough cattle of each breeding value to simulate all possible buys
                        createCards(6, CattleType.HOLSTEIN).stream(),
                        createCards(6, CattleType.WEST_HIGHLAND).stream(),
                        createCards(3, CattleType.TEXAS_LONGHORN).stream()
                ).flatMap(Function.identity()).collect(Collectors.toSet()))
                .build();

        @ParameterizedTest
        @MethodSource("allCombinationsOfCowboysAndBalance")
        void generics(int numberOfCowboys, int balance) {
            Set<CattleMarket.PossibleBuy> possibleBuys = cattleMarket.possibleBuys(numberOfCowboys, balance);

            assertGenerics(possibleBuys, numberOfCowboys, balance);
        }

        Stream<Arguments> allCombinationsOfCowboysAndBalance() {
            return IntStream.rangeClosed(1, 6)
                    .boxed()
                    .flatMap(numberOfCowboys -> IntStream.rangeClosed(0, 50)
                            .mapToObj(balance -> Arguments.of(numberOfCowboys, balance)));
        }

        @Test
        void twoThrees() {
            CattleMarket cattleMarket = CattleMarket.builder()
                    .market(createCards(2, CattleType.HOLSTEIN))
                    .build();

            Set<CattleMarket.PossibleBuy> possibleBuys = cattleMarket.possibleBuys(6, Integer.MAX_VALUE);

            assertThat(possibleBuys).containsExactlyInAnyOrder(
                    new CattleMarket.PossibleBuy(Arrays.asList(3, 3), 5, 3),
                    new CattleMarket.PossibleBuy(Arrays.asList(3), 3, 2),
                    new CattleMarket.PossibleBuy(Arrays.asList(3), 6, 1));
        }

        @Test
        void twoFours() {
            CattleMarket cattleMarket = CattleMarket.builder()
                    .market(createCards(2, CattleType.WEST_HIGHLAND))
                    .build();

            Set<CattleMarket.PossibleBuy> possibleBuys = cattleMarket.possibleBuys(6, Integer.MAX_VALUE);

            assertThat(possibleBuys).containsExactlyInAnyOrder(
                    new CattleMarket.PossibleBuy(Arrays.asList(4, 4), 8, 5),
                    new CattleMarket.PossibleBuy(Arrays.asList(4), 6, 3),
                    new CattleMarket.PossibleBuy(Arrays.asList(4), 12, 1));
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4, 5, 6})
        void unlimitedDollars(int numberOfCowboys) {
            int balance = Integer.MAX_VALUE;

            Set<CattleMarket.PossibleBuy> possibleBuys = cattleMarket.possibleBuys(numberOfCowboys, balance);

            assertGenerics(possibleBuys, numberOfCowboys, balance);

            assertThat(possibleBuys).contains(
                    new CattleMarket.PossibleBuy(Arrays.asList(3), 6, 1),
                    new CattleMarket.PossibleBuy(Arrays.asList(4), 12, 1));

            if (numberOfCowboys == 1) {
                assertThat(possibleBuys).hasSize(2);
            }

            if (numberOfCowboys >= 2) {
                assertThat(possibleBuys).contains(
                        new CattleMarket.PossibleBuy(Arrays.asList(3), 3, 2),
                        new CattleMarket.PossibleBuy(Arrays.asList(3, 3), 12, 2),
                        new CattleMarket.PossibleBuy(Arrays.asList(3, 4), 18, 2),
                        new CattleMarket.PossibleBuy(Arrays.asList(4, 4), 24, 2),
                        new CattleMarket.PossibleBuy(Arrays.asList(5), 12, 2));
            }
            if (numberOfCowboys == 2) {
                assertThat(possibleBuys).hasSize(7);
            }

            if (numberOfCowboys >= 3) {
                assertThat(possibleBuys).contains(
                        new CattleMarket.PossibleBuy(Arrays.asList(3, 3), 5, 3),
                        new CattleMarket.PossibleBuy(Arrays.asList(3, 3), 9, 3),
                        new CattleMarket.PossibleBuy(Arrays.asList(3, 3, 3), 18, 3),
                        new CattleMarket.PossibleBuy(Arrays.asList(3, 3, 4), 24, 3),
                        new CattleMarket.PossibleBuy(Arrays.asList(3, 4), 15, 3),
                        new CattleMarket.PossibleBuy(Arrays.asList(3, 4, 4), 30, 3),
                        new CattleMarket.PossibleBuy(Arrays.asList(4), 6, 3),
                        new CattleMarket.PossibleBuy(Arrays.asList(4, 4, 4), 36, 3),
                        new CattleMarket.PossibleBuy(Arrays.asList(3, 5), 18, 3),
                        new CattleMarket.PossibleBuy(Arrays.asList(4, 5), 24, 3));
            }
            if (numberOfCowboys == 3) {
                assertThat(possibleBuys).hasSize(17);
            }

            if (numberOfCowboys >= 4) {

            }
            if (numberOfCowboys == 4) {
                assertThat(possibleBuys).hasSize(36);
            }

            if (numberOfCowboys >= 5) {

            }
            if (numberOfCowboys == 5) {
                assertThat(possibleBuys).hasSize(67);
            }

            if (numberOfCowboys == 6) {
                assertThat(possibleBuys).contains(new CattleMarket.PossibleBuy(Arrays.asList(4, 4, 4, 4, 4, 4), 72, 6));
                assertThat(possibleBuys).hasSize(118);
            }
        }

        private void assertGenerics(Set<CattleMarket.PossibleBuy> possibleBuys, int numberOfCowboys, int balance) {
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

    private Set<Card.CattleCard> createCards(int count, CattleType cattleType) {
        return IntStream.range(0, count).mapToObj(i -> new Card.CattleCard(cattleType, 3)).collect(Collectors.toSet());
    }

    @Nested
    class CostSet {
        @Test
        void cost() {
            assertThat(CattleMarket.cost(CattleType.HOLSTEIN, 1)).isEqualTo(6);
            assertThat(CattleMarket.cost(CattleType.HOLSTEIN, 2)).isEqualTo(3);
            assertThat(CattleMarket.cost(CattleType.HOLSTEIN, 3)).isEqualTo(3);
            assertThat(CattleMarket.cost(CattleType.HOLSTEIN, 4)).isEqualTo(3);
            assertThat(CattleMarket.cost(CattleType.HOLSTEIN, 5)).isEqualTo(3);
            assertThat(CattleMarket.cost(CattleType.HOLSTEIN, 6)).isEqualTo(3);
        }
    }

    @Nested
    class CostSingle {
        @Test
        void cost3() {
            assertThat(CattleMarket.cost(CattleType.HOLSTEIN, 1)).isEqualTo(6);
            assertThat(CattleMarket.cost(CattleType.HOLSTEIN, 2)).isEqualTo(3);
            assertThat(CattleMarket.cost(CattleType.HOLSTEIN, 3)).isEqualTo(3);
            assertThat(CattleMarket.cost(CattleType.HOLSTEIN, 4)).isEqualTo(3);
            assertThat(CattleMarket.cost(CattleType.HOLSTEIN, 5)).isEqualTo(3);
            assertThat(CattleMarket.cost(CattleType.HOLSTEIN, 6)).isEqualTo(3);
        }

        @Test
        void cost4() {
            assertThat(CattleMarket.cost(CattleType.WEST_HIGHLAND, 1)).isEqualTo(12);
            assertThat(CattleMarket.cost(CattleType.WEST_HIGHLAND, 2)).isEqualTo(12);
            assertThat(CattleMarket.cost(CattleType.WEST_HIGHLAND, 3)).isEqualTo(6);
            assertThat(CattleMarket.cost(CattleType.WEST_HIGHLAND, 4)).isEqualTo(6);
            assertThat(CattleMarket.cost(CattleType.WEST_HIGHLAND, 5)).isEqualTo(6);
            assertThat(CattleMarket.cost(CattleType.WEST_HIGHLAND, 6)).isEqualTo(6);
        }

        @Test
        void cost5() {
            assertThatThrownBy(() -> CattleMarket.cost(CattleType.TEXAS_LONGHORN, 1)).isInstanceOf(IllegalStateException.class);
            assertThat(CattleMarket.cost(CattleType.TEXAS_LONGHORN, 2)).isEqualTo(12);
            assertThat(CattleMarket.cost(CattleType.TEXAS_LONGHORN, 3)).isEqualTo(12);
            assertThat(CattleMarket.cost(CattleType.TEXAS_LONGHORN, 4)).isEqualTo(6);
            assertThat(CattleMarket.cost(CattleType.TEXAS_LONGHORN, 5)).isEqualTo(6);
            assertThat(CattleMarket.cost(CattleType.TEXAS_LONGHORN, 6)).isEqualTo(6);
        }
    }
}
