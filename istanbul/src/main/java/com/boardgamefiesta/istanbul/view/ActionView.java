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

package com.boardgamefiesta.istanbul.view;

import com.boardgamefiesta.istanbul.logic.*;
import lombok.AllArgsConstructor;

import javax.json.*;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
public enum ActionView {

    BUY_RUBY(Action.BuyRuby.class, (jsonObject, game) -> new Action.BuyRuby()),
    BONUS_CARD_BUY_RUBY(Action.BonusCardBuyRuby.class, (jsonObject, game) -> new Action.BonusCardBuyRuby()),
    BUY_WHEELBARROW_EXTENSION(Action.BuyWheelbarrowExtension.class, (jsonObject, game) -> new Action.BuyWheelbarrowExtension()),
    CATCH_FAMILY_MEMBER_FOR_3_LIRA(Action.CatchFamilyMemberFor3Lira.class, (jsonObject, game) -> new Action.CatchFamilyMemberFor3Lira()),
    CATCH_FAMILY_MEMBER_FOR_BONUS_CARD(Action.CatchFamilyMemberForBonusCard.class, (jsonObject, game) -> new Action.CatchFamilyMemberForBonusCard()),
    DELIVER_TO_SULTAN(Action.DeliverToSultan.class, (jsonObject, game) -> new Action.DeliverToSultan()),
    BONUS_CARD_DELIVER_TO_SULTAN(Action.BonusCardDeliverToSultan.class, (jsonObject, game) -> new Action.BonusCardDeliverToSultan()),
    DISCARD_BONUS_CARD(Action.DiscardBonusCard.class, (jsonObject, game) -> new Action.DiscardBonusCard(getBonusCard(jsonObject))),
    GOVERNOR(Action.Governor.class, (jsonObject, game) -> new Action.Governor()),
    GUESS_AND_ROLL_FOR_LIRA(Action.GuessAndRollForLira.class, (jsonObject, game) -> new Action.GuessAndRollForLira(jsonObject.getInt("guess"))),
    LEAVE_ASSISTANT(Action.LeaveAssistant.class, (jsonObject, game) -> new Action.LeaveAssistant()),
    MAX_FABRIC(Action.MaxFabric.class, (jsonObject, game) -> new Action.MaxFabric()),
    MAX_FRUIT(Action.MaxFruit.class, (jsonObject, game) -> new Action.MaxFruit()),
    MAX_SPICE(Action.MaxSpice.class, (jsonObject, game) -> new Action.MaxSpice()),
    MOVE(Action.Move.class, (jsonObject, game) -> new Action.Move(place(jsonObject, game), getBonusCard(jsonObject))),
    PAY_1_FABRIC(Action.Pay1Fabric.class, (jsonObject, game) -> new Action.Pay1Fabric()),
    PAY_1_FRUIT(Action.Pay1Fruit.class, (jsonObject, game) -> new Action.Pay1Fruit()),
    PAY_1_SPICE(Action.Pay1Spice.class, (jsonObject, game) -> new Action.Pay1Spice()),
    PAY_1_BLUE(Action.Pay1Blue.class, (jsonObject, game) -> new Action.Pay1Blue()),
    PAY_2_LIRA(Action.Pay2Lira.class, (jsonObject, game) -> new Action.Pay2Lira()),
    PAY_2_LIRA_FOR_1_ADDITIONAL_GOOD(Action.Pay2LiraFor1AdditionalGood.class, (jsonObject, game) -> new Action.Pay2LiraFor1AdditionalGood()),
    PAY_2_LIRA_TO_RETURN_ASSISTANT(Action.Pay2LiraToReturnAssistant.class, (jsonObject, game) -> new Action.Pay2LiraToReturnAssistant(jsonObject.getInt("x"), jsonObject.getInt("y"))),
    PAY_OTHER_MERCHANTS(Action.PayOtherMerchants.class, (jsonObject, game) -> new Action.PayOtherMerchants()),
    RETURN_ALL_ASSISTANTS(Action.ReturnAllAssistants.class, (jsonObject, game) -> new Action.ReturnAllAssistants()),
    ROLL_FOR_BLUE_GOODS(Action.RollForBlueGoods.class, (jsonObject, game) -> new Action.RollForBlueGoods()),
    REROLL_FOR_BLUE_GOODS(Action.RerollForBlueGoods.class, (jsonObject, game) -> new Action.RerollForBlueGoods()),
    NO_REROLL_FOR_BLUE_GOODS(Action.NoRerollForBlueGoods.class, (jsonObject, game) -> new Action.NoRerollForBlueGoods()),
    SELL_GOODS(Action.SellGoods.class, (jsonObject, game) -> new Action.SellGoods(getGoods(jsonObject), getBonusCard(jsonObject))),
    SEND_FAMILY_MEMBER(Action.SendFamilyMember.class, (jsonObject, game) -> new Action.SendFamilyMember(place(jsonObject, game))),
    SMUGGLER(Action.Smuggler.class, (jsonObject, game) -> new Action.Smuggler()),
    TAKE_1_FABRIC(Action.Take1Fabric.class, (jsonObject, game) -> new Action.Take1Fabric()),
    TAKE_1_FRUIT(Action.Take1Fruit.class, (jsonObject, game) -> new Action.Take1Fruit()),
    TAKE_1_SPICE(Action.Take1Spice.class, (jsonObject, game) -> new Action.Take1Spice()),
    TAKE_1_BLUE(Action.Take1Blue.class, (jsonObject, game) -> new Action.Take1Blue()),
    TAKE_BONUS_CARD_CARAVANSARY(Action.TakeBonusCardCaravansary.class, (jsonObject, game) -> new Action.TakeBonusCardCaravansary(jsonObject.getBoolean("caravansary"))),
    BONUS_CARD_TAKE_5_LIRA(Action.BonusCardTake5Lira.class, (jsonObject, game) -> new Action.BonusCardTake5Lira()),
    TAKE_MOSQUE_TILE(Action.TakeMosqueTile.class, (jsonObject, game) -> new Action.TakeMosqueTile(getEnum(jsonObject, "mosqueTile", MosqueTile.class))),
    USE_POST_OFFICE(Action.UsePostOffice.class, (jsonObject, game) -> new Action.UsePostOffice()),
    BONUS_CARD_USE_POST_OFFICE(Action.BonusCardUsePostOffice.class, (jsonObject, game) -> new Action.BonusCardUsePostOffice()),
    BONUS_CARD_GAIN_1_GOOD(Action.BonusCardGain1Good.class, (jsonObject, game) -> new Action.BonusCardGain1Good()),
    TAKE_3_LIRA(Action.Take3Lira.class, (jsonObject, game) -> new Action.Take3Lira()),
    TAKE_BONUS_CARD(Action.TakeBonusCard.class, (jsonObject, game) -> new Action.TakeBonusCard()),
    PLACE_MEMBER_ON_POLICE_STATION(Action.PlaceFamilyMemberOnPoliceStation.class, (jsonObject, game) -> new Action.PlaceFamilyMemberOnPoliceStation()),
    RETURN_1_ASSISTANT(Action.BonusCardReturnAssistant.class, (jsonObject, game) -> new Action.BonusCardReturnAssistant(place(jsonObject, game)));

