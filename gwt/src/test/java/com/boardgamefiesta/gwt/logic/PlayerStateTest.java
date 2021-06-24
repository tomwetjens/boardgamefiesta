package com.boardgamefiesta.gwt.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerStateTest {

    static final PlayerBuilding.BuildingSet BUILDING_SET = PlayerBuilding.BuildingSet.from(GWT.Options.builder()
            .buildings(GWT.Options.Buildings.BEGINNER)
            .build(), new Random(0));

    static final GWT.Options OPTIONS = GWT.Options.builder().build();

    private Player player = new Player("A", PlayerColor.WHITE, Player.Type.HUMAN);

    @Nested
    class Create {

        @Test
        void create() {
            PlayerState playerState = new PlayerState(player, OPTIONS, 6, new Random(0),
                    BUILDING_SET);

            assertThat(playerState.getBalance()).isEqualTo(6);
            assertThat(playerState.getTempCertificates()).isEqualTo(0);
            assertThat(playerState.getNumberOfCowboys()).isEqualTo(1);
            assertThat(playerState.getNumberOfCraftsmen()).isEqualTo(1);
            assertThat(playerState.getNumberOfEngineers()).isEqualTo(1);
            assertThat(playerState.getStepLimit(2)).isEqualTo(3);
            assertThat(playerState.getTempCertificateLimit()).isEqualTo(3);
            assertThat(playerState.getHandLimit()).isEqualTo(4);
            assertThat(playerState.getHazards()).isEmpty();
            assertThat(playerState.getTeepees()).isEmpty();
            assertThat(playerState.getStationMasters()).isEmpty();
            assertThat(playerState.getBuildings()).hasSize(10);
            assertThat(playerState.getHand()).hasSize(4);
            assertThat(playerState.getDiscardPile()).isEmpty();
        }
    }

    @Nested
    class Cards {

        private PlayerState playerState;

        @BeforeEach
        void setUp() {
            playerState = new PlayerState(player, OPTIONS, 6, new Random(0), BUILDING_SET);
        }

        @Test
        void discardAllCards() {
            HashSet<Card> hand = new HashSet<>(playerState.getHand());
            playerState.discardHand();

            assertThat(playerState.getHand()).isEmpty();
            assertThat(playerState.getDiscardPile()).containsAnyElementsOf(hand);
        }

        @Test
        void discard() {
            Card card = playerState.getHand().iterator().next();
            playerState.discardCard(card);

            assertThat(playerState.getHand()).hasSize(3);
            assertThat(playerState.getHand()).doesNotContain(card);
            assertThat(playerState.getDiscardPile()).contains(card);
        }

        @Test
        void discardNotInHand() {
            Card card = playerState.getHand().iterator().next();
            playerState.discardCard(card);

            assertThatThrownBy(() -> playerState.discardCard(card)).hasMessage(GWTError.CARD_NOT_IN_HAND.toString());
        }

        @Test
        void discardCattleCard() {
            Set<Card.CattleCard> cards = playerState.discardCattleCards(CattleType.JERSEY, 1);

            assertThat(playerState.getHand()).hasSize(3);
            assertThat(playerState.getHand()).doesNotContainAnyElementsOf(cards);
            assertThat(playerState.getDiscardPile()).containsAll(cards);
        }

        @Test
        void discardCattleCards() {
            Set<Card.CattleCard> cards = playerState.discardCattleCards(CattleType.BLACK_ANGUS, 2);

            assertThat(playerState.getHand()).hasSize(2);
            assertThat(playerState.getHand()).doesNotContainAnyElementsOf(cards);
            assertThat(playerState.getDiscardPile()).containsAll(cards);
        }

        @Test
        void discardCattleCardsNotInHand() {
            assertThatThrownBy(() -> playerState.discardCattleCards(CattleType.HOLSTEIN, 1)).hasMessage(GWTError.CATTLE_CARDS_NOT_IN_HAND.toString());

            assertThat(playerState.getHand()).hasSize(4);
        }

        @Test
        void discardCattleCardsNotEnoughInHand() {
            assertThatThrownBy(() -> playerState.discardCattleCards(CattleType.BLACK_ANGUS, 3)).hasMessage(GWTError.CATTLE_CARDS_NOT_IN_HAND.toString());

            assertThat(playerState.getHand()).hasSize(4);
        }

        @Test
        void drawUpToHandLimit() {
            Card card = playerState.getHand().iterator().next();
            playerState.discardCard(card);

            playerState.drawUpToHandLimit(new Random(0));

            assertThat(playerState.getHand()).hasSize(4);
        }

        @Test
        void gainCard() {
            Card.CattleCard card = new Card.CattleCard(CattleType.TEXAS_LONGHORN, 6);

            playerState.gainCard(card);

            assertThat(playerState.getHand()).doesNotContain(card);
            assertThat(playerState.getDiscardPile().get(0)).isSameAs(card);
        }
    }

    @Nested
    class HandValue {

        @Test
        void handValue() {
            PlayerState playerState = PlayerState.builder()
                    .hand(new HashSet<>(asList(
                            new Card.CattleCard(CattleType.JERSEY, 0),
                            new Card.CattleCard(CattleType.JERSEY, 0),
                            new Card.CattleCard(CattleType.GUERNSEY, 0),
                            new Card.CattleCard(CattleType.GUERNSEY, 0),
                            new Card.CattleCard(CattleType.BLACK_ANGUS, 0),
                            new Card.CattleCard(CattleType.BLACK_ANGUS, 0),
                            new Card.CattleCard(CattleType.HOLSTEIN, 3),
                            new Card.CattleCard(CattleType.HOLSTEIN, 3),
                            new Card.CattleCard(CattleType.DUTCH_BELT, 3),
                            new Card.CattleCard(CattleType.DUTCH_BELT, 3),
                            new Card.CattleCard(CattleType.AYRSHIRE, 3),
                            new Card.CattleCard(CattleType.AYRSHIRE, 3),
                            new Card.CattleCard(CattleType.BROWN_SWISS, 3),
                            new Card.CattleCard(CattleType.BROWN_SWISS, 3),
                            new Card.CattleCard(CattleType.WEST_HIGHLAND, 4),
                            new Card.CattleCard(CattleType.WEST_HIGHLAND, 4),
                            new Card.CattleCard(CattleType.TEXAS_LONGHORN, 5),
                            new Card.CattleCard(CattleType.TEXAS_LONGHORN, 7)
                    )))
                    .build();

            assertThat(playerState.handValue()).isEqualTo(25);
        }

    }

    @Nested
    class Hazards {

        @Test
        void add() {
            PlayerState playerState = PlayerState.builder().hazards(new HashSet<>()).build();

            Hazard hazard = new Hazard(HazardType.FLOOD, Hand.GREEN, 2);
            playerState.addHazard(hazard);

            assertThat(playerState.getHazards()).containsExactly(hazard);
        }

        @Test
        void addDuplicate() {
            Hazard hazard = new Hazard(HazardType.FLOOD, Hand.GREEN, 2);
            PlayerState playerState = PlayerState.builder().hazards(new HashSet<>()).build();
            playerState.addHazard(hazard);

            assertThatThrownBy(() -> playerState.addHazard(hazard)).hasMessage(GWTError.ALREADY_HAS_HAZARD.toString());
        }

        @Test
        void addSimilar() {
            Hazard hazard = new Hazard(HazardType.FLOOD, Hand.GREEN, 2);
            PlayerState playerState = PlayerState.builder().hazards(new HashSet<>()).build();
            playerState.addHazard(hazard);

            Hazard hazard2 = new Hazard(HazardType.FLOOD, Hand.GREEN, 2);
            playerState.addHazard(hazard2);

            assertThat(playerState.getHazards()).containsExactlyInAnyOrder(hazard, hazard2);
        }
    }

    @Nested
    class Certificates {

        @Test
        void gain1() {
            PlayerState playerState = PlayerState.builder()
                    .tempCertificates(0)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .unlocked(Unlockable.CERT_LIMIT_6, 1)
                    .build();

            playerState.gainTempCertificates(1);
            assertThat(playerState.getTempCertificates()).isEqualTo(1);

            playerState.gainTempCertificates(1);
            assertThat(playerState.getTempCertificates()).isEqualTo(2);

            playerState.gainTempCertificates(1);
            assertThat(playerState.getTempCertificates()).isEqualTo(3);

            playerState.gainTempCertificates(1);
            assertThat(playerState.getTempCertificates()).isEqualTo(4);

            playerState.gainTempCertificates(1);
            assertThat(playerState.getTempCertificates()).isEqualTo(6);
        }

        @Test
        void gain2() {
            PlayerState playerState = PlayerState.builder()
                    .tempCertificates(0)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .unlocked(Unlockable.CERT_LIMIT_6, 1)
                    .build();

            playerState.gainTempCertificates(2);
            assertThat(playerState.getTempCertificates()).isEqualTo(2);

            playerState.gainTempCertificates(2);
            assertThat(playerState.getTempCertificates()).isEqualTo(4);

            playerState.gainTempCertificates(2);
            assertThat(playerState.getTempCertificates()).isEqualTo(6);
        }

        @Test
        void limit4() {
            PlayerState playerState = PlayerState.builder()
                    .tempCertificates(0)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .build();

            playerState.gainTempCertificates(4);

            assertThat(playerState.getTempCertificates()).isEqualTo(4);
        }

        @Test
        void limit6() {
            PlayerState playerState = PlayerState.builder()
                    .tempCertificates(0)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .unlocked(Unlockable.CERT_LIMIT_6, 1)
                    .build();

            playerState.gainTempCertificates(6);

            assertThat(playerState.getTempCertificates()).isEqualTo(6);
        }

        @Test
        void max4() {
            PlayerState playerState = PlayerState.builder()
                    .tempCertificates(0)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .build();

            playerState.gainMaxTempCertificates();

            assertThat(playerState.getTempCertificates()).isEqualTo(4);
        }

        @Test
        void max6() {
            PlayerState playerState = PlayerState.builder()
                    .tempCertificates(0)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .unlocked(Unlockable.CERT_LIMIT_6, 1)
                    .build();

            playerState.gainMaxTempCertificates();

            assertThat(playerState.getTempCertificates()).isEqualTo(6);
        }

        @Test
        void spend1() {
            PlayerState playerState = PlayerState.builder()
                    .tempCertificates(6)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .unlocked(Unlockable.CERT_LIMIT_6, 1)
                    .build();

            playerState.spendTempCertificates(1);
            assertThat(playerState.getTempCertificates()).isEqualTo(4);

            playerState.spendTempCertificates(1);
            assertThat(playerState.getTempCertificates()).isEqualTo(3);

            playerState.spendTempCertificates(1);
            assertThat(playerState.getTempCertificates()).isEqualTo(2);

            playerState.spendTempCertificates(1);
            assertThat(playerState.getTempCertificates()).isEqualTo(1);

            playerState.spendTempCertificates(1);
            assertThat(playerState.getTempCertificates()).isEqualTo(0);

            assertThatThrownBy(() -> playerState.spendTempCertificates(1)).hasMessage(GWTError.NOT_ENOUGH_CERTIFICATES.toString());
        }

        @Test
        void spend2() {
            PlayerState playerState = PlayerState.builder()
                    .tempCertificates(6)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .unlocked(Unlockable.CERT_LIMIT_6, 1)
                    .build();

            playerState.spendTempCertificates(2);
            assertThat(playerState.getTempCertificates()).isEqualTo(4);

            playerState.spendTempCertificates(2);
            assertThat(playerState.getTempCertificates()).isEqualTo(2);

            playerState.spendTempCertificates(2);
            assertThat(playerState.getTempCertificates()).isEqualTo(0);

            assertThatThrownBy(() -> playerState.spendTempCertificates(2)).hasMessage(GWTError.NOT_ENOUGH_CERTIFICATES.toString());
        }

        @Test
        void spend4() {
            PlayerState playerState = PlayerState.builder()
                    .tempCertificates(6)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .unlocked(Unlockable.CERT_LIMIT_6, 1)
                    .build();

            playerState.spendTempCertificates(4);
            assertThat(playerState.getTempCertificates()).isEqualTo(2);

            assertThatThrownBy(() -> playerState.spendTempCertificates(4)).hasMessage(GWTError.NOT_ENOUGH_CERTIFICATES.toString());
        }
    }

    @Nested
    class Buildings {

        private PlayerState playerState;

        @BeforeEach
        void setUp() {
            playerState = new PlayerState(player, OPTIONS, 6, new Random(0), BUILDING_SET);
        }

        @Test
        void removeBuilding() {
            PlayerBuilding building = playerState.getBuildings().iterator().next();

            playerState.removeBuilding(building);

            assertThat(playerState.getBuildings()).hasSize(9);
            assertThat(playerState.getBuildings()).doesNotContain(building);
        }

        @Test
        void removeBuildingNotAvailable() {
            PlayerBuilding building = playerState.getBuildings().iterator().next();
            playerState.removeBuilding(building);

            assertThatThrownBy(() -> playerState.removeBuilding(building)).hasMessage(GWTError.BUILDING_NOT_AVAILABLE.toString());
        }
    }

    @Nested
    class Score {

        @Mock
        private GWT game;

        @Mock
        private Trail trail;

        @Mock
        private RailroadTrack railroadTrack;

        @BeforeEach
        void setUp() {
            when(game.getTrail()).thenReturn(trail);
            when(game.getRailroadTrack()).thenReturn(railroadTrack);
        }

        @Test
        void balance() {
            var playerState = PlayerState.builder()
                    .player(player)
                    .balance(24)
                    .hand(Collections.emptySet())
                    .drawStack(new LinkedList<>())
                    .discardPile(Collections.emptyList())
                    .teepees(Collections.emptyList())
                    .hazards(Collections.emptySet())
                    .objectives(Collections.emptySet())
                    .stationMasters(Collections.emptySet())
                    .workers(Collections.emptyMap())
                    .build();

            when(game.playerState(player)).thenReturn(playerState);

            assertThat(playerState.score(game).getTotal()).isEqualTo(4);
            assertThat(playerState.score(game).getCategories()).containsEntry(ScoreCategory.DOLLARS, 4);
        }
    }

    @Test
    void payDollarsNegative() {
        var playerState = new PlayerState(player, OPTIONS, 6, new Random(0), BUILDING_SET);

        assertThatThrownBy(() -> playerState.payDollars(-1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(playerState.getBalance()).isEqualTo(6);
    }

    @Test
    void gainDollarsNegative() {
        var playerState = new PlayerState(player, OPTIONS, 6, new Random(0), BUILDING_SET);

        assertThatThrownBy(() -> playerState.gainDollars(-1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(playerState.getBalance()).isEqualTo(6);
    }
}
