package com.boardgamefiesta.gwt.view;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.gwt.logic.*;
import lombok.Getter;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class PlayerStateView {

    PlayerView player;

    int bid;
    int balance;
    int cowboys;
    int craftsmen;
    int engineers;
    int tempCertificates;
    int certificates;

    ScoreView score;

    List<CardView> hand;
    Integer handSize;
    Integer handValue;

    List<CardView> discardPile;
    int discardPileSize;
    CardView discardPileTop;

    int drawStackSize;
    List<CardView> drawStack;

    Map<Unlockable, Integer> unlocked;
    List<String> buildings;
    List<StationMaster> stationMasters;
    List<HazardView> hazards;
    List<Teepee> teepees;
    List<ObjectiveView> objectives;

    int exchangeTokens;
    int branchlets;

    AutomaStateView automaState;

    Map<String, Object> stats;

    PlayerStateView(@NonNull GWT state, @NonNull PlayerState playerState, Player viewingPlayer) {
        player = new PlayerView(playerState.getPlayer());

        balance = playerState.getBalance();
        cowboys = playerState.getNumberOfCowboys();
        craftsmen = playerState.getNumberOfCraftsmen();
        engineers = playerState.getNumberOfEngineers();
        tempCertificates = playerState.getTempCertificates();
        certificates = playerState.getTempCertificates() + playerState.permanentCertificates();

        handSize = playerState.getHand().size();
        discardPileSize = playerState.getDiscardPile().size();
        discardPileTop = !playerState.getDiscardPile().isEmpty()
                ? CardView.of(playerState.getDiscardPile().get(playerState.getDiscardPile().size() - 1))
                : null;

        if (viewingPlayer == playerState.getPlayer() || state.isEnded()) {
            hand = playerState.getHand().stream()
                    .map(CardView::of)
                    .sorted()
                    .collect(Collectors.toList());
            handValue = playerState.handValue();
        } else {
            hand = null;
            handValue = null;
        }

        if (state.isEnded() || viewingPlayer == playerState.getPlayer()
                || state.getMode() == GWT.Options.Mode.STRATEGIC) {
            // Player is allowed to go through discard pile
            discardPile = playerState.getDiscardPile().stream()
                    .map(CardView::of)
                    .collect(Collectors.toList());
            Collections.reverse(discardPile);
        }

        drawStackSize = playerState.getDrawStack().size();
        if (state.isEnded() || state.getMode() == GWT.Options.Mode.STRATEGIC) {
            if (viewingPlayer == playerState.getPlayer()) {
                // Hand already visible to player, show only cards in actual draw stack
                drawStack = playerState.getDrawStack().stream()
                        .map(CardView::of)
                        .collect(Collectors.toList());

            } else {
                // Hand not visible, show draw stack + hand as "draw stack" to prevent deducing what is in hand
                drawStack = Stream.concat(playerState.getDrawStack().stream(), playerState.getHand().stream())
                        .map(CardView::of)
                        .collect(Collectors.toList());
            }

            // Sort so player cannot know which cards are coming
            Collections.sort(drawStack);
        }

        unlocked = playerState.getUnlocked();

        buildings = playerState.getBuildings().stream()
                .sorted(Comparator.comparingInt(PlayerBuilding::getCraftsmen)
                        .thenComparingInt(PlayerBuilding::getNumber))
                .map(Building::getName)
                .collect(Collectors.toList());

        stationMasters = new ArrayList<>(playerState.getStationMasters());
        Collections.sort(stationMasters);

        hazards = playerState.getHazards().stream()
                .map(HazardView::new)
                .sorted()
                .collect(Collectors.toList());

        teepees = new ArrayList<>(playerState.getTeepees());
        Collections.sort(teepees);

        var objectivesScores = playerState.scoreObjectives(state);
        objectives = (state.isEnded() || state.getMode() == GWT.Options.Mode.STRATEGIC
                ? Stream.concat(playerState.getCommittedObjectives().stream(), playerState.getOptionalObjectives())
                : playerState.getCommittedObjectives().stream())
                .map(objectiveCard -> new ObjectiveView(new ObjectiveCardView(objectiveCard), objectivesScores.getScore(objectiveCard),
                        state.isEnded() ? objectivesScores.getCommitted().contains(objectiveCard)
                                : playerState.getCommittedObjectives().contains(objectiveCard)))
                .sorted()
                .collect(Collectors.toList());

        score = state.scoreDetails(playerState.getPlayer()).map(ScoreView::new).orElse(null);

        exchangeTokens = playerState.getExchangeTokens();

        branchlets = playerState.getBranchlets();

        automaState = playerState.getAutomaState().map(AutomaStateView::new).orElse(null);

        if (state.isEnded()) {
            stats = state.stats(playerState.getPlayer()).asMap();
        }
    }

}
