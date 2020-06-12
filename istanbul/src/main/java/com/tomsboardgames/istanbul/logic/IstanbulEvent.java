package com.tomsboardgames.istanbul.logic;

import com.tomsboardgames.api.InGameEvent;
import com.tomsboardgames.api.Player;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IstanbulEvent implements InGameEvent {

    Player player;
    String type;
    List<String> parameters;

    static IstanbulEvent create(Player player, Type type, int... values) {
        return new IstanbulEvent(player, type.name(), Arrays.stream(values)
                .mapToObj(Integer::toString)
                .collect(Collectors.toUnmodifiableList()));
    }

    public enum Type {
        ROLLED,
        TURNED_DIE,
        GUESSED
    }
}
