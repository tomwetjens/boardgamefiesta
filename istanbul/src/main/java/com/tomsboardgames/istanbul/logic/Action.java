package com.tomsboardgames.istanbul.logic;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Action implements com.tomsboardgames.api.Action, Serializable {

    private static final long serialVersionUID = 1L;

    abstract ActionResult perform(Game game, Random random);

    @SuppressWarnings("unchecked")
    protected <T extends Place> T expectCurrentPlace(Game game, T... anyOfPlaces) {
        var currentPlace = game.getCurrentPlace();

        if (!Arrays.<Place>asList(anyOfPlaces).contains(currentPlace)) {
            throw new IstanbulException(IstanbulError.NOT_AT_PLACE);
        }

        return (T) currentPlace;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Move extends Action {
        Place to;

        @Override
        ActionResult perform(Game game, Random random) {
            var from = game.getCurrentPlace();

            if (game.distance(from, to) > 2) {
                throw new IstanbulException(IstanbulError.PLACE_NOT_REACHABLE);
            }

            return game.move(from, to);
        }
    }

    public static class LeaveAssistant extends Action {

        @Override
        ActionResult perform(Game game, Random random) {
            var place = game.getCurrentPlace();

            return place.leaveAssistant(game.currentPlayerState().getMerchant(), game);
        }
    }

    public static class PayOtherMerchants extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var currentPlayerState = game.currentPlayerState();
            var place = game.getCurrentPlace();

            var otherMerchants = place.getMerchants().stream()
                    .filter(merchant -> merchant != currentPlayerState.getMerchant())
                    .collect(Collectors.toList());

            if (otherMerchants.size() * 2 > currentPlayerState.getLira()) {
                throw new IstanbulException(IstanbulError.NOT_ENOUGH_LIRA);
            }

            otherMerchants.forEach(otherMerchant -> {
                currentPlayerState.payLira(2);

                otherMerchant.getPlayer().ifPresentOrElse(player -> game.getPlayerState(player).gainLira(2),
                        // In 2P variant, if neutral, pay to bank then randomly place somewhere else
                        () -> repositionDummyMerchant(otherMerchant, game, random));
            });

            return place.placeActions(game);
        }

        private void repositionDummyMerchant(Merchant otherMerchant, Game game, Random random) {
            game.getCurrentPlace(otherMerchant.getColor()).takeMerchant(otherMerchant);
            game.randomPlace(random).placeMerchant(otherMerchant, game);
        }
    }

    public static class Governor extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var bonusCard = game.drawBonusCard(random);

            game.currentPlayerState().addBonusCard(bonusCard);

            game.getPlace(Place::isGovernor).takeGovernor();
            game.randomPlace(random).placeGovernor();

            return ActionResult.followUp(PossibleAction.choice(Set.of(Action.Pay2Lira.class, Action.DiscardBonusCard.class)));
        }

    }

    public static class Smuggler extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            game.getPlace(Place::isSmuggler).takeSmuggler();
            game.randomPlace(random).placeSmuggler();

            return ActionResult.followUp(
                    PossibleAction.whenThen(PossibleAction.optional(takeAnyGood()),
                            PossibleAction.choice(Set.of(Action.Pay2Lira.class, Action.Pay1Good.class))));
        }

        private static PossibleAction takeAnyGood() {
            return PossibleAction.choice(Set.of(Take1Fabric.class, Take1Spice.class, Take1Fruit.class, Take1Blue.class));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class TakeMosqueTile extends Action {
        MosqueTile mosqueTile;

        @Override
        ActionResult perform(Game game, Random random) {
            return expectCurrentPlace(game, game.getGreatMosque(), game.getSmallMosque()).takeMosqueTile(mosqueTile, game);
        }
    }

    public static class BuyWheelbarrowExtension extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var currentPlayerState = game.currentPlayerState();
            currentPlayerState.payLira(7);
            currentPlayerState.addExtension();
            return ActionResult.none();
        }
    }

    private static class MaxGoods extends Action {

        private final GoodsType goodsType;

        MaxGoods(GoodsType goodsType) {
            this.goodsType = goodsType;
        }

        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().maxGoods(goodsType);

            if (game.currentPlayerState().hasMosqueTile(MosqueTile.PAY_2_LIRA_FOR_1_ADDITIONAL_GOOD)) {
                return ActionResult.followUp(PossibleAction.optional(Action.Pay2LiraFor1AdditionalGood.class));
            }
            return ActionResult.none();
        }
    }

    public static class MaxFabric extends MaxGoods {
        public MaxFabric() {
            super(GoodsType.FABRIC);
        }
    }

    public static class MaxSpice extends MaxGoods {
        public MaxSpice() {
            super(GoodsType.SPICE);
        }
    }

    public static class MaxFruit extends MaxGoods {
        public MaxFruit() {
            super(GoodsType.FRUIT);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Pay2LiraFor1AdditionalGood extends Action {
        GoodsType goodsType;

        @Override
        ActionResult perform(Game game, Random random) {
            var currentPlayerState = game.currentPlayerState();
            currentPlayerState.payLira(2);
            currentPlayerState.addGoods(goodsType, 1);
            return ActionResult.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Pay2LiraToReturnAssistant extends Action {
        int x, y;

        @Override
        ActionResult perform(Game game, Random random) {
            var place = game.getPlace(x, y);

            var currentPlayerState = game.currentPlayerState();

            currentPlayerState.payLira(2);
            place.returnAssistant(currentPlayerState.getMerchant());

            return ActionResult.none();
        }
    }

    public static class UsePostOffice extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var postOffice = game.getPostOffice();

            return postOffice.use(game);
        }
    }

    public static class CatchFamilyMemberForBonusCard extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var place = game.getCurrentPlace();

            place.catchFamilyMember(game);

            var bonusCard = game.drawBonusCard(random);
            game.currentPlayerState().addBonusCard(bonusCard);

            return ActionResult.none();
        }
    }

    public static class CatchFamilyMemberFor3Lira extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var place = game.getCurrentPlace();

            place.catchFamilyMember(game);

            game.currentPlayerState().gainLira(3);

            return ActionResult.none();
        }
    }

    public static class Pay2Lira extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().payLira(2);
            return ActionResult.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class DiscardBonusCard extends Action {
        BonusCard bonusCard;

        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().discardBonusCard(bonusCard);
            return ActionResult.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Pay1Good extends Action {
        GoodsType goodsType;

        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().removeGoods(goodsType, 1);
            return null;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Take2BonusCards extends Action {
        boolean fromCaravansary;

        @Override
        ActionResult perform(Game game, Random random) {
            var currentPlayerState = game.currentPlayerState();

            if (fromCaravansary) {
                var caravansary = game.getCaravansary();
                currentPlayerState.addBonusCard(caravansary.drawBonusCard());
                currentPlayerState.addBonusCard(caravansary.drawBonusCard());
            } else {
                currentPlayerState.addBonusCard(game.drawBonusCard(random));
                currentPlayerState.addBonusCard(game.drawBonusCard(random));
            }

            return ActionResult.followUp(PossibleAction.mandatory(Action.DiscardBonusCard.class));
        }
    }

    public static class ReturnAllAssistants extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            Place.Fountain.returnAllAssistants(game.currentPlayerState(), game.getLayout());

            return ActionResult.none();
        }
    }

    public static class Take1Fabric extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().addGoods(GoodsType.FABRIC, 1);
            return ActionResult.none();
        }
    }

    public static class Take1Spice extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().addGoods(GoodsType.SPICE, 1);
            return ActionResult.none();
        }
    }

    public static class Take1Blue extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().addGoods(GoodsType.BLUE, 1);
            return ActionResult.none();
        }
    }

    public static class Take1Fruit extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().addGoods(GoodsType.FRUIT, 1);
            return ActionResult.none();
        }
    }

    public static class RollForBlueGoods extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            Place.BlackMarket.rollForBlueGoods(game.currentPlayerState(), random);

            return ActionResult.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class GuessAndRollForLira extends Action {
        int guess;

        public GuessAndRollForLira(int guess) {
            if (guess < 3 || guess > 12) {
                throw new IstanbulException(IstanbulError.INVALID_GUESS);
            }
            this.guess = guess;
        }

        @Override
        ActionResult perform(Game game, Random random) {
            var teaHouse = game.getTeaHouse();

            teaHouse.guessAndRoll(game.currentPlayerState(), guess, random);

            return ActionResult.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class SellGoods extends Action {
        Map<GoodsType, Integer> goods;

        @Override
        ActionResult perform(Game game, Random random) {
            expectCurrentPlace(game, game.getSmallMarket(), game.getLargeMarket())
                    .sellGoods(game, goods);

            return ActionResult.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class SendFamilyMember extends Action {
        Place to;

        @Override
        ActionResult perform(Game game, Random random) {
            var policeStation = game.getPoliceStation();

            if (to == policeStation) {
                throw new IstanbulException(IstanbulError.ALREADY_AT_PLACE);
            }

            policeStation.takeFamilyMember(game.getCurrentPlayer());

            return to.sendFamilyMember(game, game.getCurrentPlayer());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class DeliverToSultan extends Action {
        Set<GoodsType> preferredGoodsTypes;

        @Override
        ActionResult perform(Game game, Random random) {
            var sultansPalace = game.getSultansPalace();

            sultansPalace.deliverToSultan(game.currentPlayerState(), preferredGoodsTypes);

            return ActionResult.none();
        }
    }

    public static class BuyRuby extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var gemstoneDealer = game.getGemstoneDealer();

            gemstoneDealer.buy(game.currentPlayerState());

            return ActionResult.none();
        }
    }
}
