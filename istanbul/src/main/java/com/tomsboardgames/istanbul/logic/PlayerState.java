package com.tomsboardgames.istanbul.logic;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

import java.io.Serializable;
import java.util.*;

public class PlayerState implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter
    private final List<BonusCard> bonusCards;
    @Getter
    private final Set<MosqueTile> mosqueTiles;
    @Getter
    private final Map<GoodsType, Integer> goods;

    @Getter
    private int lira;

    @Getter
    private int capacity;

    @Getter
    private int rubies;

    @Builder
    PlayerState(int lira,
                int capacity,
                int rubies,
                @Singular @NonNull List<BonusCard> bonusCards,
                @Singular @NonNull Set<MosqueTile> mosqueTiles,
                @Singular @NonNull Map<GoodsType, Integer> goods) {
        if (lira < 0) {
            throw new IllegalArgumentException("Lira must be >= 0");
        }

        if (capacity < 2 || capacity > 5) {
            throw new IllegalArgumentException("Capacity must be between 2 and 5");
        }

        if (rubies < 0 || rubies > 6) {
            throw new IllegalArgumentException("Rubies must be between 0 and 6");
        }

        if (goods.values().stream().anyMatch(amount -> amount < 0 || amount > capacity)) {
            throw new IllegalArgumentException("Goods must be >= 0 and must not exceed capacity");
        }

        this.lira = lira;
        this.capacity = capacity;
        this.rubies = rubies;
        this.bonusCards = new LinkedList<>(bonusCards);
        this.mosqueTiles = new HashSet<>(mosqueTiles);
        this.goods = new HashMap<>(goods);
    }

    static PlayerState start(int playerIndex) {
        return new PlayerState(2 + playerIndex, 2, 0, Collections.emptyList(), Collections.emptySet(), Collections.emptyMap());
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

    void gainRubies(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >=0");
        }
        rubies += amount;
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

    boolean hasMaxRubies(int playerCount) {
        return rubies >= maxRubies(playerCount);
    }

    public static int maxRubies(int playerCount) {
        return playerCount > 2 ? 5 : 6;
    }
}
