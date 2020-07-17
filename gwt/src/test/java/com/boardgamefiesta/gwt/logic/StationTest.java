package com.boardgamefiesta.gwt.logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationTest {

    @Mock
    Game game;

    @Mock
    PlayerState currentPlayerState;

    @BeforeEach
    void setUp() {
        lenient().when(game.currentPlayerState()).thenReturn(currentPlayerState);
    }

    @Test
    void upgrade() {
        var station = Station.initial(3, 1, Collections.singleton(DiscColor.WHITE), StationMaster.PERM_CERT_POINTS_FOR_EACH_2_CERTS);
        when(game.placeDisc(anyCollection())).thenReturn(ImmediateActions.of(PossibleAction.mandatory(Action.UnlockWhite.class)));

        var immediateActions = station.upgrade(game);

        assertThat(immediateActions.getActions()).hasSize(2);
        assertThat(immediateActions.getActions().get(0).canPerform(Action.UnlockWhite.class)).isTrue();
        assertThat(immediateActions.getActions().get(1).canPerform(Action.AppointStationMaster.class)).isTrue();
    }
}