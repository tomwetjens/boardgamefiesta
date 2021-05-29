package com.boardgamefiesta.istanbul.logic;

import com.boardgamefiesta.api.domain.Stats;
import com.boardgamefiesta.api.repository.JsonDeserializer;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.*;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerState {

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

    @Getter
    private final PlayerStats stats;

    @Builder
    PlayerState(int lira,
                int capacity,
                int rubies,
                @Singular @NonNull List<BonusCard> bonusCards,
                @Singular @NonNull Set<MosqueTile> mosqueTiles,
                @Singular @NonNull Map<GoodsType, Integer> goods,
                @NonNull PlayerStats stats) {
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
        this.stats = stats;
    }

    static PlayerState start(int playerIndex) {
        return new PlayerState(2 + playerIndex, 2, 0, Collections.emptyList(), Collections.emptySet(), Collections.emptyMap(), new PlayerStats());
    }

    void beginTurn() {
        stats.beginTurn();
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
        if (lira < amount) {
            throw new IstanbulException(IstanbulError.NOT_ENOUGH_LIRA);
        }
        lira -= amount;
    }

    void gainLira(int amount) {
        lira += amount;
        stats.gainLira(amount);
    }

    void addGoods(GoodsType goodsType, int amount) {
        var current = goods.getOrDefault(goodsType, 0);
        var updated = Math.min(capacity, current + amount);
        goods.put(goodsType, updated);
        stats.gainedGoods(goodsType, updated - current);
    }

    void addBonusCard(BonusCard bonusCard) {
        bonusCards.add(bonusCard);
    }

    void addExtension() {
        if (capacity == 5) {
            throw new IstanbulException(IstanbulError.CANNOT_ADD_EXTENSION);
        }
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

    void playBonusCard(BonusCard bonusCard) {
        if (!bonusCards.remove(bonusCard)) {
            throw new IstanbulException(IstanbulError.DOESNT_HAVE_BONUS_CARD);
        }
        stats.playedBonusCard(bonusCard);
    }

    void discardBonusCard(BonusCard bonusCard) {
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

    JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("bonusCards", serializer.fromStrings(bonusCards, BonusCard::name))
                .add("mosqueTiles", serializer.fromStrings(mosqueTiles, MosqueTile::name))
                .add("goods", serializer.fromIntegerMap(goods, GoodsType::name))
                .add("lira", lira)
                .add("capacity", capacity)
                .add("rubies", rubies)
                .add("stats", stats.serialize(factory, serializer))
                .build();
    }

    static PlayerState deserialize(JsonObject jsonObject) {
        return new PlayerState(
                jsonObject.getInt("lira"),
                jsonObject.getInt("capacity"),
                jsonObject.getInt("rubies"),
                jsonObject.getJsonArray("bonusCards").stream()
                        .map(jsonValue -> (JsonString) jsonValue)
                        .map(JsonString::getString)
                        .map(BonusCard::valueOf)
                        .collect(Collectors.toList()),
                jsonObject.getJsonArray("mosqueTiles").stream()
                        .map(jsonValue -> (JsonString) jsonValue)
                        .map(JsonString::getString)
                        .map(MosqueTile::valueOf)
                        .collect(Collectors.toSet()),
                JsonDeserializer.forObject(jsonObject.getJsonObject("goods")).asIntegerMap(GoodsType::valueOf),
                PlayerStats.deserialize(jsonObject.getJsonObject("stats"))
        );
    }

    public Stats stats() {
        var builder = Stats.builder()
                .value("turns", stats.turns)
                .value("rubies", rubies)
                .value("lira", lira)
                .value("capacity", capacity)
                .value("liraGained", stats.liraGained);

        for (var goodsType : GoodsType.values()) {
            builder
                    .value("goods." + goodsType.name(), goods.getOrDefault(GoodsType.FABRIC, 0))
                    .value("goodsGained." + goodsType.name(), stats.goodsGained.getOrDefault(GoodsType.FABRIC, 0));
        }

        builder
                .value("distanceMoved", stats.distanceMoved)
                .value("caughtFamilyMembers", stats.caughtFamilyMembers)
                .value("placedFamilyMembers", stats.placedFamilyMembers)
                .value("smugglerUses", stats.smugglerUses)
                .value("governorUses", stats.governorUses);

        for (var bonusCard : BonusCard.values()) {
            builder.value("playedBonusCards." + bonusCard.name(), stats.playedBonusCards.getOrDefault(bonusCard, 0));
        }

        builder.value("liraPaidToOtherMerchants", stats.liraPaidToOtherMerchants);

        return builder.build();
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class PlayerStats {

        @Getter
        private int turns;
        @Getter
        private final Map<BonusCard, Integer> playedBonusCards;
        @Getter
        private final Map<Integer, Integer> placeUses;
        @Getter
        private int liraGained;
        @Getter
        private int distanceMoved;
        @Getter
        private int assistantsLeft;
        @Getter
        private int assistantsPickedUp;
        @Getter
        private int caughtFamilyMembers;
        @Getter
        private int placedFamilyMembers;
        @Getter
        private int governorUses;
        @Getter
        private int smugglerUses;
        @Getter
        private final Map<GoodsType, Integer> goodsGained;
        @Getter
        private int liraPaidToOtherMerchants;

        public PlayerStats() {
            this.playedBonusCards = new HashMap<>();
            this.placeUses = new HashMap<>();
            this.goodsGained = new HashMap<>();
        }

        public static PlayerStats deserialize(JsonObject jsonObject) {
            return new PlayerStats(
                    jsonObject.getInt("turns"),
                    JsonDeserializer.forObject(jsonObject.getJsonObject("playedBonusCards")).asIntegerMap(BonusCard::valueOf),
                    JsonDeserializer.forObject(jsonObject.getJsonObject("placeUses")).asIntegerMap(Integer::valueOf),
                    jsonObject.getInt("liraGained"),
                    jsonObject.getInt("distanceMoved"),
                    jsonObject.getInt("assistantsLeft"),
                    jsonObject.getInt("assistantsPickedUp"),
                    jsonObject.getInt("caughtFamilyMembers"),
                    jsonObject.getInt("placedFamilyMembers"),
                    jsonObject.getInt("governorUses"),
                    jsonObject.getInt("smugglerUses"),
                    JsonDeserializer.forObject(jsonObject.getJsonObject("goodsGained")).asIntegerMap(GoodsType::valueOf),
                    jsonObject.getInt("liraPaidToOtherMerchants")
            );
        }

        JsonObjectBuilder serialize(JsonBuilderFactory factory, JsonSerializer serializer) {
            return factory.createObjectBuilder()
                    .add("turns", turns)
                    .add("playedBonusCards", serializer.fromIntegerMap(playedBonusCards, BonusCard::name))
                    .add("placeUses", serializer.fromIntegerMap(placeUses, Object::toString, Function.identity()))
                    .add("liraGained", liraGained)
                    .add("distanceMoved", distanceMoved)
                    .add("assistantsLeft", assistantsLeft)
                    .add("assistantsPickedUp", assistantsPickedUp)
                    .add("caughtFamilyMembers", caughtFamilyMembers)
                    .add("placedFamilyMembers", placedFamilyMembers)
                    .add("governorUses", governorUses)
                    .add("smugglerUses", smugglerUses)
                    .add("goodsGained", serializer.fromIntegerMap(goodsGained, GoodsType::name))
                    .add("liraPaidToOtherMerchants", liraPaidToOtherMerchants);
        }

        private void beginTurn() {
            turns++;
        }

        private void playedBonusCard(BonusCard bonusCard) {
            playedBonusCards.compute(bonusCard, (key, count) -> (count != null ? count : 0) + 1);
        }

        void placeUsed(Place place) {
            placeUses.compute(place.getNumber(), (key, count) -> (count != null ? count : 0) + 1);
        }

        private void gainLira(int amount) {
            liraGained += amount;
        }

        void movedMerchant(int dist) {
            distanceMoved += dist;
        }

        void leftAssistant() {
            assistantsLeft++;
        }

        void pickedUpAssistants(int numberOfAssistants) {
            assistantsPickedUp += numberOfAssistants;
        }

        void caughtFamilyMember() {
            caughtFamilyMembers++;
        }

        void placedFamilyMember() {
            placedFamilyMembers++;
        }

        void usedGovernor() {
            governorUses++;
        }

        void usedSmuggler() {
            smugglerUses++;
        }

        private void gainedGoods(GoodsType goodsType, int amount) {
            goodsGained.compute(goodsType, (key, count) -> (count != null ? count : 0) + amount);
        }

        void paidOtherMerchant(int amount) {
            liraPaidToOtherMerchants += amount;
        }

    }
}
