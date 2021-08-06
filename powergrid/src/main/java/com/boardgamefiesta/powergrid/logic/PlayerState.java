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

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.repository.JsonDeserializer;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerState {

    @Getter
    int balance;

    Map<PowerPlant, List<ResourceType>> powerPlants;

    static PlayerState create(Player player) {
        return new PlayerState(50, new HashMap<>());
    }

    static PlayerState deserialize(JsonObject jsonObject) {
        return new PlayerState(
                jsonObject.getInt("balance"),
                JsonDeserializer.forObject(jsonObject.getJsonObject("powerPlants"))
                        .asMap(PowerPlant::valueOf, jsonValue -> jsonValue.asJsonArray().stream()
                                .map(JsonString.class::cast)
                                .map(JsonString::getString)
                                .map(ResourceType::valueOf)
                                .collect(Collectors.toList()))
        );
    }

    JsonObject serialize(JsonBuilderFactory jsonBuilderFactory) {
        var jsonSerializer = JsonSerializer.forFactory(jsonBuilderFactory);
        return jsonBuilderFactory.createObjectBuilder()
                .add("balance", balance)
                .add("powerPlants", jsonSerializer.fromMap(powerPlants, PowerPlant::name,
                        resources -> jsonSerializer.fromStrings(resources, ResourceType::name)))
                .build();
    }

    void pay(int amount) {
        if (amount > balance) {
            throw new PowerGridException(PowerGridError.BALANCE_TOO_LOW);
        }

        balance -= amount;
    }

    void addPowerPlant(PowerPlant powerPlant) {
        powerPlants.put(powerPlant, new ArrayList<>());
    }

    void removePowerPlant(PowerPlant powerPlant) {
        // TODO
    }

    int producePower(Map<ResourceType, Integer> resources) {
        // TODO
        return 0;
    }

    void earn(int amount) {
        balance += amount;
    }

    void removeResource(ResourceType resourceType, int amount) {

    }

    public Set<PowerPlant> getPowerPlants() {
        return powerPlants.keySet();
    }

    public List<ResourceType> getResources(PowerPlant powerPlant) {
        return powerPlants.get(powerPlant);
    }
}
