package com.boardgamefiesta.istanbul.view;

import com.boardgamefiesta.istanbul.logic.BonusCard;
import com.boardgamefiesta.istanbul.logic.GoodsType;
import com.boardgamefiesta.istanbul.logic.MosqueTile;
import com.boardgamefiesta.istanbul.logic.PlayerState;
import lombok.Getter;

import java.util.*;

@Getter
public class PlayerStateView {

    private final int rubies;
    private final int lira;
    private final int capacity;
    private final Map<GoodsType, Integer> goods;
    private final List<MosqueTile> mosqueTiles;

    private List<BonusCard> bonusCards;

    public PlayerStateView(PlayerState playerState, boolean self) {
        this.rubies = playerState.getRubies();
        this.lira = playerState.getLira();
        this.capacity = playerState.getCapacity();
        this.goods = playerState.getGoods();

        this.mosqueTiles = new ArrayList<>(playerState.getMosqueTiles());
        Collections.sort(this.mosqueTiles);

        if (self) {
            this.bonusCards = playerState.getBonusCards();
            Collections.sort(this.bonusCards);
        }
    }
}
