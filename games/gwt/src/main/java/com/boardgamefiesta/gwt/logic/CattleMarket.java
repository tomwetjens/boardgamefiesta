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

import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class CattleMarket {

    private final Queue<Card.CattleCard> drawStack;
    private final Set<Card.CattleCard> market;
    private final boolean simmental;

    private static final Map<Integer, Set<Cost>> COSTS = Map.of(
            2, Set.of(
                    Cost.single(8, 1),
                    Cost.single(5, 2)
            ),
            3, Set.of(
                    Cost.single(6, 1),
                    Cost.single(3, 2),
                    Cost.pair(5, 3),
                    // 2 singles:
                    Cost.pair(12, 2),
                    Cost.pair(9, 3),
                    Cost.pair(6, 4)
            ),
            4, Set.of(
                    Cost.single(12, 1),
                    Cost.single(6, 3),
                    Cost.pair(8, 5),
                    // 2 singles:
                    Cost.pair(12, 6),
                    Cost.pair(24, 2),
                    Cost.pair(18, 4)
            ),
            5, Set.of(
                    Cost.single(12, 2),
                    Cost.single(6, 4),
                    // 2 singles:
                    Cost.pair(18, 6),
                    Cost.pair(24, 4)
            ));

    static CattleMarket original(int playerCount, boolean simmental, Random random) {
        return forDrawStack(playerCount, createDrawStack(4, simmental, random), simmental);
    }

    static CattleMarket balanced(int playerCount, boolean simmental, Random random) {
        return forDrawStack(playerCount, createDrawStack(playerCount, simmental, random), simmental);
    }

    private static CattleMarket forDrawStack(int playerCount, Queue<Card.CattleCard> drawStack, boolean simmental) {
        var cattleMarket = new CattleMarket(drawStack, new HashSet<>(), simmental);

        cattleMarket.fillUp(playerCount);

        return cattleMarket;
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("drawStack", serializer.fromCollection(drawStack, Card.CattleCard::serialize))
                .add("market", serializer.fromCollection(market, Card.CattleCard::serialize))
                .add("simmental", simmental)
                .build();
    }

    static CattleMarket deserialize(JsonObject jsonObject) {
        return new CattleMarket(
                jsonObject.getJsonArray("drawStack").stream()
                        .map(JsonValue::asJsonObject)
                        .map(Card.CattleCard::deserialize)
                        .collect(Collectors.toCollection(LinkedList::new)),
                jsonObject.getJsonArray("market").stream()
                        .map(JsonValue::asJsonObject)
                        .map(Card.CattleCard::deserialize)
                        .collect(Collectors.toSet()),
                jsonObject.getBoolean("simmental", false));
    }

    public Set<Card.CattleCard> getMarket() {
        return Collections.unmodifiableSet(market);
    }

    public Stream<PossibleBuy> possibleBuys(int numberOfCowboys, int balance) {
        if (numberOfCowboys == 0 || balance < 3) {
            return Stream.empty();
        }

        return market.stream()
                .mapToInt(Card.CattleCard::getValue)
                .boxed()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(count -> count.getValue() > 0)
                .flatMap(count -> COSTS.get(count.getKey()).stream()
                        .filter(cost -> cost.getCowboys() <= numberOfCowboys)
                        .filter(cost -> cost.getDollars() <= balance)
                        .filter(cost -> !cost.isPair() || count.getValue() >= 2)
                        .map(cost -> new PossibleBuy(count.getKey(), cost.isPair(), cost.getDollars(), cost.getCowboys())));
    }

    public int getDrawStackSize() {
        return drawStack.size();
    }

    public Set<Card.CattleCard> getCardsInDrawStack() {
        return Collections.unmodifiableSet(new HashSet<>(drawStack));
    }

    @Value
    public static class Cost {
        int dollars;
        int cowboys;
        boolean pair;

        static Cost pair(int dollars, int cowboys) {
            return new Cost(dollars, cowboys, true);
        }

        static Cost single(int dollars, int cowboys) {
            return new Cost(dollars, cowboys, false);
        }
    }

    Cost buy(@NonNull Card.CattleCard card, Card.CattleCard secondCard, int cowboys, int dollars) {
        if (secondCard != null && (secondCard == card || secondCard.getValue() != card.getValue())) {
            throw new GWTException(GWTError.NOT_PAIR);
        }

        var result = COSTS.get(card.getValue()).stream()
                .filter(cost -> cost.isPair() == (secondCard != null))
                .filter(cost -> cost.getCowboys() == cowboys)
                .filter(cost -> cost.getDollars() == dollars)
                .findAny()
                .orElseThrow(() -> new GWTException(GWTError.CANNOT_PERFORM_ACTION));

        take(card);

        if (secondCard != null) {
            take(secondCard);
        }

        return result;
    }

    void take(Card.CattleCard card) {
        if (!market.remove(card)) {
            throw new GWTException(GWTError.CATTLE_CARD_NOT_AVAILABLE);
        }
    }

    void fillUp(int playerCount) {
        var limit = simmental
                ? playerCount == 2 ? 9 : playerCount == 3 ? 12 : 15
                : playerCount == 2 ? 7 : playerCount == 3 ? 10 : 13;
        while (market.size() < limit && !drawStack.isEmpty()) {
            draw();
        }
    }

    Optional<Card.CattleCard> draw() {
        if (!drawStack.isEmpty()) {
            var card = drawStack.poll();
            market.add(card);
            return Optional.of(card);
        }
        return Optional.empty();
    }

    private static Queue<Card.CattleCard> createDrawStack(int playerCount, boolean simmental, Random random) {
        List<Card.CattleCard> cards = createSet(playerCount, simmental);

        Collections.shuffle(cards, random);

        return new LinkedList<>(cards);
    }

    private static List<Card.CattleCard> createSet(int playerCount, boolean simmental) {
        List<Card.CattleCard> cards = new ArrayList<>(36);

        // 2/3p variant to approximate ratios of 4P game, courtesy of Fernando Moritz
        // Alternative: https://boardgamegeek.com/image/3336021/proezas

        // 2P: remove 3 of each
        // 3P: remove 2 of each
        var a = playerCount == 4 ? 7 : playerCount == 3 ? 5 : 4;
        IntStream.range(0, a).mapToObj(i -> new Card.CattleCard(CattleType.HOLSTEIN, 1, 3)).forEach(cards::add);
        IntStream.range(0, a).mapToObj(i -> new Card.CattleCard(CattleType.BROWN_SWISS, 2, 3)).forEach(cards::add);
        IntStream.range(0, a).mapToObj(i -> new Card.CattleCard(CattleType.AYRSHIRE, 3, 3)).forEach(cards::add);

        // 2P: remove 3+3+5+5
        // 3P: remove 3+5
        var b = playerCount == 4 ? 3 : playerCount == 3 ? 2 : 1;
        IntStream.range(0, b).mapToObj(i -> new Card.CattleCard(CattleType.WEST_HIGHLAND, 3, 4)).forEach(cards::add);
        IntStream.range(0, 3).mapToObj(i -> new Card.CattleCard(CattleType.WEST_HIGHLAND, 4, 4)).forEach(cards::add);
        IntStream.range(0, b).mapToObj(i -> new Card.CattleCard(CattleType.WEST_HIGHLAND, 5, 4)).forEach(cards::add);

        // 2P: remove 5+7
        // 3P: remove 6
        IntStream.range(0, playerCount == 2 ? 1 : 2).mapToObj(i -> new Card.CattleCard(CattleType.TEXAS_LONGHORN, 5, 5)).forEach(cards::add);
        IntStream.range(0, playerCount == 3 ? 1 : 2).mapToObj(i -> new Card.CattleCard(CattleType.TEXAS_LONGHORN, 6, 5)).forEach(cards::add);
        IntStream.range(0, playerCount == 2 ? 1 : 2).mapToObj(i -> new Card.CattleCard(CattleType.TEXAS_LONGHORN, 7, 5)).forEach(cards::add);

        if (simmental) {
            IntStream.range(0, playerCount == 4 ? 8 : playerCount == 3 ? 6 : 5).mapToObj(i -> new Card.CattleCard(CattleType.SIMMENTAL, 3, 2)).forEach(cards::add);
        }

        return cards;
    }

    @Value
    public static class PossibleBuy {
        int breedingValue;
        boolean pair;
        int dollars;
        int cowboys;
    }

}
