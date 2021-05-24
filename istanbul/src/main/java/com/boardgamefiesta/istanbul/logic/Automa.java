package com.boardgamefiesta.istanbul.logic;

import com.boardgamefiesta.api.domain.Player;

import java.util.Collections;
import java.util.Random;

public class Automa {

    public void execute(Istanbul state, Player player, Random random) {
        var possibleActions = state.getPossibleActions();

        if (possibleActions.contains(Action.Move.class)) {
            state.perform(player, bestPossibleMove(state, random), random);
        } else {
            // For now just end turn
            state.endTurn(player, random);
        }
    }

    private Action.Move bestPossibleMove(Istanbul state, Random random) {
        var possiblePlaces = state.possiblePlaces();
        // TODO Make smarter. Just random for now
        Collections.shuffle(possiblePlaces, random);
        return new Action.Move(possiblePlaces.get(0));
    }
}
