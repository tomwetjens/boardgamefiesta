package com.wetjens.gwt;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.*;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;

class PlayerStateTest {

    private Player player = new Player("Player A", Player.Color.WHITE);

    @Nested
    class Create {

        @Test
        void create() {
            PlayerState playerState = new PlayerState(player, 6, new Random(0), PlayerBuilding.BuildingSet.beginner());

            assertThat(playerState.getBalance()).isEqualTo(6);
            assertThat(playerState.getCertificates()).isEqualTo(0);
            assertThat(playerState.getNumberOfCowboys()).isEqualTo(1);
            assertThat(playerState.getNumberOfCraftsmen()).isEqualTo(1);
            assertThat(playerState.getNumberOfEngineers()).isEqualTo(1);
            assertThat(playerState.getStepLimit()).isEqualTo(3);
            assertThat(playerState.getCertificateLimit()).isEqualTo(4);
            assertThat(playerState.getHandLimit()).isEqualTo(4);
            assertThat(playerState.getHazards()).isEmpty();
            assertThat(playerState.getTeepees()).isEmpty();
            assertThat(playerState.getStationMasters()).isEmpty();
            assertThat(playerState.getObjectives()).isEmpty();
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
            playerState = new PlayerState(player, 6, new Random(0), PlayerBuilding.BuildingSet.beginner());
        }

        @Test
        void discardAllCards() {
            HashSet<Card> hand = new HashSet<>(playerState.getHand());
            playerState.discardAllCards();

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

            assertThatThrownBy(() -> playerState.discardCard(card)).hasMessage("Card must be in hand");
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
            assertThatThrownBy(() -> playerState.discardCattleCards(CattleType.HOLSTEIN, 1)).hasMessage("Player hand does not contain 1 cattle cards of type HOLSTEIN");

            assertThat(playerState.getHand()).hasSize(4);
        }

        @Test
        void discardCattleCardsNotEnoughInHand() {
            assertThatThrownBy(() -> playerState.discardCattleCards(CattleType.BLACK_ANGUS, 3)).hasMessage("Player hand does not contain 3 cattle cards of type BLACK_ANGUS");

            assertThat(playerState.getHand()).hasSize(4);
        }

        @Test
        void drawUpToHandLimit() {
            Card card = playerState.getHand().iterator().next();
            playerState.discardCard(card);

            playerState.drawUpToHandLimit();

            assertThat(playerState.getHand()).hasSize(4);
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
            PlayerState playerState = PlayerState.builder().build();

            Hazard hazard = new Hazard(HazardType.FLOOD, Hand.GREEN, 2);
            playerState.addHazard(hazard);

            assertThat(playerState.getHazards()).containsExactly(hazard);
        }

        @Test
        void addDuplicate() {
            Hazard hazard = new Hazard(HazardType.FLOOD, Hand.GREEN, 2);
            PlayerState playerState = PlayerState.builder().build();
            playerState.addHazard(hazard);

            assertThatThrownBy(() -> playerState.addHazard(hazard)).hasMessage("Already has hazard");
        }

        @Test
        void addSimilar() {
            Hazard hazard = new Hazard(HazardType.FLOOD, Hand.GREEN, 2);
            PlayerState playerState = PlayerState.builder().build();
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
                    .certificates(0)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .unlocked(Unlockable.CERT_LIMIT_6, 1)
                    .build();

            playerState.gainCertificates(1);
            assertThat(playerState.getCertificates()).isEqualTo(1);

            playerState.gainCertificates(1);
            assertThat(playerState.getCertificates()).isEqualTo(2);

            playerState.gainCertificates(1);
            assertThat(playerState.getCertificates()).isEqualTo(3);

            playerState.gainCertificates(1);
            assertThat(playerState.getCertificates()).isEqualTo(4);

            playerState.gainCertificates(1);
            assertThat(playerState.getCertificates()).isEqualTo(6);
        }

