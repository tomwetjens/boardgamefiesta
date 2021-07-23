package com.boardgamefiesta.istanbul.view;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.istanbul.logic.BonusCard;
import com.boardgamefiesta.istanbul.logic.GoodsType;
import com.boardgamefiesta.istanbul.logic.MosqueTile;
import com.boardgamefiesta.istanbul.logic.PlayerState;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
public class PlayerStateView {

    private final String name;
    private final boolean startPlayer;
    private final int rubies;
    private final int lira;
    private final int capacity;
    private final Map<GoodsType, Integer> goods;
    private final List<MosqueTile> mosqueTiles;
    private final int numberOfBonusCards;

    private List<BonusCard> bonusCards;

    private PlayerStatsView stats;

    public PlayerStateView(Player player, PlayerState playerState, boolean startPlayer, boolean self, boolean ended) {
        this.name = player.getName();
        this.startPlayer = startPlayer;
        this.rubies = playerState.getRubies();
        this.lira = playerState.getLira();
        this.capacity = playerState.getCapacity();
        this.goods = playerState.getGoods();

        this.mosqueTiles = new ArrayList<>(playerState.getMosqueTiles());
        Collections.sort(this.mosqueTiles);

        this.numberOfBonusCards = playerState.getBonusCards().size();
        if (self || ended) {
            this.bonusCards = playerState.getBonusCards();
            Collections.sort(this.bonusCards);
        }

        if (ended) {
            this.stats = new PlayerStatsView(playerState.getStats());
        }
    }

    @Getter
    public static final class PlayerStatsView {

        private final int turns;
        private final int playedBonusCards;
        private final Map<Integer, Integer> placeUses;
        private final int liraGained;
        private final int distanceMoved;
        private final int assistantsLeft;
        private final int assistantsPickedUp;
        private final int caughtFamilyMembers;
        private final int placedFamilyMembers;
        private final int governorUses;
        private final int smugglerUses;
        private final Map<GoodsType, Integer> goodsGained;
        private final int liraPaidToOtherMerchants;

        public PlayerStatsView(PlayerState.PlayerStats playerStats) {
            this.turns = playerStats.getTurns();
            this.playedBonusCards = playerStats.getPlayedBonusCards().values().stream().mapToInt(Integer::intValue).sum();
            this.placeUses = playerStats.getPlaceUses();
            this.liraGained = playerStats.getLiraGained();
            this.distanceMoved = playerStats.getDistanceMoved();
            this.assistantsLeft = playerStats.getAssistantsLeft();
            this.assistantsPickedUp = playerStats.getAssistantsPickedUp();
            this.caughtFamilyMembers = playerStats.getCaughtFamilyMembers();
            this.placedFamilyMembers = playerStats.getPlacedFamilyMembers();
            this.governorUses = playerStats.getGovernorUses();
            this.smugglerUses = playerStats.getSmugglerUses();
            this.goodsGained = playerStats.getGoodsGained();
            this.liraPaidToOtherMerchants = playerStats.getLiraPaidToOtherMerchants();
        }
    }
}
