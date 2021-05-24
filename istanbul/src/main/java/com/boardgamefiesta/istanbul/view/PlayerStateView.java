package com.boardgamefiesta.istanbul.view;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.Stats;
import com.boardgamefiesta.istanbul.logic.BonusCard;
import com.boardgamefiesta.istanbul.logic.GoodsType;
import com.boardgamefiesta.istanbul.logic.MosqueTile;
import com.boardgamefiesta.istanbul.logic.PlayerState;
import lombok.Getter;

import java.util.*;

@Getter
public class PlayerStateView {

    private final String name;
    private final int rubies;
    private final int lira;
    private final int capacity;
    private final Map<GoodsType, Integer> goods;
    private final List<MosqueTile> mosqueTiles;
    private final int numberOfBonusCards;

    private List<BonusCard> bonusCards;

    public PlayerStateView(Player player, PlayerState playerState, boolean self) {
        this.name = player.getName();
        this.rubies = playerState.getRubies();
        this.lira = playerState.getLira();
        this.capacity = playerState.getCapacity();
        this.goods = playerState.getGoods();

        this.mosqueTiles = new ArrayList<>(playerState.getMosqueTiles());
        Collections.sort(this.mosqueTiles);

        this.numberOfBonusCards = playerState.getBonusCards().size();
        if (self) {
            this.bonusCards = playerState.getBonusCards();
            Collections.sort(this.bonusCards);
        }
    }
}