    private static BonusCard getBonusCard(JsonObject jsonObject) {
        return jsonObject.containsKey("bonusCard") ? BonusCard.valueOf(jsonObject.getString("bonusCard")) : null;
    }

    private static Map<GoodsType, Integer> getGoods(JsonObject jsonObject) {
        return jsonObject.getJsonArray("goods").getValuesAs(JsonString.class).stream()
                .map(JsonString::getString)
                .map(GoodsType::valueOf)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().intValue()));
    }

    private static Place place(JsonObject jsonObject, Istanbul game) {
        var x = getInt(jsonObject,"x");
        var y = getInt(jsonObject,"y");
        return game.place(x, y);
    }

    private final Class<? extends Action> actionClass;
    private final BiFunction<JsonObject, Istanbul, Action> actionFunction;

    public Action toAction(JsonObject jsonObject, Istanbul state) {
        return actionFunction.apply(jsonObject, state);
    }

    public static ActionView of(Class<? extends Action> actionClass) {
        for (ActionView value : values()) {
            if (value.actionClass == actionClass) {
                return value;
            }
        }
        throw new JsonException("Unknown action: " + actionClass);
    }

    private static String getString(JsonObject jsonObject, String key) {
        JsonValue jsonValue = getValue(jsonObject, key);

        if (jsonValue.getValueType() != JsonValue.ValueType.STRING) {
            throw new JsonException("Property '" + key + "' expected to be string, but was: " + jsonValue.getValueType() + " in JSON object: " + jsonObject);
        }

        return ((JsonString) jsonValue).getString();
    }

    private static int getInt(JsonObject jsonObject, String key) {
        JsonValue jsonValue = getValue(jsonObject, key);

        if (jsonValue.getValueType() != JsonValue.ValueType.NUMBER) {
            throw new JsonException("Property '" + key + "' expected to be number, but was: " + jsonValue.getValueType() + " in JSON object: " + jsonObject);
        }

        return ((JsonNumber) jsonValue).intValue();
    }

    private static <E extends Enum<E>> E getEnum(JsonObject jsonObject, String key, Class<E> enumType) {
        try {
            return Enum.valueOf(enumType, getString(jsonObject, key));
        } catch (IllegalArgumentException e) {
            throw new JsonException("Property '" + key + "' invalid in JSON object: " + jsonObject);
        }
    }

    private static List<JsonString> getJsonStrings(JsonObject jsonObject, String key) {
        return getJsonArray(jsonObject, key).getValuesAs(JsonString.class);
    }

    private static JsonArray getJsonArray(JsonObject jsonObject, String key) {
        JsonValue jsonValue = getValue(jsonObject, key);

        if (jsonValue.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new JsonException("Property '" + key + "' expected to be array, but was: " + jsonValue.getValueType() + " in JSON object: " + jsonObject);
        }

        return jsonValue.asJsonArray();
    }

    private static JsonObject getJsonObject(JsonObject jsonObject, String key) {
        JsonValue jsonValue = getValue(jsonObject, key);

        if (jsonValue.getValueType() != JsonValue.ValueType.OBJECT) {
            throw new JsonException("Property '" + key + "' expected to be object, but was: " + jsonValue.getValueType() + " in JSON object: " + jsonObject);
        }

        return jsonValue.asJsonObject();
    }

    private static JsonValue getValue(JsonObject jsonObject, String key) {
        var jsonValue = jsonObject.get(key);
        if (jsonValue == null) {
            throw new JsonException("Property '" + key + "' missing in JSON object: " + jsonObject);
        }
        return jsonValue;
    }

}
