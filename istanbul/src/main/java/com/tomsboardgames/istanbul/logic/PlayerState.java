package com.tomsboardgames.istanbul.logic;

import lombok.Getter;

import java.io.Serializable;
import java.util.*;

public class PlayerState implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter
    private final List<BonusCard> bonusCards = new LinkedList<>();
    @Getter
    private final Set<MosqueTile> mosqueTiles = new HashSet<>();
    @Getter
    private final Map<GoodsType, Integer> goods = new HashMap<>();

    @Getter
    private final Merchant merchant;

    @Getter
    private int lira;

    @Getter
    private int capacity = 2;

    @Getter
    private int rubies;

    PlayerState(int lira, Merchant merchant) {
        this.lira = lira;
        this.merchant = merchant;
    }

    boolean hasMosqueTile(MosqueTile mosqueTile) {
        return mosqueTiles.contains(mosqueTile);
    }

    boolean hasAtLeastGoods(GoodsType goodsType, Integer amount) {
        return goods.getOrDefault(goodsType, 0) >= amount;
    }

    int removeGoods(GoodsType goodsType, int amount) {
        var current = goods.getOrDefault(goodsType, 0);

        if (current < amount) {
            throw new IstanbulException(IstanbulError.NOT_ENOUGH_GOODS);
        }

        goods.put(goodsType, current - amount);

        return amount;
    }

    void addMosqueTile(MosqueTile mosqueTile) {
        if (!mosqueTiles.add(mosqueTile)) {
            throw new IstanbulException(IstanbulError.ALREADY_HAS_MOSQUE_TILE);
        }
    }

    void maxGoods(GoodsType goodsType) {
        goods.put(goodsType, capacity);
    }

    void payLira(int amount) {
        if (this.lira < amount) {
            throw new IstanbulException(IstanbulError.NOT_ENOUGH_LIRA);
        }
        this.lira -= amount;
    }

    void gainLira(int amount) {
        this.lira += amount;
    }

    void addGoods(GoodsType goodsType, int amount) {
        var current = goods.getOrDefault(goodsType, 0);

        goods.put(goodsType, Math.min(capacity, current + amount));
    }

    void addBonusCard(BonusCard bonusCard) {
        bonusCards.add(bonusCard);
    }

    void addExtension() {
        capacity++;
        if (capacity == 5) {
            rubies++;
        }
    }

    void gainRuby() {
        rubies++;
    }

    boolean hasBonusCard(BonusCard bonusCard) {
        return bonusCards.contains(bonusCard);
    }

    void removeBonusCard(BonusCard bonusCard) {
        if (!bonusCards.remove(bonusCard)) {
            throw new IstanbulException(IstanbulError.DOESNT_HAVE_BONUS_CARD);
        }
    }

    int getTotalGoods() {
        return goods.values().stream().mapToInt(Integer::intValue).sum();
    }
}
