package com.wetjens.gwt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Move extends Action {

    private final List<Location> steps;

    public Move(List<Location> steps) {
        this.steps = new ArrayList<>(steps);
    }

    @Override
    public ImmediateActions perform(Game game) {
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Must take at least one step");
        }

        PlayerState currentPlayerState = game.currentPlayerState();
        if (steps.size() > currentPlayerState.getStepLimit()) {
            throw new IllegalArgumentException("Number of steps exceeds player step limit");
        }

        if (game.getTrail().isAtLocation(game.getCurrentPlayer())) {
            Location from = game.getTrail().getCurrentLocation(game.getCurrentPlayer());

            checkDirectAndConsecutiveSteps(from, steps);

            payFees(game);
        }

        Location to = steps.get(steps.size() - 1);

        game.getTrail().movePlayer(game.getCurrentPlayer(), to);

        return to.getPossibleAction()
                .map(ImmediateActions::of)
                .orElse(ImmediateActions.none());
    }

    private void checkDirectAndConsecutiveSteps(Location from, List<Location> steps) {
        Location to = steps.get(0);
        if (!from.isDirect(to)) {
            throw new IllegalArgumentException("Cannot step from " + from + " to " + to);
        }
        if (steps.size() > 1) {
            checkDirectAndConsecutiveSteps(to, steps.subList(1, steps.size()));
        }
    }

    private void payFees(Game game) {
        for (Location location : steps) {
            payFee(game, location);
        }
    }

    private void payFee(Game game, Location location) {
        PlayerState currentPlayerState = game.currentPlayerState();

        // Can never pay more than player has
        int amount = Math.min(currentPlayerState.getBalance(),
                location.getFee().getAmount(game.getPlayers().size()));

        currentPlayerState.payDollars(amount);

        feeRecipient(location)
                .map(game::playerState)
                .ifPresent(recipient -> recipient.gainDollars(amount));
    }

    private Optional<Player> feeRecipient(Location location) {
        Optional<Player> recipient = Optional.empty();
        if (location instanceof Location.BuildingLocation) {
            recipient = ((Location.BuildingLocation) location).getBuilding()
                    .filter(building -> building instanceof PlayerBuilding)
                    .map(building -> ((PlayerBuilding) building).getPlayer());
        }
        return recipient;
    }
}
