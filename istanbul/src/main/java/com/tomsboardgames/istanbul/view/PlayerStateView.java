package com.tomsboardgames.istanbul.view;

import com.tomsboardgames.istanbul.logic.BonusCard;
import com.tomsboardgames.istanbul.logic.GoodsType;
import com.tomsboardgames.istanbul.logic.MosqueTile;
import com.tomsboardgames.istanbul.logic.PlayerState;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class PlayerStateView {

    private final int rubies;
    private final int lira;
    private final int capacity;
    private final Map<GoodsType, Integer> goods;
    private final Set<MosqueTile> mosqueTiles;

    private List<BonusCard> bonusCards;

    public PlayerStateView(PlayerState playerState, boolean self) {
        this.rubies = playerState.getRubies();
        this.lira = playerState.getLira();
        this.capacity = playerState.getCapacity();
        this.goods = playerState.getGoods();
        this.mosqueTiles = playerState.getMosqueTiles();

        if (self) {
            this.bonusCards = playerState.getBonusCards();
        }
    }
}
