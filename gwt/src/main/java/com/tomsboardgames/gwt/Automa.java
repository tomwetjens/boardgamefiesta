package com.boardgamefiesta.gwt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Automa {

    public void execute(Game game, Random random) {
        // TODO Currently a super stupid implementation
        var possibleActions = game.possibleActions();

        if (possibleActions.contains(Action.Move.class)) {
            // TODO For now just go to next location
            var steps = game.getTrail().getCurrentLocation(game.getCurrentPlayer())
                    .map(from -> {
                        var to = from.reachableLocations(1, 1).stream()
                                .findAny()
                                .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS));

                        return game.possibleMoves(game.getCurrentPlayer(), to).stream()
                                .findAny()
                                .map(PossibleMove::getSteps)
                                .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS));
                    })
                    .orElseGet(() -> Collections.singletonList(game.getTrail().getLocation("A")));

            var action = new Action.Move(steps);

            game.perform(action, random);
        } else if (possibleActions.contains(Action.ChooseForesights.class)) {
            // TODO Just pick any tile now
            game.perform(new Action.ChooseForesights(List.of(0, 0, 0)), random);
        } else if (possibleActions.contains(Action.DeliverToCity.class)) {
            // TODO Always KC for now (stupid)
            game.perform(new Action.DeliverToCity(City.KANSAS_CITY, 0), random);
        } else if (possibleActions.contains(Action.UnlockWhite.class)) {
            // TODO Just pick any now
            var unlock = Arrays.stream(Unlockable.values())
                    .filter(unlockable -> unlockable.getDiscColor() == DiscColor.WHITE)
                    .filter(unlockable -> game.currentPlayerState().canUnlock(unlockable))
                    .findAny()
                    .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS));
            game.perform(new Action.UnlockWhite(unlock), random);
        } else if (possibleActions.contains(Action.UnlockBlackOrWhite.class)) {
            // TODO Just pick any now
            var unlock = Arrays.stream(Unlockable.values())
                    .filter(unlockable -> game.currentPlayerState().canUnlock(unlockable))
                    .findAny()
                    .orElseThrow(() -> new GWTException(GWTError.NO_ACTIONS));
            game.perform(new Action.UnlockWhite(unlock), random);
        } else if (possibleActions.contains(Action.TakeObjectiveCard.class)) {
            // TODO Just pick any now
            var objectiveCard = game.getObjectiveCards().getAvailable().iterator().next();
            game.perform(new Action.TakeObjectiveCard(objectiveCard), random);
        } else {
            // TODO For now just stupidly end turn
            game.endTurn(random);
        }
    }


}
