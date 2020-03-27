package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import static java.util.stream.Collectors.counting;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CattleMarket {

    private final int limit;
    private final Queue<Card.CattleCard> drawStack;
    private final Set<Card.CattleCard> market;

    public CattleMarket(int playerCount, Random random) {
        this.limit = playerCount == 2 ? 7 : playerCount == 3 ? 10 : 13;
        this.drawStack = createDrawStack(random);
        this.market = new HashSet<>();

        fillUp();
    }

    public Set<PossibleBuy> possibleBuys(int numberOfCowboys, int balance) {
        return possibleBuysInternal(numberOfCowboys, balance, Collections.unmodifiableSet(market), new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private static Set<PossibleBuy> possibleBuysInternal(int cowboysRemaining, int dollarsRemaining, Set<Card.CattleCard> market, Map<Integer, Set<PossibleBuy>> cache) {
        if (cowboysRemaining == 0) {
            return Collections.emptySet();
        }

        Set<PossibleBuy> result = cache.get(cowboysRemaining);

        if (result == null) {
            Stream<PossibleBuy> otherCombinations = concatStreams(
                    possibleBuysInternal(cowboysRemaining - 1, dollarsRemaining, market, cache).stream(), // If not use all cowboys

                    // Use all cowboys but split differently:
                    IntStream.range(1, cowboysRemaining)
                            .boxed()
                            .flatMap(cowboysUsed -> possibleBuysInternal(cowboysUsed, dollarsRemaining, market, cache)
                                    .stream()
                                    .flatMap(a -> possibleBuysInternal(cowboysRemaining - cowboysUsed, dollarsRemaining - a.cost, a.apply(market), cache)
                                            .stream()
                                            .map(b -> PossibleBuy.combine(a, b)))));

            if (cowboysRemaining == 1) {
                result = concatStreams(
                        otherCombinations,
                        dollarsRemaining < 6 ? Stream.empty() : market.stream()
                                .filter(cattleCard -> cattleCard.getType().getValue() == 3)
                                .map(Card.CattleCard::getType)
                                .map(CattleType::getValue)
                                .map(cattleCard -> new PossibleBuy(Collections.singletonList(cattleCard), 6, 1)),
                        dollarsRemaining < 12 ? Stream.empty() : market.stream()
                                .filter(cattleCard -> cattleCard.getType().getValue() == 4)
                                .map(Card.CattleCard::getType)
                                .map(CattleType::getValue)
                                .map(cattleCard -> new PossibleBuy(Collections.singletonList(cattleCard), 12, 1)))
                        .collect(Collectors.toSet());
            } else if (cowboysRemaining == 2) {
                result = concatStreams(
                        otherCombinations,
                        dollarsRemaining < 3 ? Stream.empty() : market.stream()
                                .map(Card.CattleCard::getType)
                                .map(CattleType::getValue)
                                .filter(breedingValue -> breedingValue == 3)
                                .map(cattleCard -> new PossibleBuy(Collections.singletonList(cattleCard), 3, 2)),
                        dollarsRemaining < 12 ? Stream.empty() : market.stream()
                                .map(Card.CattleCard::getType)
                                .map(CattleType::getValue)
                                .filter(breedingValue -> breedingValue == 5)
                                .map(cattleCard -> new PossibleBuy(Collections.singletonList(cattleCard), 12, 2)))
                        .collect(Collectors.toSet());
            } else if (cowboysRemaining == 3) {
                result = concatStreams(
                        otherCombinations,
                        dollarsRemaining < 5 ? Stream.empty() : market.stream()
                                .filter(cattleCard -> cattleCard.getType().getValue() == 3)
                                .flatMap(a -> market.stream() // Find pair
                                        .filter(cattleCard -> cattleCard.getType().getValue() == a.getType().getValue()) // Find pair of matching breeding value
                                        .filter(b -> b != a) // Should be two different cards to form a pair
                                        .findAny()
                                        .map(Card.CattleCard::getType)
                                        .map(CattleType::getValue)
                                        .map(b -> new PossibleBuy(Arrays.asList(a.getType().getValue(), b), 5, 3))
                                        .stream()),
                        dollarsRemaining < 6 ? Stream.empty() : market.stream()
                                .map(Card.CattleCard::getType)
                                .map(CattleType::getValue)
                                .filter(breedingValue -> breedingValue == 4)
                                .map(cattleCard -> new PossibleBuy(Collections.singletonList(cattleCard), 6, 3)))
                        .collect(Collectors.toSet());
            } else if (cowboysRemaining == 4) {
                result = concatStreams(
                        otherCombinations,
                        dollarsRemaining < 6 ? Stream.empty() : market.stream()
                                .map(Card.CattleCard::getType)
                                .map(CattleType::getValue)
                                .filter(breedingValue -> breedingValue == 5)
                                .map(cattleType -> new PossibleBuy(Collections.singletonList(cattleType), 6, 4)))
                        .collect(Collectors.toSet());
            } else if (cowboysRemaining == 5) {
                result = concatStreams(
                        otherCombinations,
                        dollarsRemaining < 8 ? Stream.empty() : market.stream()
                                .filter(cattleCard -> cattleCard.getType().getValue() == 3)
                                .flatMap(a -> market.stream()
                                        .filter(cattleCard -> cattleCard.getType().getValue() == a.getType().getValue()) // Find pair of matching breeding value
                                        .filter(b -> b != a) // Should be two different cards to form a pair
                                        .findAny()
                                        .map(Card.CattleCard::getType)
                                        .map(CattleType::getValue)
                                        .map(b -> new PossibleBuy(Arrays.asList(a.getType().getValue(), b), 8, 5))
                                        .stream()))
                        .collect(Collectors.toSet());
            } else {
                result = otherCombinations.collect(Collectors.toSet());
            }

            cache.put(cowboysRemaining, result);
        }

        return result;
    }

    private static <T> Stream<T> concatStreams(Stream<T>... streams) {
        return Arrays.stream(streams).flatMap(Function.identity());
    }

    public static int cost(Set<Card.CattleCard> cattleCards, int numberOfCowboys) {
        //TODO
        return 0;
    }

    public static int cost(CattleType cattleType, int numberOfCowboys) {
        if (numberOfCowboys < 1) {
            throw new IllegalStateException("Not enough cowboys");
        }

        switch (cattleType.getValue()) {
            case 3:
                return numberOfCowboys >= 2 ? 3 : 6;
            case 4:
                return numberOfCowboys >= 3 ? 6 : 12;
            case 5:
                if (numberOfCowboys < 2) {
                    throw new IllegalStateException("Not enough cowboys");
                }
                return numberOfCowboys >= 4 ? 6 : 12;
            default:
                throw new IllegalArgumentException("Unsupported cattle type: " + cattleType);
        }
    }

    public ImmediateActions buy(Set<Card.CattleCard> cattleCards, int numberOfCowboys) {
        // TODO
        //Any of the cowboys that you do not put to use buying a cattle card during this action may instead
        //be used to draw 2 cards from the market cattle stack and add them face up to the cattle market
        return null;
    }

    public void fillUp() {
        while (market.size() < limit) {
            market.add(drawStack.poll());
        }
    }

    private static Queue<Card.CattleCard> createDrawStack(Random random) {
        List<Card.CattleCard> cards = createSet();

        Collections.shuffle(cards, random);

        return new LinkedList<>(cards);
    }

    private static List<Card.CattleCard> createSet() {
        List<Card.CattleCard> cards = new ArrayList<>(36);
        // TODO Correct the points for each of the cattle cards
        IntStream.range(0, 7).mapToObj(i -> new Card.CattleCard(CattleType.HOLSTEIN, 3)).forEach(cards::add);
        IntStream.range(0, 7).mapToObj(i -> new Card.CattleCard(CattleType.BROWN_SWISS, 3)).forEach(cards::add);
        IntStream.range(0, 7).mapToObj(i -> new Card.CattleCard(CattleType.AYRSHIRE, 3)).forEach(cards::add);
        IntStream.range(0, 9).mapToObj(i -> new Card.CattleCard(CattleType.WEST_HIGHLAND, 3)).forEach(cards::add);
        IntStream.range(0, 6).mapToObj(i -> new Card.CattleCard(CattleType.TEXAS_LONGHORN, 5)).forEach(cards::add);
        return cards;
    }

    @Value
    public static class PossibleBuy {
        List<Integer> breedingValues;
        int cost;
        int cowboysNeeded;

        public PossibleBuy(List<Integer> breedingValues, int cost, int cowboysNeeded) {
            this.breedingValues = new ArrayList<>(breedingValues);

            // [3,3,4] and [4,3,3] should be considered equal, however duplicates are allowed
            // Therefore sort the list
            this.breedingValues.sort(Integer::compareTo);

            this.cost = cost;
            this.cowboysNeeded = cowboysNeeded;
        }

        public static PossibleBuy combine(PossibleBuy a, PossibleBuy b) {
            return new PossibleBuy(Stream.concat(a.breedingValues.stream(), b.breedingValues.stream()).collect(Collectors.toList()), a.cost + b.cost, a.cowboysNeeded + b.cowboysNeeded);
        }

        private Set<Card.CattleCard> apply(Set<Card.CattleCard> cattleCards) {
            Set<Card.CattleCard> result = new HashSet<>(cattleCards);
            breedingValues.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .forEach((breedingValue, count) -> result.removeAll(result.stream()
                            .filter(cattleCard -> cattleCard.getType().getValue() == breedingValue)
                            .limit(count)
                            .collect(Collectors.toList())));
            return result;
        }
    }
}
