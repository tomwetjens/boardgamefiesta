package com.wetjens.gwt;

import java.util.Collections;
import java.util.Set;

public abstract class DiscardCards extends Action {

    private final int expected;
    private final Set<Card> cards;

    public DiscardCards(int expected, Set<Card> cards) {
        this.expected = expected;
        this.cards = cards;
    }

    @Override
    public ImmediateActions perform(Game game) {
        if (cards.size() != expected) {
            throw new IllegalStateException("Must discard " + expected + " cards");
        }

        cards.forEach(game.currentPlayerState()::discardCard);

        return ImmediateActions.none();
    }

    public static class Discard1Card extends DiscardCards {
        public Discard1Card(Card card) {
            super(1, Collections.singleton(card));
        }
    }

    public static class Discard2Cards extends DiscardCards {
        public Discard2Cards(Set<Card> cards) {
            super(2, cards);
        }
    }

    public static class Discard3Cards extends DiscardCards {
        public Discard3Cards(Set<Card> cards) {
            super(3, cards);
        }
    }
}
