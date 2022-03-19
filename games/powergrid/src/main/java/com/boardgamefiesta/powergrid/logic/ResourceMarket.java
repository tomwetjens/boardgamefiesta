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

package com.boardgamefiesta.powergrid.logic;

import com.boardgamefiesta.api.repository.JsonDeserializer;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class ResourceMarket {

    private static final Map<ResourceType, List<List<Integer>>> FILL_UP = Map.of(
            ResourceType.COAL, List.of(
                    List.of(3, 4, 3), // 2 players
                    List.of(4, 5, 3), // 3 players
                    List.of(5, 6, 4), // etc.
                    List.of(5, 7, 5),
                    List.of(7, 9, 6)
            ),
            ResourceType.OIL, List.of(
                    List.of(2, 2, 4), // 2 players
                    List.of(2, 3, 4), // 3 players
                    List.of(3, 4, 5), // etc.
                    List.of(4, 5, 6),
                    List.of(5, 6, 7)
            ),
            ResourceType.BIO_MASS, List.of(
                    List.of(1, 2, 3), // 2 players
                    List.of(1, 2, 3), // 3 players
                    List.of(2, 3, 4), // etc.
                    List.of(3, 3, 5),
                    List.of(3, 5, 6)
            ),
            ResourceType.URANIUM, List.of(
                    List.of(1, 1, 1), // 2 players
                    List.of(1, 1, 1), // 3 players
                    List.of(1, 2, 2), // etc.
                    List.of(2, 3, 2),
                    List.of(2, 3, 3)
            )
    );

    private static final List<Space> DEFAULT = IntStream.rangeClosed(1, 8).mapToObj(cost -> Space.of(3, cost)).collect(Collectors.toList());

    private static final Map<ResourceType, List<Space>> SPACES = Map.of(
            ResourceType.COAL, DEFAULT,
            ResourceType.OIL, DEFAULT,
            ResourceType.BIO_MASS, DEFAULT,
            ResourceType.URANIUM, Stream.concat(
                    IntStream.rangeClosed(1, 8).mapToObj(cost -> Space.of(1, cost)),
                    IntStream.rangeClosed(10, 16).filter(cost -> cost % 2 == 0).mapToObj(cost -> Space.of(1, cost)))
                    .collect(Collectors.toList())
    );

    private final Map<ResourceType, Integer> available;

    static ResourceMarket create() {
        var available = new HashMap<ResourceType, Integer>();
        available.put(ResourceType.COAL, 24);
        available.put(ResourceType.OIL, 18);
        available.put(ResourceType.BIO_MASS, 6);
        available.put(ResourceType.URANIUM, 2);
        return new ResourceMarket(available);
    }

    static ResourceMarket deserialize(JsonObject jsonObject) {
        return new ResourceMarket(JsonDeserializer.forObject(jsonObject).asIntegerMap(ResourceType::valueOf));
    }

    JsonObject serialize(JsonBuilderFactory jsonBuilderFactory) {
        var jsonSerializer = JsonSerializer.forFactory(jsonBuilderFactory);
        return jsonSerializer.fromIntegerMap(available, ResourceType::name);
    }

    public int getCapacity(ResourceType resourceType) {
        return resourceType == ResourceType.URANIUM ? 12 : 24;
    }

    public int getAvailable(ResourceType resourceType) {
        return available.getOrDefault(resourceType, 0);
    }

    public int calculateCost(ResourceType resourceType, int amount) {
        var spaces = SPACES.get(resourceType);
        var capacity = getCapacity(resourceType);

        int remaining = available.get(resourceType);
        int cost = 0;
        while (amount > 0) {
            if (remaining < amount) {
                throw new PowerGridException(PowerGridError.NOT_ENOUGH_AVAILABLE);
            }

            var space = spaces.get(spaces.size() - (int) Math.ceil(((float) remaining / capacity) * spaces.size()));
            cost += space.getCost();

            amount--;
            remaining--;
        }

        return cost;
    }

    private void add(ResourceType resourceType, int amount) {
        available.put(resourceType, Math.min(available.getOrDefault(resourceType, 0) + amount, getCapacity(resourceType)));
    }

    void fillUp(int numberOfPlayers, int step) {
        FILL_UP.forEach((resourceType, amounts) -> {
            add(resourceType, amounts.get(numberOfPlayers - 1).get(step - 1));
        });
    }

    void remove(ResourceType resourceType, int amount) {
        var remaining = available.getOrDefault(resourceType, 0) - amount;

        if (remaining < 0) {
            throw new PowerGridException(PowerGridError.NOT_ENOUGH_AVAILABLE);
        }

        available.put(resourceType, remaining);
    }

    @Value(staticConstructor = "of")
    private static class Space {
        int capacity;
        int cost;
    }
}