        @Test
        void gain2() {
            PlayerState playerState = PlayerState.builder()
                    .certificates(0)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .unlocked(Unlockable.CERT_LIMIT_6, 1)
                    .build();

            playerState.gainCertificates(2);
            assertThat(playerState.getCertificates()).isEqualTo(2);

            playerState.gainCertificates(2);
            assertThat(playerState.getCertificates()).isEqualTo(4);

            playerState.gainCertificates(2);
            assertThat(playerState.getCertificates()).isEqualTo(6);
        }

        @Test
        void limit4() {
            PlayerState playerState = PlayerState.builder()
                    .certificates(0)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .build();

            playerState.gainCertificates(4);

            assertThat(playerState.getCertificates()).isEqualTo(4);
        }

        @Test
        void limit6() {
            PlayerState playerState = PlayerState.builder()
                    .certificates(0)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .unlocked(Unlockable.CERT_LIMIT_6, 1)
                    .build();

            playerState.gainCertificates(6);

            assertThat(playerState.getCertificates()).isEqualTo(6);
        }

        @Test
        void max4() {
            PlayerState playerState = PlayerState.builder()
                    .certificates(0)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .build();

            playerState.gainMaxCertificates();

            assertThat(playerState.getCertificates()).isEqualTo(4);
        }

        @Test
        void max6() {
            PlayerState playerState = PlayerState.builder()
                    .certificates(0)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .unlocked(Unlockable.CERT_LIMIT_6, 1)
                    .build();

            playerState.gainMaxCertificates();

            assertThat(playerState.getCertificates()).isEqualTo(6);
        }

        @Test
        void spend1() {
            PlayerState playerState = PlayerState.builder()
                    .certificates(6)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .unlocked(Unlockable.CERT_LIMIT_6, 1)
                    .build();

            playerState.spendCertificates(1);
            assertThat(playerState.getCertificates()).isEqualTo(4);

            playerState.spendCertificates(1);
            assertThat(playerState.getCertificates()).isEqualTo(3);

            playerState.spendCertificates(1);
            assertThat(playerState.getCertificates()).isEqualTo(2);

            playerState.spendCertificates(1);
            assertThat(playerState.getCertificates()).isEqualTo(1);

            playerState.spendCertificates(1);
            assertThat(playerState.getCertificates()).isEqualTo(0);

            assertThatThrownBy(() -> playerState.spendCertificates(1)).hasMessage("Not enough certificates");
        }

        @Test
        void spend2() {
            PlayerState playerState = PlayerState.builder()
                    .certificates(6)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .unlocked(Unlockable.CERT_LIMIT_6, 1)
                    .build();

            playerState.spendCertificates(2);
            assertThat(playerState.getCertificates()).isEqualTo(4);

            playerState.spendCertificates(2);
            assertThat(playerState.getCertificates()).isEqualTo(2);

            playerState.spendCertificates(2);
            assertThat(playerState.getCertificates()).isEqualTo(0);

            assertThatThrownBy(() -> playerState.spendCertificates(2)).hasMessage("Not enough certificates");
        }

        @Test
        void spend4() {
            PlayerState playerState = PlayerState.builder()
                    .certificates(6)
                    .unlocked(Unlockable.CERT_LIMIT_4, 1)
                    .unlocked(Unlockable.CERT_LIMIT_6, 1)
                    .build();

            playerState.spendCertificates(4);
            assertThat(playerState.getCertificates()).isEqualTo(2);

            assertThatThrownBy(() -> playerState.spendCertificates(4)).hasMessage("Not enough certificates");
        }
    }

    @Nested
    class Buildings {

        private PlayerState playerState;

        @BeforeEach
        void setUp() {
            playerState = new PlayerState(player, 6, new Random(0), PlayerBuilding.BuildingSet.beginner());
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

            assertThatThrownBy(() -> playerState.removeBuilding(building)).hasMessage("Building not available for player");
        }
    }
}
