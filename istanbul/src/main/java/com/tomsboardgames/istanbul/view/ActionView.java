package com.tomsboardgames.istanbul.view;

import com.tomsboardgames.istanbul.logic.*;
import lombok.AllArgsConstructor;

import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.Map;
import java.util.Set;
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
    DELIVER_TO_SULTAN(Action.DeliverToSultan.class, (jsonObject, game) -> new Action.DeliverToSultan(getGoodsTypes(jsonObject))),
    BONUS_CARD_DELIVER_TO_SULTAN(Action.BonusCardDeliverToSultan.class, (jsonObject, game) -> new Action.BonusCardDeliverToSultan(getGoodsTypes(jsonObject))),
    DISCARD_BONUS_CARD(Action.DiscardBonusCard.class, (jsonObject, game) -> new Action.DiscardBonusCard(getBonusCard(jsonObject))),
    GOVERNOR(Action.Governor.class, (jsonObject, game) -> new Action.Governor()),
    GUESS_AND_ROLL_FOR_LIRA(Action.GuessAndRollForLira.class, (jsonObject, game) -> new Action.GuessAndRollForLira(jsonObject.getInt("guess"))),
    LEAVE_ASSISTANT(Action.LeaveAssistant.class, (jsonObject, game) -> new Action.LeaveAssistant()),
    MAX_FABRIC(Action.MaxFabric.class, (jsonObject, game) -> new Action.MaxFabric()),
    MAX_FRUIT(Action.MaxFruit.class, (jsonObject, game) -> new Action.MaxFruit()),
    MAX_SPICE(Action.MaxSpice.class, (jsonObject, game) -> new Action.MaxSpice()),
    MOVE(Action.Move.class, (jsonObject, game) -> new Action.Move(getPlace(jsonObject, game), getBonusCard(jsonObject))),
    PAY_1_GOOD(Action.Pay1Good.class, (jsonObject, game) -> new Action.Pay1Good(getGoodsType(jsonObject))),
    PAY_2_LIRA(Action.Pay2Lira.class, (jsonObject, game) -> new Action.Pay2Lira()),
    PAY_2_LIRA_FOR_1_ADDITIONAL_GOOD(Action.Pay2LiraFor1AdditionalGood.class, (jsonObject, game) -> new Action.Pay2LiraFor1AdditionalGood()),
    PAY_2_LIRA_TO_RETURN_ASSISTANT(Action.Pay2LiraToReturnAssistant.class, (jsonObject, game) -> new Action.Pay2LiraToReturnAssistant(jsonObject.getInt("x"), jsonObject.getInt("y"))),
    PAY_OTHER_MERCHANTS(Action.PayOtherMerchants.class, (jsonObject, game) -> new Action.PayOtherMerchants()),
    RETURN_ALL_ASSISTANTS(Action.ReturnAllAssistants.class, (jsonObject, game) -> new Action.ReturnAllAssistants()),
    ROLL_FOR_BLUE_GOODS(Action.RollForBlueGoods.class, (jsonObject, game) -> new Action.RollForBlueGoods()),
    SELL_GOODS(Action.SellGoods.class, (jsonObject, game) -> new Action.SellGoods(getGoods(jsonObject), getBonusCard(jsonObject))),
    SEND_FAMILY_MEMBER(Action.SendFamilyMember.class, (jsonObject, game) -> new Action.SendFamilyMember(getPlace(jsonObject, game))),
    SMUGGLER(Action.Smuggler.class, (jsonObject, game) -> new Action.Smuggler()),
    TAKE_1_FABRIC(Action.Take1Fabric.class, (jsonObject, game) -> new Action.Take1Fabric()),
    TAKE_1_FRUIT(Action.Take1Fruit.class, (jsonObject, game) -> new Action.Take1Fruit()),
    TAKE_1_SPICE(Action.Take1Spice.class, (jsonObject, game) -> new Action.Take1Spice()),
    TAKE_2_BONUS_CARDS(Action.Take2BonusCards.class, (jsonObject, game) -> new Action.Take2BonusCards(jsonObject.getBoolean("caravansary"))),
    BONUS_CARD_TAKE_5_LIRA(Action.BonusCardTake5Lira.class, (jsonObject, game) -> new Action.BonusCardTake5Lira()),
    TAKE_MOSQUE_TILE(Action.TakeMosqueTile.class, (jsonObject, game) -> new Action.TakeMosqueTile(MosqueTile.valueOf(jsonObject.getString("mosqueTile")))),
    USE_POST_OFFICE(Action.UsePostOffice.class, (jsonObject, game) -> new Action.UsePostOffice()),
    BONUS_CARD_USE_POST_OFFICE(Action.BonusCardUsePostOffice.class, (jsonObject, game) -> new Action.BonusCardUsePostOffice()),
    BONUS_CARD_GAIN_1_GOOD(Action.BonusCardGain1Good.class, (jsonObject, game) -> new Action.BonusCardGain1Good());

    private static GoodsType getGoodsType(JsonObject jsonObject) {
        return GoodsType.valueOf(jsonObject.getString("goodsType"));
    }

    private static BonusCard getBonusCard(JsonObject jsonObject) {
        return jsonObject.containsKey("bonusCard") ? BonusCard.valueOf(jsonObject.getString("bonusCard")) : null;
    }

    private static Set<GoodsType> getGoodsTypes(JsonObject jsonObject) {
        return jsonObject.getJsonArray("preferredGoodsTypes")
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .map(GoodsType::valueOf)
                .collect(Collectors.toSet());
    }

    private static Map<GoodsType, Integer> getGoods(JsonObject jsonObject) {
        return jsonObject.getJsonArray("goods").getValuesAs(JsonString.class).stream()
                .map(JsonString::getString)
                .map(GoodsType::valueOf)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().intValue()));
    }

    private static Place getPlace(JsonObject jsonObject, Game game) {
        var x = jsonObject.getInt("x");
        var y = jsonObject.getInt("y");
        return game.getLayout()[x][y];
    }

    private final Class<? extends Action> actionClass;
    private final BiFunction<JsonObject, Game, Action> actionFunction;

    public Action toAction(JsonObject jsonObject, Game state) {
        return actionFunction.apply(jsonObject, state);
    }

    public static ActionView of(Class<? extends Action> actionClass) {
        for (ActionView value : values()) {
            if (value.actionClass == actionClass) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown action: " + actionClass);
    }
}
