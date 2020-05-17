package com.wetjens.gwt;

import com.wetjens.gwt.api.Player;
import com.wetjens.gwt.api.PlayerColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionTest {

    @ParameterizedTest
    @MethodSource("allActions")
    void accessible(Class<? extends Action> action) {
        assertThat(action).isPublic();
        assertThat(action.getConstructors()).isNotEmpty();
        assertThat(Modifier.isAbstract(action.getModifiers())).isFalse();
    }

    static Stream<Arguments> allActions() {
        return Arrays.stream(Action.class.getDeclaredClasses())
                .filter(Action.class::isAssignableFrom)
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .map(Arguments::of);
    }

    Player player = new Player("Player A", PlayerColor.RED);

    @Mock
    Game game;

    @Mock
    PlayerState playerState;

    @Mock
    RailroadTrack railroadTrack;

    @Mock
    Trail trail;

    @BeforeEach
    void setUp() {
        lenient().when(game.getCurrentPlayer()).thenReturn(player);
        lenient().when(game.currentPlayerState()).thenReturn(playerState);
        lenient().when(game.getRailroadTrack()).thenReturn(railroadTrack);
        lenient().when(game.getTrail()).thenReturn(trail);
    }

    @Nested
    class DeliverToCity {

        @Test
        void notEnoughCertificates() {
// TODO
        }

        @Test
        void passedNotAllSignals() {
// TODO
        }

        @Test
        void passedAllSignals() {
            when(playerState.handValue()).thenReturn(11);
            when(railroadTrack.signalsPassed(player)).thenReturn(7);

            Action.DeliverToCity deliverToCity = new Action.DeliverToCity(City.EL_PASO, 1);
            deliverToCity.perform(game, new Random(0));

            verify(playerState).gainDollars(12);
            verify(playerState).discardHand();
            verify(trail).moveToStart(player);
        }

        @Test
        void spendOnlyPermCerts() {
// TODO
        }

        @Test
        void spendTempCerts() {
// TODO
        }

        @Test
        void toEventParams() {
            when(playerState.handValue()).thenReturn(11);
            when(railroadTrack.signalsPassed(player)).thenReturn(4);

            Action.DeliverToCity deliverToCity = new Action.DeliverToCity(City.EL_PASO, 1);
            List<String> eventParams = deliverToCity.toEventParams(game);

            assertThat(eventParams).containsExactly("EL_PASO", "1", "9");
        }
    }
}
