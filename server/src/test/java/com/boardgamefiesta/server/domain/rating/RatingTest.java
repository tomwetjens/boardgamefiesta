package com.boardgamefiesta.server.domain.rating;

import com.boardgamefiesta.api.Game;
import com.boardgamefiesta.server.domain.Table;
import com.boardgamefiesta.server.domain.User;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RatingTest {

    private static final Offset<Float> STRICT = Offset.strictOffset(0.01f);

    private static final Game.Id GAME_ID = Game.Id.of("aGame");
    private static final Table.Id TABLE_ID = Table.Id.of("aTable");

    private static final User.Id A = User.Id.of("A");
    private static final User.Id B = User.Id.of("B");
    private static final User.Id C = User.Id.of("C");

    @Nested
    class Initial {
        @Test
        void initial() {
            assertThat(Rating.initial(A, GAME_ID).getRating()).isEqualTo(0, STRICT);
        }
    }

    @Nested
    class ExpectedScoreAgainst {

        @Test
        void bothEqual() {
            var a = Rating.initial(A, GAME_ID, 100);
            var b = Rating.initial(B, GAME_ID, 100);

            assertThat(a.expectedAgainst(b)).isEqualTo(0.5f, STRICT);
        }

        @Test
        void difference50() {
            var a = Rating.initial(A, GAME_ID, 150);
            var b = Rating.initial(B, GAME_ID, 100);

            assertThat(a.expectedAgainst(b)).isEqualTo(0.57f, STRICT);
        }

        @Test
        void difference300() {
            var a = Rating.initial(A, GAME_ID, 400);
            var b = Rating.initial(B, GAME_ID, 100);

            assertThat(a.expectedAgainst(b)).isEqualTo(0.85f, STRICT);
        }

        @Test
        void difference1000() {
            var a = Rating.initial(A, GAME_ID, 1100);
            var b = Rating.initial(B, GAME_ID, 100);

            assertThat(a.expectedAgainst(b)).isEqualTo(0.99f, STRICT);
        }

        @Test
        void total() {
            var a1 = Rating.initial(A, GAME_ID, 150);
            var b1 = Rating.initial(B, GAME_ID, 100);
            var c1 = Rating.initial(C, GAME_ID, 200);

            var aAgainstB = a1.expectedAgainst(b1);
            assertThat(aAgainstB).isEqualTo(0.57f, STRICT);

            var aAgainstC = a1.expectedAgainst(c1);
            assertThat(aAgainstC).isEqualTo(0.43f, STRICT);

            var bAgainstA = b1.expectedAgainst(a1);
            assertThat(bAgainstA).isEqualTo(0.43f, STRICT);

            var bAgainstC = b1.expectedAgainst(c1);
            assertThat(bAgainstC).isEqualTo(0.36f, STRICT);

            var cAgainstA = c1.expectedAgainst(a1);
            assertThat(cAgainstA).isEqualTo(0.57f, STRICT);

            var cAgainstB = c1.expectedAgainst(b1);
            assertThat(cAgainstB).isEqualTo(0.64f, STRICT);

            assertThat(bAgainstA + aAgainstB).isEqualTo(1);
            assertThat(cAgainstA + aAgainstC).isEqualTo(1);
            assertThat(cAgainstB + bAgainstC).isEqualTo(1);
            assertThat(aAgainstC + cAgainstA).isEqualTo(1);
        }
    }

    @Nested
    class Adjust {

        @Test
        void belowMinRating() {
            var a1 = Rating.initial(A, GAME_ID, 50);
            var b1 = Rating.initial(B, GAME_ID, 50);

            var ratings = Set.of(a1, b1);
            var scores = Map.of(A, 1, B, 0);

            var a2 = a1.adjust(ratings, TABLE_ID, scores, 1);
            var b2 = b1.adjust(ratings, TABLE_ID, scores, 0);

            assertThat(a2.getRating()).isEqualTo(66f, STRICT);
            assertThat(a2.getDeltas().get(B)).isEqualTo(16f, STRICT);

            assertThat(b2.getRating()).isEqualTo(50f, STRICT);
            assertThat(b2.getDeltas().get(A)).isEqualTo(0, STRICT);
        }

        @Test
        void multiplePlayers() {
            var a1 = Rating.initial(A, GAME_ID, 150);
            var b1 = Rating.initial(B, GAME_ID, 100);
            var c1 = Rating.initial(C, GAME_ID, 200);

            var ratings = Set.of(a1, b1, c1);
            var scores = Map.of(A, 112, B, 90, C, 64);

            var a2 = a1.adjust(ratings, TABLE_ID, scores, 112);
            var b2 = b1.adjust(ratings, TABLE_ID, scores, 90);
            var c2 = c1.adjust(ratings, TABLE_ID, scores, 64);

            assertThat(a2.getRating()).isEqualTo(166f, STRICT);
            assertThat(a2.getDeltas().get(B)).isEqualTo(6.86f, STRICT);
            assertThat(a2.getDeltas().get(C)).isEqualTo(9.14f, STRICT);

            assertThat(b2.getRating()).isEqualTo(110.24f, STRICT);
            assertThat(b2.getDeltas().get(A)).isEqualTo(0, STRICT);
            assertThat(b2.getDeltas().get(C)).isEqualTo(10.24f, STRICT);

            assertThat(c2.getRating()).isEqualTo(180.62f, STRICT);
            assertThat(c2.getDeltas().get(A)).isEqualTo(-9.14f, STRICT);
            assertThat(c2.getDeltas().get(B)).isEqualTo(-10.24f, STRICT);
        }

        @Test
        void cannotLoseBelowMinRating() {
            var a1 = Rating.initial(A, GAME_ID, 50);
            var b1 = Rating.initial(B, GAME_ID, 100);

            var ratings = Set.of(a1, b1);
            var scores = Map.of(A, 0, B, 1);

            var a2 = a1.adjust(ratings, TABLE_ID, scores, 0);

            assertThat(a2.getRating()).isEqualTo(a1.getRating());
        }

        @Test
        void cannotLoseAtMinRating() {
            var a1 = Rating.initial(A, GAME_ID, 100);
            var b1 = Rating.initial(B, GAME_ID, 100);

            var ratings = Set.of(a1, b1);
            var scores = Map.of(A, 0, B, 1);

            var a2 = a1.adjust(ratings, TABLE_ID, scores, 1);

            assertThat(a2.getRating()).isEqualTo(a1.getRating());
        }

        @Test
        void canStillGainFromPlayerBelowMinRating() {
            var a1 = Rating.initial(A, GAME_ID, 100);
            var b1 = Rating.initial(B, GAME_ID, 50);

            var ratings = Set.of(a1, b1);
            var scores = Map.of(A, 1, B, 0);

            var a2 = a1.adjust(ratings, TABLE_ID, scores, 1);

            assertThat(a2.getRating()).isGreaterThan(a1.getRating());
        }

        @Test
        void canStillGainFromPlayerAtMinRating() {
            var a1 = Rating.initial(A, GAME_ID, 100);
            var b1 = Rating.initial(B, GAME_ID, 100);

            var ratings = Set.of(a1, b1);
            var scores = Map.of(A, 1, B, 0);

            var a2 = a1.adjust(ratings, TABLE_ID, scores, 1);

            assertThat(a2.getRating()).isGreaterThan(a1.getRating());
        }
    }
}