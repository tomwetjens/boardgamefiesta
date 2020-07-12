package com.boardgamefiesta.istanbul.logic;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Action implements com.boardgamefiesta.api.Action {

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
    @AllArgsConstructor
    public static class Move extends Action {
        @NonNull
        Place to;

        BonusCard bonusCard;

        public Move(@NonNull Place to) {
            this.to = to;
            this.bonusCard = null;
        }

        @Override
        ActionResult perform(Game game, Random random) {
            if (bonusCard == BonusCard.MOVE_0) {
                game.currentPlayerState().removeBonusCard(BonusCard.MOVE_0);
                return game.moveMerchant(game.getCurrentMerchant(), to, 0, 0);
            } else if (bonusCard == BonusCard.MOVE_3_OR_4) {
                game.currentPlayerState().removeBonusCard(BonusCard.MOVE_3_OR_4);
                return game.moveMerchant(game.getCurrentMerchant(), to, 3, 4);
            } else {
                return game.moveMerchant(game.getCurrentMerchant(), to, 1, 2);
            }
        }
    }

    public static class LeaveAssistant extends Action {

        @Override
        ActionResult perform(Game game, Random random) {
            var place = game.getCurrentPlace();
            return place.leaveAssistant(place.getMerchant(game.getCurrentPlayer().getColor()), game);
        }
    }

    public static class PayOtherMerchants extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var currentPlayerState = game.currentPlayerState();
            var place = game.getCurrentPlace();

            var otherMerchants = place.getMerchants().stream()
                    .filter(merchant -> merchant.getColor() != game.getCurrentPlayer().getColor())
                    .collect(Collectors.toList());

            if (otherMerchants.size() * 2 > currentPlayerState.getLira()) {
                throw new IstanbulException(IstanbulError.NOT_ENOUGH_LIRA);
            }

            otherMerchants.forEach(otherMerchant -> {
                currentPlayerState.payLira(2);

                otherMerchant.getPlayer().ifPresentOrElse(player -> game.getPlayerState(player).gainLira(2),
                        // In 2P variant, if neutral, pay to bank then randomly place somewhere else
                        () -> game.moveMerchant(otherMerchant, game.randomPlace(random), 0, 4));
            });

            return place.placeActions(game);
        }

    }

    public static class Governor extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var bonusCard = game.drawBonusCard(random);

            game.currentPlayerState().addBonusCard(bonusCard);

            game.place(Place::isGovernor).takeGovernor();
            game.randomPlace(random).placeGovernor();

            return ActionResult.followUp(PossibleAction.choice(Set.of(
                    PossibleAction.optional(Action.Pay2Lira.class),
                    PossibleAction.optional(Action.DiscardBonusCard.class))));
        }

    }

    public static class Smuggler extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            game.place(Place::isSmuggler).takeSmuggler();
            game.randomPlace(random).placeSmuggler();

            return ActionResult.followUp(PossibleAction.whenThen(takeAnyGood(),
                    PossibleAction.choice(Set.of(
                            PossibleAction.optional(Action.Pay2Lira.class),
                            PossibleAction.optional(Action.Pay1Fabric.class),
                            PossibleAction.optional(Action.Pay1Fruit.class),
                            PossibleAction.optional(Action.Pay1Spice.class),
                            PossibleAction.optional(Action.Pay1Blue.class))), 0, 1));
        }

        private static PossibleAction takeAnyGood() {
            return PossibleAction.choice(Set.of(
                    PossibleAction.optional(Take1Fabric.class),
                    PossibleAction.optional(Take1Spice.class),
                    PossibleAction.optional(Take1Fruit.class),
                    PossibleAction.optional(Take1Blue.class)));
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

    public static class Pay2LiraFor1AdditionalGood extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var currentPlayerState = game.currentPlayerState();

            currentPlayerState.payLira(2);

            return ActionResult.followUp(PossibleAction.choice(Set.of(
                    PossibleAction.optional(Action.Take1Blue.class),
                    PossibleAction.optional(Action.Take1Fruit.class),
                    PossibleAction.optional(Action.Take1Spice.class),
                    PossibleAction.optional(Action.Take1Fabric.class))));
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Pay2LiraToReturnAssistant extends Action {
        int x, y;

        @Override
        ActionResult perform(Game game, Random random) {
            var place = game.place(x, y);

            game.currentPlayerState().payLira(2);
            place.returnAssistant(game.getCurrentMerchant());

            return ActionResult.none();
        }
    }

    public static class UsePostOffice extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var postOffice = game.getPostOffice();

            var result = postOffice.use(game);

            return game.currentPlayerState().hasBonusCard(BonusCard.POST_OFFICE_2X)
                    ? result.andThen(ActionResult.followUp(PossibleAction.optional(BonusCardUsePostOffice.class)))
                    : result;
        }
    }

    public static class BonusCardUsePostOffice extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var actionResult = new UsePostOffice().perform(game, random);
            game.currentPlayerState().removeBonusCard(BonusCard.POST_OFFICE_2X);
            return actionResult;
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
            game.currentPlayerState().removeBonusCard(bonusCard);
            return ActionResult.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Take2BonusCards extends Action {
        boolean caravansary;

        @Override
        ActionResult perform(Game game, Random random) {
            var currentPlayerState = game.currentPlayerState();

            if (caravansary) {
                var caravansary = expectCurrentPlace(game, game.getCaravansary());
                currentPlayerState.addBonusCard(caravansary.drawBonusCard());
                currentPlayerState.addBonusCard(caravansary.drawBonusCard());
            } else {
                currentPlayerState.addBonusCard(game.drawBonusCard(random));
                currentPlayerState.addBonusCard(game.drawBonusCard(random));
            }

            return ActionResult.none();
        }
    }

    public static class ReturnAllAssistants extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var merchant = game.getCurrentMerchant();

            for (Place place : game.getLayout().getPlaces()) {
                place.returnAssistants(merchant);
            }

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

            teaHouse.guessAndRoll(game, guess, random);

            return ActionResult.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class SellGoods extends Action {
        @NonNull
        Map<GoodsType, Integer> goods;

        BonusCard bonusCard;

        @Override
        ActionResult perform(Game game, Random random) {
            var smallMarket = game.getSmallMarket();

            var market = expectCurrentPlace(game, smallMarket, game.getLargeMarket());

            if (bonusCard == BonusCard.SMALL_MARKET_ANY_GOOD) {
                if (market != smallMarket) {
                    throw new IstanbulException(IstanbulError.NOT_AT_PLACE);
                }

                game.currentPlayerState().removeBonusCard(bonusCard);

                smallMarket.sellAnyGoods(game, goods);
            } else {
                market.sellDemandGoods(game, goods);
            }

            return ActionResult.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class SendFamilyMember extends Action {
        @NonNull
        Place to;

        @Override
        ActionResult perform(Game game, Random random) {
            var policeStation = game.getPoliceStation();

            if (to == policeStation) {
                throw new IstanbulException(IstanbulError.ALREADY_AT_PLACE);
            }

            policeStation.takeFamilyMember(game.getCurrentPlayer());

            return to.placeFamilyMember(game, game.getCurrentPlayer());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class DeliverToSultan extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var sultansPalace = game.getSultansPalace();

            var actionResult = sultansPalace.deliverToSultan(game.currentPlayerState());

            return game.currentPlayerState().hasBonusCard(BonusCard.SULTAN_2X)
                    ? actionResult.andThen(ActionResult.followUp(PossibleAction.optional(BonusCardDeliverToSultan.class)))
                    : ActionResult.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class BonusCardDeliverToSultan extends Action {

        @Override
        ActionResult perform(Game game, Random random) {
            var actionResult = new DeliverToSultan().perform(game, random);
            game.currentPlayerState().removeBonusCard(BonusCard.SULTAN_2X);
            return actionResult;
        }
    }

    public static class BuyRuby extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var gemstoneDealer = game.getGemstoneDealer();

            gemstoneDealer.buy(game.currentPlayerState());

            return game.currentPlayerState().hasBonusCard(BonusCard.GEMSTONE_DEALER_2X)
                    ? ActionResult.followUp(PossibleAction.optional(PossibleAction.optional(BonusCardBuyRuby.class)))
                    : ActionResult.none();
        }
    }

    public static class BonusCardBuyRuby extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var actionResult = new BuyRuby().perform(game, random);
            game.currentPlayerState().removeBonusCard(BonusCard.GEMSTONE_DEALER_2X);
            return actionResult;
        }
    }

    public static class BonusCardTake5Lira extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var currentPlayerState = game.currentPlayerState();

            currentPlayerState.removeBonusCard(BonusCard.TAKE_5_LIRA);
            currentPlayerState.gainLira(5);

            return ActionResult.none();
        }
    }

    public static class BonusCardGain1Good extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var currentPlayerState = game.currentPlayerState();

            currentPlayerState.removeBonusCard(BonusCard.GAIN_1_GOOD);

            return ActionResult.followUp(PossibleAction.choice(Set.of(
                    PossibleAction.optional(Action.Take1Fabric.class),
                    PossibleAction.optional(Action.Take1Spice.class),
                    PossibleAction.optional(Action.Take1Fruit.class),
                    PossibleAction.optional(Action.Take1Blue.class))));
        }
    }

    public static class PlaceFamilyMemberOnPoliceStation extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            var policeStation = game.getPoliceStation();

            if (policeStation.getFamilyMembers().contains(game.getCurrentPlayer())) {
                throw new IstanbulException(IstanbulError.ALREADY_AT_PLACE);
            }

            var from = game.getFamilyMemberCurrentPlace(game.getCurrentPlayer());

            from.takeFamilyMember(game.getCurrentPlayer());
            policeStation.placeFamilyMember(game, game.getCurrentPlayer());

            return ActionResult.followUp(PossibleAction.choice(Set.of(
                    PossibleAction.optional(Action.TakeBonusCard.class),
                    PossibleAction.optional(Action.Take3Lira.class))));
        }
    }

    public static class TakeBonusCard extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().addBonusCard(game.drawBonusCard(random));
            return ActionResult.none();
        }
    }

    public static class Take3Lira extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().gainLira(3);
            return ActionResult.none();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Return1Assistant extends Action {
        Place from;

        @Override
        ActionResult perform(Game game, Random random) {
            from.returnAssistant(game.getCurrentMerchant());
            return ActionResult.none();
        }
    }

    public static class Pay1Fabric extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().removeGoods(GoodsType.FABRIC, 1);
            return ActionResult.none();
        }
    }

    public static class Pay1Fruit extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().removeGoods(GoodsType.FRUIT, 1);
            return ActionResult.none();
        }
    }

    public static class Pay1Spice extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().removeGoods(GoodsType.SPICE, 1);
            return ActionResult.none();
        }
    }

    public static class Pay1Blue extends Action {
        @Override
        ActionResult perform(Game game, Random random) {
            game.currentPlayerState().removeGoods(GoodsType.BLUE, 1);
            return ActionResult.none();
        }
    }
}
