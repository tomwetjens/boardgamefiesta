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

public abstract class Action implements com.boardgamefiesta.api.domain.Action {

    abstract ActionResult perform(Istanbul game, Random random);

    @SuppressWarnings("unchecked")
    protected <T extends Place> T expectCurrentPlace(Istanbul game, T... anyOfPlaces) {
        var currentPlace = game.getCurrentPlace();

        if (!Arrays.<Place>asList(anyOfPlaces).contains(currentPlace)) {
            var familyMemberCurrentPlace = game.getFamilyMemberCurrentPlace(game.getCurrentPlayer());

            if (!Arrays.<Place>asList(anyOfPlaces).contains(familyMemberCurrentPlace)) {
                throw new IstanbulException(IstanbulError.NOT_AT_PLACE);
            }

            return (T) familyMemberCurrentPlace;
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
        ActionResult perform(Istanbul game, Random random) {
            if (bonusCard == BonusCard.MOVE_0) {
                game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PLAY_BONUS_CARD, bonusCard.name()));
                game.currentPlayerState().removeBonusCard(BonusCard.MOVE_0);

                game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.MOVE, Integer.toString(to.getNumber())));
                return game.moveMerchant(game.getCurrentMerchant(), to, 0, 0);
            } else if (bonusCard == BonusCard.MOVE_3_OR_4) {
                game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PLAY_BONUS_CARD, bonusCard.name()));
                game.currentPlayerState().removeBonusCard(BonusCard.MOVE_3_OR_4);

                game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.MOVE, Integer.toString(to.getNumber())));
                return game.moveMerchant(game.getCurrentMerchant(), to, 3, 4);
            } else {
                game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.MOVE, Integer.toString(to.getNumber())));
                return game.moveMerchant(game.getCurrentMerchant(), to, 1, 2);
            }
        }
    }

    public static class LeaveAssistant extends Action {

        @Override
        ActionResult perform(Istanbul game, Random random) {
            var place = game.getCurrentPlace();

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.LEAVE_ASSISTANT));

            return place.leaveAssistant(place.getMerchant(game.getCurrentPlayer().getColor()), game);
        }
    }

    public static class PayOtherMerchants extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            var currentPlayerState = game.currentPlayerState();
            var place = game.getCurrentPlace();

            var otherMerchants = place.getMerchants().stream()
                    .filter(merchant -> merchant.getColor() != game.getCurrentPlayer().getColor())
                    .collect(Collectors.toList());

            if (otherMerchants.size() * 2 > currentPlayerState.getLira()) {
                throw new IstanbulException(IstanbulError.NOT_ENOUGH_LIRA);
            }

            otherMerchants.forEach(otherMerchant -> {
                var amount = 2;
                currentPlayerState.payLira(amount);

                otherMerchant.getPlayer().ifPresentOrElse(otherPlayer -> {
                            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PAY_OTHER_PLAYER, Integer.toString(amount), otherPlayer.getName()));
                            game.getPlayerState(otherPlayer).gainLira(2);
                        },
                        // In 2P variant, if neutral, pay to bank then randomly place somewhere else
                        () -> {
                            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PAY_LIRA, Integer.toString(amount)));

                            var to = game.randomPlace(random);
                            game.moveMerchant(otherMerchant, to, 0, 4);

                            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.MOVE_DUMMY, otherMerchant.getColor().name(), Integer.toString(to.getNumber())));
                        });
            });

            return place.placeActions(game);
        }

    }

    public static class Governor extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            var bonusCard = game.drawBonusCard(random);

            game.currentPlayerState().addBonusCard(bonusCard);

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.USE_GOVERNOR, bonusCard.name()));

            game.place(Place::isGovernor).takeGovernor();
            var to = game.randomPlace(random);
            to.placeGovernor();

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.MOVE_GOVERNOR, Integer.toString(to.getNumber())));

            return ActionResult.followUp(PossibleAction.choice(Set.of(
                    PossibleAction.optional(Action.Pay2Lira.class),
                    PossibleAction.optional(Action.DiscardBonusCard.class))),  false);
        }

    }

    public static class Smuggler extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.USE_SMUGGLER));

            game.place(Place::isSmuggler).takeSmuggler();
            var to = game.randomPlace(random);
            to.placeSmuggler();

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.MOVE_SMUGGLER, Integer.toString(to.getNumber())));

            return ActionResult.followUp(PossibleAction.whenThen(takeAnyGood(),
                    PossibleAction.choice(Set.of(
                            PossibleAction.optional(Action.Pay2Lira.class),
                            PossibleAction.optional(Action.Pay1Fabric.class),
                            PossibleAction.optional(Action.Pay1Fruit.class),
                            PossibleAction.optional(Action.Pay1Spice.class),
                            PossibleAction.optional(Action.Pay1Blue.class))), 0, 1), true);
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
        ActionResult perform(Istanbul game, Random random) {
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.TAKE_MOSQUE_TILE, mosqueTile.name()));
            return expectCurrentPlace(game, game.getGreatMosque(), game.getSmallMosque()).takeMosqueTile(mosqueTile, game);
        }
    }

    public static class BuyWheelbarrowExtension extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            var currentPlayerState = game.currentPlayerState();

            var amount = 7;
            currentPlayerState.payLira(amount);
            currentPlayerState.addExtension();

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.BUY_WHEELBARROW_EXTENSION, Integer.toString(amount), Integer.toString(currentPlayerState.getCapacity())));

            return ActionResult.none(true);
        }
    }

    private static class MaxGoods extends Action {

        private final GoodsType goodsType;

        MaxGoods(GoodsType goodsType) {
            this.goodsType = goodsType;
        }

        @Override
        ActionResult perform(Istanbul game, Random random) {
            var currentPlayerState = game.currentPlayerState();

            currentPlayerState.maxGoods(goodsType);
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.MAX_GOODS, Integer.toString(currentPlayerState.getGoods().get(goodsType))));

            if (currentPlayerState.hasMosqueTile(MosqueTile.PAY_2_LIRA_FOR_1_ADDITIONAL_GOOD)) {
                game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.MAY_PAY_2_LIRA_FOR_1_ADDITIONAL_GOOD));
                return ActionResult.followUp(PossibleAction.optional(Action.Pay2LiraFor1AdditionalGood.class), true);
            }
            return ActionResult.none(true);
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
        ActionResult perform(Istanbul game, Random random) {
            var currentPlayerState = game.currentPlayerState();

            currentPlayerState.payLira(2);

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PAY_2_LIRA_FOR_1_ADDITIONAL_GOOD));

            return ActionResult.followUp(PossibleAction.choice(Set.of(
                    PossibleAction.optional(Action.Take1Blue.class),
                    PossibleAction.optional(Action.Take1Fruit.class),
                    PossibleAction.optional(Action.Take1Spice.class),
                    PossibleAction.optional(Action.Take1Fabric.class))), true);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Pay2LiraToReturnAssistant extends Action {
        int x, y;

        @Override
        ActionResult perform(Istanbul game, Random random) {
            var place = game.place(x, y);

            game.currentPlayerState().payLira(2);
            place.returnAssistant(game.getCurrentMerchant());

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PAY_2_LIRA_TO_RETURN_ASSISTANT));

            return ActionResult.none(true);
        }
    }

    public static class UsePostOffice extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            var postOffice = game.getPostOffice();

            var result = postOffice.use(game);

            if (game.currentPlayerState().hasBonusCard(BonusCard.POST_OFFICE_2X)) {
                game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.MAY_USE_POSTOFFICE_2X));
                return result.andThen(ActionResult.followUp(PossibleAction.optional(BonusCardUsePostOffice.class), true));
            }
            return result;
        }
    }

    public static class BonusCardUsePostOffice extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PLAY_BONUS_CARD, BonusCard.POST_OFFICE_2X.name()));

            var actionResult = new UsePostOffice().perform(game, random);
            game.currentPlayerState().removeBonusCard(BonusCard.POST_OFFICE_2X);
            return actionResult;
        }
    }

    public static class CatchFamilyMemberForBonusCard extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            var place = game.getCurrentPlace();

            var familyMember = place.catchFamilyMember(game);

            var bonusCard = game.drawBonusCard(random);
            game.currentPlayerState().addBonusCard(bonusCard);

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.CATCH_FAMILY_MEMBER, familyMember.getName()));
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.TAKE_BONUS_CARD, bonusCard.name()));

            return ActionResult.none(false);
        }
    }

    public static class CatchFamilyMemberFor3Lira extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            var place = game.getCurrentPlace();

            var familyMember = place.catchFamilyMember(game);

            var amount = 3;
            game.currentPlayerState().gainLira(amount);

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.CATCH_FAMILY_MEMBER, familyMember.getName()));
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.GAIN_LIRA, Integer.toString(amount)));

            return ActionResult.none(true);
        }
    }

    public static class Pay2Lira extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            var amount = 2;
            game.currentPlayerState().payLira(amount);
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PAY_LIRA, Integer.toString(amount)));
            return ActionResult.none(true);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class DiscardBonusCard extends Action {
        BonusCard bonusCard;

        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.currentPlayerState().removeBonusCard(bonusCard);
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.DISCARD_BONUS_CARD, bonusCard.name()));
            return ActionResult.none(true);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Take2BonusCards extends Action {
        boolean caravansary;

        @Override
        ActionResult perform(Istanbul game, Random random) {
            var currentPlayerState = game.currentPlayerState();

            BonusCard a;
            BonusCard b;
            boolean canUndo;

            if (caravansary) {
                var caravansary = expectCurrentPlace(game, game.getCaravansary());
                a = caravansary.drawBonusCard();
                b = caravansary.drawBonusCard();
                canUndo = true;
            } else {
                a = game.drawBonusCard(random);
                b = game.drawBonusCard(random);
                canUndo = false;
            }

            currentPlayerState.addBonusCard(a);
            currentPlayerState.addBonusCard(b);

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.TAKE_BONUS_CARD, a.name()));
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.TAKE_BONUS_CARD, b.name()));

            return ActionResult.none(canUndo);
        }
    }

    public static class ReturnAllAssistants extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            var merchant = game.getCurrentMerchant();

            for (Place place : game.getLayout().getPlaces()) {
                place.returnAssistants(merchant);
            }

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.RETURN_ALL_ASSISTANTS));

            return ActionResult.none(true);
        }
    }

    public static class Take1Fabric extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.currentPlayerState().addGoods(GoodsType.FABRIC, 1);

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.TAKE_GOODS, "1", GoodsType.FABRIC.name()));

            return ActionResult.none(true);
        }
    }

    public static class Take1Spice extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.currentPlayerState().addGoods(GoodsType.SPICE, 1);

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.TAKE_GOODS, "1", GoodsType.SPICE.name()));

            return ActionResult.none(true);
        }
    }

    public static class Take1Blue extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.currentPlayerState().addGoods(GoodsType.BLUE, 1);

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.TAKE_GOODS, "1", GoodsType.BLUE.name()));

            return ActionResult.none(true);
        }
    }

    public static class Take1Fruit extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.currentPlayerState().addGoods(GoodsType.FRUIT, 1);

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.TAKE_GOODS, "1", GoodsType.FRUIT.name()));

            return ActionResult.none(true);
        }
    }

    public static class RollForBlueGoods extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            Place.BlackMarket.rollForBlueGoods(game, game.currentPlayerState(), random);

            return ActionResult.none(false);
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
        ActionResult perform(Istanbul game, Random random) {
            var teaHouse = game.getTeaHouse();

            teaHouse.guessAndRoll(game, guess, random);

            return ActionResult.none(false);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class SellGoods extends Action {
        @NonNull
        Map<GoodsType, Integer> goods;

        BonusCard bonusCard;

        @Override
        ActionResult perform(Istanbul game, Random random) {
            var smallMarket = game.getSmallMarket();

            var market = expectCurrentPlace(game, smallMarket, game.getLargeMarket());

            if (bonusCard == BonusCard.SMALL_MARKET_ANY_GOOD) {
                if (market != smallMarket) {
                    throw new IstanbulException(IstanbulError.NOT_AT_PLACE);
                }

                game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PLAY_BONUS_CARD, bonusCard.name()));

                game.currentPlayerState().removeBonusCard(bonusCard);

                smallMarket.sellAnyGoods(game, goods);
            } else {
                market.sellDemandGoods(game, goods);
            }

            return ActionResult.none(true);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class SendFamilyMember extends Action {
        @NonNull
        Place to;

        @Override
        ActionResult perform(Istanbul game, Random random) {
            var policeStation = game.getPoliceStation();

            if (to == policeStation) {
                throw new IstanbulException(IstanbulError.ALREADY_AT_PLACE);
            }

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.SEND_FAMILY_MEMBER, Integer.toString(to.getNumber())));

            policeStation.takeFamilyMember(game.getCurrentPlayer());

            return to.placeFamilyMember(game, game.getCurrentPlayer());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class DeliverToSultan extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            var sultansPalace = game.getSultansPalace();

            var actionResult = sultansPalace.deliverToSultan(game.currentPlayerState());

            if (game.currentPlayerState().hasBonusCard(BonusCard.SULTAN_2X)) {
                game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.MAY_DELIVER_TO_SULTAN_2X));
                return actionResult.andThen(ActionResult.followUp(PossibleAction.optional(BonusCardDeliverToSultan.class), true));
            }
            return ActionResult.none(true);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class BonusCardDeliverToSultan extends Action {

        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PLAY_BONUS_CARD, BonusCard.SULTAN_2X.name()));

            var actionResult = new DeliverToSultan().perform(game, random);
            game.currentPlayerState().removeBonusCard(BonusCard.SULTAN_2X);
            return actionResult;
        }
    }

    public static class BuyRuby extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            var gemstoneDealer = game.getGemstoneDealer();

            gemstoneDealer.buy(game);

            if (game.currentPlayerState().hasBonusCard(BonusCard.GEMSTONE_DEALER_2X)) {
                game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.MAY_USE_GEMSTONE_DEALER_2X));
                return ActionResult.followUp(PossibleAction.optional(PossibleAction.optional(BonusCardBuyRuby.class)), true);
            }
            return ActionResult.none(true);
        }
    }

    public static class BonusCardBuyRuby extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PLAY_BONUS_CARD, BonusCard.GEMSTONE_DEALER_2X.name()));

            var actionResult = new BuyRuby().perform(game, random);
            game.currentPlayerState().removeBonusCard(BonusCard.GEMSTONE_DEALER_2X);
            return actionResult;
        }
    }

    public static class BonusCardTake5Lira extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            var currentPlayerState = game.currentPlayerState();

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PLAY_BONUS_CARD, BonusCard.TAKE_5_LIRA.name()));

            currentPlayerState.removeBonusCard(BonusCard.TAKE_5_LIRA);
            currentPlayerState.gainLira(5);

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.GAIN_LIRA, "5"));

            return ActionResult.none(true);
        }
    }

    public static class BonusCardGain1Good extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PLAY_BONUS_CARD, BonusCard.GAIN_1_GOOD.name()));

            var currentPlayerState = game.currentPlayerState();

            currentPlayerState.removeBonusCard(BonusCard.GAIN_1_GOOD);

            return ActionResult.followUp(PossibleAction.choice(Set.of(
                    PossibleAction.optional(Action.Take1Fabric.class),
                    PossibleAction.optional(Action.Take1Spice.class),
                    PossibleAction.optional(Action.Take1Fruit.class),
                    PossibleAction.optional(Action.Take1Blue.class))), true);
        }
    }

    public static class PlaceFamilyMemberOnPoliceStation extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            var policeStation = game.getPoliceStation();

            if (policeStation.getFamilyMembers().contains(game.getCurrentPlayer())) {
                throw new IstanbulException(IstanbulError.ALREADY_AT_PLACE);
            }

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PLAY_BONUS_CARD, BonusCard.FAMILY_MEMBER_TO_POLICE_STATION.name()));

            game.currentPlayerState().removeBonusCard(BonusCard.FAMILY_MEMBER_TO_POLICE_STATION);

            var from = game.getFamilyMemberCurrentPlace(game.getCurrentPlayer());

            from.takeFamilyMember(game.getCurrentPlayer());
            policeStation.placeFamilyMember(game, game.getCurrentPlayer());

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.FAMILY_MEMBER_TO_POLICE_STATION));

            return ActionResult.followUp(PossibleAction.choice(Set.of(
                    PossibleAction.optional(Action.TakeBonusCard.class),
                    PossibleAction.optional(Action.Take3Lira.class))), true);
        }
    }

    public static class TakeBonusCard extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            var bonusCard = game.drawBonusCard(random);
            game.currentPlayerState().addBonusCard(bonusCard);

            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.TAKE_BONUS_CARD, bonusCard.name()));

            return ActionResult.none(false);
        }
    }

    public static class Take3Lira extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.GAIN_LIRA, "3"));
            game.currentPlayerState().gainLira(3);
            return ActionResult.none(true);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Return1Assistant extends Action {
        Place from;

        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PLAY_BONUS_CARD, BonusCard.RETURN_1_ASSISTANT.name()));
            from.returnAssistant(game.getCurrentMerchant());
            return ActionResult.none(true);
        }
    }

    public static class Pay1Fabric extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PAY_GOODS, "1", GoodsType.FABRIC.name()));
            game.currentPlayerState().removeGoods(GoodsType.FABRIC, 1);
            return ActionResult.none(true);
        }
    }

    public static class Pay1Fruit extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PAY_GOODS, "1", GoodsType.FRUIT.name()));
            game.currentPlayerState().removeGoods(GoodsType.FRUIT, 1);
            return ActionResult.none(true);
        }
    }

    public static class Pay1Spice extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PAY_GOODS, "1", GoodsType.SPICE.name()));
            game.currentPlayerState().removeGoods(GoodsType.SPICE, 1);
            return ActionResult.none(true);
        }
    }

    public static class Pay1Blue extends Action {
        @Override
        ActionResult perform(Istanbul game, Random random) {
            game.fireEvent(IstanbulEvent.create(game.getCurrentPlayer(), IstanbulEvent.Type.PAY_GOODS, "1", GoodsType.BLUE.name()));
            game.currentPlayerState().removeGoods(GoodsType.BLUE, 1);
            return ActionResult.none(true);
        }
    }
}
