package com.boardgamefiesta.istanbul.logic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Random;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
class Roll {

    @Getter
    private int die1;
    @Getter
    private int die2;

    static Roll deserialize(JsonObject jsonObject) {
        return new Roll(jsonObject.getInt("die1"), jsonObject.getInt("die2"));
    }

    JsonObjectBuilder serialize(JsonBuilderFactory factory) {
        return factory.createObjectBuilder()
                .add("die1", die1)
                .add("die2", die2);
    }

    static Roll random(Random random) {
        var die1 = random.nextInt(6) + 1;
        var die2 = random.nextInt(6) + 1;
        return new Roll(die1, die2);
    }

    int getTotal() {
        return die1 + die2;
    }

    void turnDie() {
        if (die1 <= die2 && die1 < 4) {
            die1 = 4;
        } else if (die2 < 4) {
            die2 = 4;
        }
    }

    public boolean canTurnDie() {
        return die1 < 4 || die2 < 4;
    }
}
