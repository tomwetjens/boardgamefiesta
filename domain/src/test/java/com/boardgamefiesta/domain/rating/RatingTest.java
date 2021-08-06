/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.domain.rating;

import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingTest {

    private static final Game.Id GAME_ID = Game.Id.of("aGame");

    private static final User.Id A = User.Id.of("A");
    private static final User.Id B = User.Id.of("B");
    private static final User.Id C = User.Id.of("C");
    private static final User.Id D = User.Id.of("D");

    @Mock
    Game game;

    @Mock
    Table table;

    @Mock
    Player playerA;
    @Mock
    Player playerB;
    @Mock
    Player playerC;
    @Mock
    Player playerD;

    @BeforeEach
    void setUp() {
        lenient().when(table.getPlayerByUserId(A)).thenReturn(Optional.of(playerA));
        lenient().when(table.getPlayerByUserId(B)).thenReturn(Optional.of(playerB));
        lenient().when(table.getPlayerByUserId(C)).thenReturn(Optional.of(playerC));
        lenient().when(table.getPlayerByUserId(D)).thenReturn(Optional.of(playerD));

        lenient().when(playerA.isPlaying()).thenReturn(true);
        lenient().when(playerA.isUser()).thenReturn(true);
        lenient().when(playerA.getUserId()).thenReturn(Optional.of(A));

        lenient().when(playerB.isPlaying()).thenReturn(true);
        lenient().when(playerB.isUser()).thenReturn(true);
        lenient().when(playerB.getUserId()).thenReturn(Optional.of(B));

        lenient().when(playerC.isPlaying()).thenReturn(true);
        lenient().when(playerC.isUser()).thenReturn(true);
        lenient().when(playerC.getUserId()).thenReturn(Optional.of(C));

        lenient().when(playerD.isPlaying()).thenReturn(true);
        lenient().when(playerD.isUser()).thenReturn(true);
        lenient().when(playerD.getUserId()).thenReturn(Optional.of(D));
    }

    @Nested
    class Initial {
        @Test
        void initial() {
            assertThat(Rating.initial(A, GAME_ID).getRating()).isEqualTo(1000);
        }
    }

    @Nested
    class Adjust {

        @BeforeEach
        void setUp() {
            when(game.getId()).thenReturn(GAME_ID);

            when(table.getId()).thenReturn(Table.Id.of("aTable"));
            when(table.getGame()).thenReturn(game);
            when(table.getEnded()).thenReturn(Instant.now());
        }

        @Test
        void multiplePlayers() {
            var a1 = Rating.initial(A, GAME_ID, 150);
            var b1 = Rating.initial(B, GAME_ID, 100);
            var c1 = Rating.initial(C, GAME_ID, 200);

            var ratings = Map.of(A, a1, B, b1, C, c1);
            when(table.getUserRanking()).thenReturn(List.of(A, B, C));
            when(table.getPlayers()).thenReturn(Set.of(playerA, playerB, playerC));

            var a2 = a1.adjust(ratings, table);
            var b2 = b1.adjust(ratings, table);
            var c2 = c1.adjust(ratings, table);

            assertThat(a2.getRating()).isEqualTo(171);
            assertThat(a2.getDeltas().get(B)).isEqualTo(9);
            assertThat(a2.getDeltas().get(C)).isEqualTo(12);

            assertThat(b2.getRating()).isEqualTo(105);
            assertThat(b2.getDeltas().get(A)).isEqualTo(-9);
            assertThat(b2.getDeltas().get(C)).isEqualTo(14);

            assertThat(c2.getRating()).isEqualTo(174);
            assertThat(c2.getDeltas().get(A)).isEqualTo(-12);
            assertThat(c2.getDeltas().get(B)).isEqualTo(-14);
        }

        @Test
        void gainMoreWhenWinningAgainstMoreHigherRatedPlayers() {
            var a1 = Rating.initial(A, GAME_ID, 100);
            var b1 = Rating.initial(B, GAME_ID, 200);
            var c1 = Rating.initial(C, GAME_ID, 200);
            var d1 = Rating.initial(D, GAME_ID, 200);

            when(table.getUserRanking()).thenReturn(List.of(A, B));
            when(table.getPlayers()).thenReturn(Set.of(playerA, playerB));
            var adjustedAfterWinningAgainst1Opponent = a1.adjust(Map.of(A, a1, B, b1), table);

            when(table.getUserRanking()).thenReturn(List.of(A, B, C, D));
            when(table.getPlayers()).thenReturn(Set.of(playerA, playerB, playerC, playerD));
            var adjustedAfterWinningAgainst3Opponents = a1.adjust(Map.of(A, a1, B, b1, C, c1, D, d1), table);

            assertThat(adjustedAfterWinningAgainst3Opponents.getRating())
                    .isGreaterThan(adjustedAfterWinningAgainst1Opponent.getRating());
        }

        @Test
        void fernando2P() {
            var a = Rating.initial(A, GAME_ID, 1000);
            var b = Rating.initial(B, GAME_ID, 1000);

            var ratings = Map.of(A, a, B, b);
            when(table.getUserRanking()).thenReturn(List.of(A, B));
            when(table.getPlayers()).thenReturn(Set.of(playerA, playerB));

            assertThat(a.adjust(ratings, table).getRating()).isEqualTo(1016);
            assertThat(b.adjust(ratings, table).getRating()).isEqualTo(984);
        }

        @Test
        void fernando3P() {
            var a = Rating.initial(A, GAME_ID, 1000);
            var b = Rating.initial(B, GAME_ID, 1000);
            var c = Rating.initial(C, GAME_ID, 1000);

            var ratings = Map.of(A, a, B, b, C, c);
            when(table.getUserRanking()).thenReturn(List.of(A, B, C));
            when(table.getPlayers()).thenReturn(Set.of(playerA, playerB, playerC));

            assertThat(a.adjust(ratings, table).getRating()).isEqualTo(1022);
            assertThat(b.adjust(ratings, table).getRating()).isEqualTo(1000);
            assertThat(c.adjust(ratings, table).getRating()).isEqualTo(978);
        }

        @Test
        void fernando4P() {
            var a = Rating.initial(A, GAME_ID, 1000);
            var b = Rating.initial(B, GAME_ID, 1000);
            var c = Rating.initial(C, GAME_ID, 1000);
            var d = Rating.initial(D, GAME_ID, 1000);

            var ratings = Map.of(A, a, B, b, C, c, D, d);
            when(table.getUserRanking()).thenReturn(List.of(A, B, C, D));
            when(table.getPlayers()).thenReturn(Set.of(playerA, playerB, playerC, playerD));

            assertThat(a.adjust(ratings, table).getRating()).isEqualTo(1024);
            assertThat(b.adjust(ratings, table).getRating()).isEqualTo(1008);
            assertThat(c.adjust(ratings, table).getRating()).isEqualTo(992);
            assertThat(d.adjust(ratings, table).getRating()).isEqualTo(976);
        }
    }
}