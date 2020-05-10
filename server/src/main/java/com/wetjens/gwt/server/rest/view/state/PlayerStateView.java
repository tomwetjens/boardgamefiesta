package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Building;
import com.wetjens.gwt.Game;
import com.wetjens.gwt.Player;
import com.wetjens.gwt.PlayerBuilding;
import com.wetjens.gwt.PlayerState;
import com.wetjens.gwt.StationMaster;
import com.wetjens.gwt.Teepee;
import com.wetjens.gwt.Unlockable;
import lombok.NonNull;
import lombok.Value;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class PlayerStateView {

    PlayerView player;

    int balance;
    int cowboys;
    int craftsmen;
    int engineers;
    int tempCertificates;
    int certificates;

    ScoreView score;

    List<CardView> hand;
    Integer handValue;
    List<CardView> discardPile;
    Integer drawStackSize;

    Map<Unlockable, Integer> unlocked;
    List<String> buildings;
    Set<StationMaster> stationMasters;
    List<HazardView> hazards;
    List<Teepee> teepees;
    List<ObjectiveCardView> objectives;

    PlayerStateView(@NonNull Game game, @NonNull PlayerState playerState, @NonNull Player viewingPlayer) {
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

        drawStackSize = playerState.getDrawStackSize();

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

        objectives = playerState.getObjectives().stream()
                .map(ObjectiveCardView::new)
                .sorted()
                .collect(Collectors.toList());

        if (game.isEnded()) {
            this.score = new ScoreView(game.score(playerState.getPlayer()), game.winners().contains(playerState.getPlayer()));
        } else {
            this.score = null;
        }
    }
}
