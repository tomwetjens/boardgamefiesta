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

    int balance;
    int cowboys;
    int craftsmen;
    int engineers;
    int tempCertificates;
    int certificates;

    ScoreView score;
    Boolean winner;

    List<CardView> hand;
    Integer handValue;
    List<CardView> discardPile;
    Integer drawStackSize;
    Integer handSize;

    Map<Unlockable, Integer> unlocked;
    List<String> buildings;
    Set<StationMaster> stationMasters;
    List<HazardView> hazards;
    List<Teepee> teepees;
    List<ObjectiveView> objectives;

    PlayerStateView(@NonNull Game state, @NonNull PlayerState playerState, @NonNull Player viewingPlayer) {
        player = new PlayerView(playerState.getPlayer());

        balance = playerState.getBalance();
        cowboys = playerState.getNumberOfCowboys();
        craftsmen = playerState.getNumberOfCraftsmen();
        engineers = playerState.getNumberOfEngineers();
        tempCertificates = playerState.getTempCertificates();
        certificates = playerState.getTempCertificates() + playerState.permanentCertificates();

        discardPile = playerState.getDiscardPile().stream()
                .map(CardView::of)
                .collect(Collectors.toList());
        Collections.reverse(discardPile);

        drawStackSize = playerState.getDrawStackSize();
        handSize = playerState.getHand().size();

        if (viewingPlayer == playerState.getPlayer()) {
            hand = playerState.getHand().stream()
                    .map(CardView::of)
                    .sorted()
                    .collect(Collectors.toList());
            handValue = playerState.handValue();
        } else {
            hand = null;
            handValue = null;
        }

        unlocked = playerState.getUnlocked();

        buildings = playerState.getBuildings().stream()
                .sorted(Comparator.comparingInt(PlayerBuilding::getNumber))
                .map(Building::getName)
                .collect(Collectors.toList());

        stationMasters = playerState.getStationMasters();

        hazards = playerState.getHazards().stream().map(HazardView::new).collect(Collectors.toList());

        teepees = playerState.getTeepees();

        var objectivesScores = playerState.scoreObjectives(state);
        objectives = Stream.concat(playerState.getCommittedObjectives().stream(), playerState.getOptionalObjectives())
                .map(objectiveCard -> new ObjectiveView(new ObjectiveCardView(objectiveCard), objectivesScores.get(objectiveCard)))
                .sorted()
                .collect(Collectors.toList());

        this.score = new ScoreView(state.scoreDetails(playerState.getPlayer()));
        if (state.isEnded()) {
            winner = state.winners().contains(playerState.getPlayer());
        }
    }
}
