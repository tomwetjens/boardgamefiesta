package com.wetjens.gwt;

public abstract class DrawCardThenDiscardCard extends Action {

    public static final class Draw1CardThenDiscard1Card extends DrawCardThenDiscardCard {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().drawCard();
            return ImmediateActions.of(PossibleAction.of(DiscardCards.Discard1Card.class));
        }
    }

    public static final class Draw2CardsThenDiscard2Cards extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().drawCard();
            game.currentPlayerState().drawCard();
            return ImmediateActions.of(PossibleAction.of(DiscardCards.Discard2Cards.class));
        }
    }

    public static class Draw3CardsThenDiscard3Cards extends Action {
        @Override
        public ImmediateActions perform(Game game) {
            game.currentPlayerState().drawCard();
            game.currentPlayerState().drawCard();
            game.currentPlayerState().drawCard();
            return ImmediateActions.of(PossibleAction.of(DiscardCards.Discard3Cards.class));
        }
    }

}
