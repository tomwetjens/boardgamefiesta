package com.boardgamefiesta.domain.rating;

import com.boardgamefiesta.domain.game.Game;
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

import static org.assertj.core.api.Assertions.assertThat;
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
            var adjustedAfterWinningAgainst1Opponent = a1.adjust(Map.of(A, a1, B, b1), table);

            when(table.getUserRanking()).thenReturn(List.of(A, B, C, D));
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

            assertThat(a.adjust(ratings, table).getRating()).isEqualTo(1024);
            assertThat(b.adjust(ratings, table).getRating()).isEqualTo(1008);
            assertThat(c.adjust(ratings, table).getRating()).isEqualTo(992);
            assertThat(d.adjust(ratings, table).getRating()).isEqualTo(976);
        }
    }
}