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
    Boolean winner;

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
    Set<StationMaster> stationMasters;
    List<HazardView> hazards;
    List<Teepee> teepees;
    List<ObjectiveView> objectives;

    PlayerStateView(@NonNull Game state, @NonNull PlayerState playerState, Player viewingPlayer) {
        player = new PlayerView(playerState.getPlayer());

        balance = playerState.getBalance();
        cowboys = playerState.getNumberOfCowboys();
        craftsmen = playerState.getNumberOfCraftsmen();
        engineers = playerState.getNumberOfEngineers();
        tempCertificates = playerState.getTempCertificates();
        certificates = playerState.getTempCertificates() + playerState.permanentCertificates();

        drawStackSize = playerState.getDrawStack().size();
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

            // Player is allowed to go through discard pile
            discardPile = playerState.getDiscardPile().stream()
                    .map(CardView::of)
                    .collect(Collectors.toList());
            Collections.reverse(discardPile);
        } else {
            hand = null;
            handValue = null;
        }

        if (state.isEnded() || (viewingPlayer == playerState.getPlayer() && state.getMode() == Game.Options.Mode.STRATEGIC)) {
            drawStack = playerState.getDrawStack().stream()
                    .map(CardView::of)
                    .collect(Collectors.toList());
            // Randomize so player cannot know which cards are coming
            Collections.shuffle(drawStack);
        }

        unlocked = playerState.getUnlocked();

        buildings = playerState.getBuildings().stream()
                .sorted(Comparator.comparingInt(PlayerBuilding::getNumber))
                .map(Building::getName)
                .collect(Collectors.toList());

        // TODO sorting
        stationMasters = playerState.getStationMasters();

        // TODO sorting
        hazards = playerState.getHazards().stream().map(HazardView::new).collect(Collectors.toList());

        // TODO sorting
        teepees = playerState.getTeepees();

        var objectivesScores = playerState.scoreObjectives(state);
        objectives = Stream.concat(playerState.getCommittedObjectives().stream(), playerState.getOptionalObjectives())
                .map(objectiveCard -> new ObjectiveView(new ObjectiveCardView(objectiveCard), objectivesScores.getScore(objectiveCard)))
                .sorted()
                .collect(Collectors.toList());

        this.score = state.scoreDetails(playerState.getPlayer()).map(ScoreView::new).orElse(null);

        if (state.isEnded()) {
            winner = state.winners().contains(playerState.getPlayer());
        }
    }
}
