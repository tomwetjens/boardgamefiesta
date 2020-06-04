package com.tomsboardgames.istanbul.view;

import com.tomsboardgames.istanbul.logic.*;
import lombok.AllArgsConstructor;

import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@AllArgsConstructor
public enum ActionView {

    BUY_RUBY(Action.BuyRuby.class, (jsonObject, game) -> new Action.BuyRuby()),
    BUY_WHEELBARROW_EXTENSION(Action.BuyWheelbarrowExtension.class, (jsonObject, game) -> new Action.BuyWheelbarrowExtension()),
    CATCH_FAMILY_MEMBER_FOR_3_LIRA(Action.CatchFamilyMemberFor3Lira.class, (jsonObject, game) -> new Action.CatchFamilyMemberFor3Lira()),
    CATCH_FAMILY_MEMBER_FOR_BONUS_CARD(Action.CatchFamilyMemberForBonusCard.class, (jsonObject, game) -> new Action.CatchFamilyMemberForBonusCard()),
    DELIVER_TO_SULTAN(Action.DeliverToSultan.class, (jsonObject, game) -> new Action.DeliverToSultan(
            jsonObject.getJsonArray("preferredGoodsTypes")
                    .getValuesAs(JsonString.class)
                    .stream()
                    .map(JsonString::getString)
                    .map(GoodsType::valueOf)
                    .collect(Collectors.toSet()))),
    DISCARD_BONUS_CARD(Action.DiscardBonusCard.class, (jsonObject, game) -> new Action.DiscardBonusCard(BonusCard.valueOf(jsonObject.getString("bonusCard")))),
    GOVERNOR(Action.Governor.class, (jsonObject, game) -> new Action.Governor()),
    GUESS_AND_ROLL_FOR_LIRA(Action.GuessAndRollForLira.class, (jsonObject, game) -> new Action.GuessAndRollForLira(jsonObject.getInt("guess"))),
    LEAVE_ASSISTANT(Action.LeaveAssistant.class, (jsonObject, game) -> new Action.LeaveAssistant()),
    MAX_FABRIC(Action.MaxFabric.class, (jsonObject, game) -> new Action.MaxFabric()),
    MAX_FRUIT(Action.MaxFruit.class, (jsonObject, game) -> new Action.MaxFruit()),
    MAX_SPICE(Action.MaxSpice.class, (jsonObject, game) -> new Action.MaxSpice()),
    MOVE(Action.Move.class, (jsonObject, game) -> new Action.Move(jsonObject.getInt("x"), jsonObject.getInt("y"))),
    PAY_1_GOOD(Action.Pay1Good.class, (jsonObject, game) -> new Action.Pay1Good(GoodsType.valueOf(jsonObject.getString("goodsType")))),
    PAY_2_LIRA(Action.Pay2Lira.class, (jsonObject, game) -> new Action.Pay2Lira()),
    PAY_2_LIRA_FOR_1_ADDITIONAL_GOOD(Action.Pay2LiraFor1AdditionalGood.class, (jsonObject, game) -> new Action.Pay2LiraFor1AdditionalGood(GoodsType.valueOf(jsonObject.getString("goodsType")))),
    PAY_2_LIRA_TO_RETURN_ASSISTANT(Action.Pay2LiraToReturnAssistant.class, (jsonObject, game) -> new Action.Pay2LiraToReturnAssistant(jsonObject.getInt("x"), jsonObject.getInt("y"))),
    PAY_OTHER_MERCHANTS(Action.PayOtherMerchants.class, (jsonObject, game) -> new Action.PayOtherMerchants()),
    RETURN_ALL_ASSISTANTS(Action.ReturnAllAssistants.class, (jsonObject, game) -> new Action.ReturnAllAssistants()),
    ROLL_FOR_BLUE_GOODS(Action.RollForBlueGoods.class, (jsonObject, game) -> new Action.RollForBlueGoods()),
    SELL_GOODS(Action.SellGoods.class, (jsonObject, game) -> new Action.SellGoods(getGoods(jsonObject.getJsonObject("goods")))),
    SEND_FAMILY_MEMBER(Action.SendFamilyMember.class, (jsonObject, game) -> new Action.SendFamilyMember(jsonObject.getInt("x"), jsonObject.getInt("y"))),
    SMUGGLER(Action.Smuggler.class, (jsonObject, game) -> new Action.Smuggler(GoodsType.valueOf(jsonObject.getString("goodsType")))),
    TAKE_1_FABRIC(Action.Take1Fabric.class, (jsonObject, game) -> new Action.Take1Fabric()),
    TAKE_1_FRUIT(Action.Take1Fruit.class, (jsonObject, game) -> new Action.Take1Fruit()),
    TAKE_1_SPICE(Action.Take1Spice.class, (jsonObject, game) -> new Action.Take1Spice()),
    TAKE_2_BONUS_CARDS(Action.Take2BonusCards.class, (jsonObject, game) -> new Action.Take2BonusCards(jsonObject.getBoolean("fromCaravansary"))),
    TAKE_MOSQUE_TILE(Action.TakeMosqueTile.class, (jsonObject, game) -> new Action.TakeMosqueTile(MosqueTile.valueOf(jsonObject.getString("mosqueTile")))),
    USE_POST_OFFICE(Action.UsePostOffice.class, (jsonObject, game) -> new Action.UsePostOffice());

    private static Map<GoodsType, Integer> getGoods(JsonObject goods) {
        return goods.keySet().stream()
                .collect(Collectors.toMap(GoodsType::valueOf, goods::getInt));
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
