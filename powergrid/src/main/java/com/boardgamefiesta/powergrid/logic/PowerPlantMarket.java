package com.boardgamefiesta.powergrid.logic;

import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.*;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PowerPlantMarket {

    private final LinkedList<PowerPlant> deck;

    @Getter
    private List<PowerPlant> actual;
    @Getter
    private List<PowerPlant> future;

    private int drawsUntilStep3;

    /**
     * Initializes a default market with a shuffled deck for the given number of players,
     * with the actual offering filled [3,4,5,6] and the future offering filled [7,8,9,10].
     *
     * @param numberOfPlayers
     * @param random
     * @return
     */
    static PowerPlantMarket create(int numberOfPlayers, @NonNull Random random) {
        var shuffled = Arrays.stream(PowerPlant.values())
                .filter(powerPlant -> powerPlant.getCost() > 10 && powerPlant.getCost() != 13)
                .collect(Collectors.toList());
        Collections.shuffle(shuffled);

        var deck = new LinkedList<>(shuffled);

        var desiredDeckSize = deck.size() - (numberOfPlayers == 4 ? 4 : numberOfPlayers < 4 ? 8 : 0);
        while (deck.size() > desiredDeckSize) {
            deck.poll();
        }

        deck.push(PowerPlant.P13);

        return new PowerPlantMarket(deck,
                Arrays.stream(PowerPlant.values())
                        .sorted(Comparator.comparingInt(PowerPlant::getCost))
                        .limit(4)
                        .collect(Collectors.toList()),
                Arrays.stream(PowerPlant.values())
                        .sorted(Comparator.comparingInt(PowerPlant::getCost))
                        .skip(4)
                        .limit(4)
                        .collect(Collectors.toList()),
                deck.size() + 1);
    }

    static PowerPlantMarket deserialize(JsonObject jsonObject) {
        return new PowerPlantMarket(
                jsonObject.getJsonArray("deck").stream()
                        .map(JsonString.class::cast)
                        .map(JsonString::getString)
                        .map(PowerPlant::valueOf)
                        .collect(Collectors.toCollection(LinkedList::new)),
                jsonObject.getJsonArray("actual").stream()
                        .map(JsonString.class::cast)
                        .map(JsonString::getString)
                        .map(PowerPlant::valueOf)
                        .collect(Collectors.toList()),
                jsonObject.getJsonArray("future").stream()
                        .map(JsonString.class::cast)
                        .map(JsonString::getString)
                        .map(PowerPlant::valueOf)
                        .collect(Collectors.toList()),
                jsonObject.getInt("drawsUntilStep3")
        );
    }

    JsonObjectBuilder serialize(JsonBuilderFactory jsonBuilderFactory) {
        var jsonSerializer = JsonSerializer.forFactory(jsonBuilderFactory);

        return jsonBuilderFactory.createObjectBuilder()
                .add("deck", jsonSerializer.fromStrings(deck, PowerPlant::name))
                .add("actual", jsonSerializer.fromStrings(actual, PowerPlant::name))
                .add("future", jsonSerializer.fromStrings(future, PowerPlant::name))
                .add("drawsUntilStep3", drawsUntilStep3);
    }


    /**
     * Takes a power plant from the actual offering and replaces it with one from the deck.
     */
    void take(PowerPlant powerPlant, Random random) {
        removeAndReplace(powerPlant, random);
    }

    private void removeAndReplace(PowerPlant powerPlant, Random random) {
        removeAndReplace(powerPlant, drawsUntilStep3 == 1 ? null : deck.getFirst(), random);
    }

    private void removeAndReplace(PowerPlant powerPlant, PowerPlant replacement, Random random) {
        if (!actual.remove(powerPlant)) {
            throw new PowerGridException(PowerGridError.POWER_PLANT_NOT_AVAILABLE);
        }

        var newActualAndFuture = new ArrayList<PowerPlant>(actual.size() + future.size() + 1);
        newActualAndFuture.addAll(actual);
        newActualAndFuture.addAll(future);

        if (replacement != null) {
            newActualAndFuture.add(replacement);
        }

        newActualAndFuture.sort(Comparator.comparingInt(PowerPlant::getCost));

        actual = new ArrayList<>(newActualAndFuture.subList(0, Math.min(actual.size(), newActualAndFuture.size())));
        future = new ArrayList<>(newActualAndFuture.subList(newActualAndFuture.size(), newActualAndFuture.size() - 1));

        if (drawsUntilStep3 == 1) {
            step3(random);
        }

        drawsUntilStep3 = Math.max(0, drawsUntilStep3 - 1);
    }

    /**
     * Removes the lowest power plant from the market,
     * moves everything in the future offering to the actual offering and reshuffles the deck.
     */
    void step3(Random random) {
        drawsUntilStep3 = 0;

        actual.remove(0);
        Collections.shuffle(deck, random);

        actual.addAll(future);
        future.clear();
    }

    /**
     * Removes all power plants from the actual offering that have a cost lower than or equal to the given cost,
     * and replaces them with new power plants from the deck (that have a cost higher than the given cost).
     */
    void removeLowerOrEqual(int cost, Random random) {
        while (!actual.isEmpty() && actual.get(0).getCost() <= cost) {
            removeAndReplace(actual.get(0), random);
        }
    }

    /**
     * Removes the highest power plant from the future offering, putting it back in the deck under the pile,
     * and replaces it with a new power plant drawn from the deck.
     */
    void removeHighestFuture(Random random) {
        if (!future.isEmpty()) {
            var highest = future.get(future.size() - 1);

            removeAndReplace(highest, random);

            deck.add(highest);
        }
    }

    /**
     * Removes lowest power plant from actual offering and replaces it with a new power plant drawn from the deck.
     */
    void removeLowestAndReplace(Random random) {
        removeAndReplace(actual.get(0), random);
    }

    void removeLowestWithoutReplacement() {
        removeAndReplace(actual.get(0), null);
    }

    public int getDeckSize() {
        return deck.size();
    }
}
