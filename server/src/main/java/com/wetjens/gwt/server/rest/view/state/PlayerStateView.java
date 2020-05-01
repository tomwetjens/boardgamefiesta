package com.wetjens.gwt.server.rest.view.state;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.wetjens.gwt.Building;
import com.wetjens.gwt.Card;
import com.wetjens.gwt.CattleType;
import com.wetjens.gwt.ObjectiveCard;
import com.wetjens.gwt.Player;
import com.wetjens.gwt.PlayerBuilding;
import com.wetjens.gwt.PlayerState;
import com.wetjens.gwt.StationMaster;
import com.wetjens.gwt.Teepee;
import com.wetjens.gwt.Unlockable;
import com.wetjens.gwt.server.domain.User;
import lombok.NonNull;
import lombok.Value;

@Value
public class PlayerStateView {

    PlayerView player;

    int balance;
    int cowboys;
    int craftsmen;
    int engineers;
    int certificates;

    List<CardView> hand;
    List<CardView> discardPile;
    Integer drawStackSize;

    Map<Unlockable, Integer> unlocked;
    List<String> buildings;
    Set<StationMaster> stationMasters;
    List<HazardView> hazards;
    List<Teepee> teepees;
    List<ObjectiveCardView> objectives;

    PlayerStateView(@NonNull PlayerState playerState, @NonNull Player viewingPlayer, User user) {
        player = new PlayerView(playerState.getPlayer(), user);

        balance = playerState.getBalance();
        cowboys = playerState.getNumberOfCowboys();
        craftsmen = playerState.getNumberOfCraftsmen();
        engineers = playerState.getNumberOfEngineers();
        certificates = playerState.getTempCertificates();

        if (viewingPlayer == playerState.getPlayer()) {
            hand = playerState.getHand().stream()
                    .map(CardView::of)
                    .sorted()
                    .collect(Collectors.toList());

            discardPile = playerState.getDiscardPile().stream()
                    .map(CardView::of)
                    .collect(Collectors.toList());

            drawStackSize = playerState.getDrawStackSize();
        } else {
            hand = null;
            discardPile = null;
            drawStackSize = null;
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
    }
}
