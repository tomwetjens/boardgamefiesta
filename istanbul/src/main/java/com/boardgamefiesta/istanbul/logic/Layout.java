package com.boardgamefiesta.istanbul.logic;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.repository.JsonSerializer;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Layout {

    private static final int WIDTH = 4;
    private static final int HEIGHT = 4;

    private final Place[][] layout;

    Layout(@NonNull Place... places) {
        this(Arrays.asList(places));
    }

    Layout(@NonNull List<Place> places) {
        if (places.size() != WIDTH * HEIGHT) {
            throw new IllegalArgumentException("Layout must contain " + (WIDTH * HEIGHT) + " places");
        }

        var iterator = places.iterator();

        this.layout = new Place[WIDTH][HEIGHT];

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                this.layout[x][y] = iterator.next();
            }
        }
    }

    Place randomPlace(@NonNull Random random) {
        var x = random.nextInt(layout.length);
        var y = random.nextInt(layout[x].length);
        return layout[x][y];
    }

    public Place place(int x, int y) {
        return layout[x][y];
    }

    public int width() {
        return WIDTH;
    }

    public int height() {
        return HEIGHT;
    }

    Place place(Predicate<Place> predicate) {
        for (Place[] places : layout) {
            for (Place place : places) {
                if (predicate.test(place)) {
                    return place;
                }
            }
        }
        throw new IllegalArgumentException("Place not found");
    }

    int distance(Place from, Place to) {
        for (int x1 = 0; x1 < WIDTH; x1++) {
            for (int y1 = 0; y1 < HEIGHT; y1++) {
                if (layout[x1][y1] == from) {
                    for (int x2 = 0; x2 < WIDTH; x2++) {
                        for (int y2 = 0; y2 < HEIGHT; y2++) {
                            if (layout[x2][y2] == to) {
                                return Math.abs(x1 - x2) + Math.abs(y1 - y2);
                            }
                        }
                    }
                }
            }
        }
        throw new IllegalArgumentException("Place not found");
    }

    Place currentPlaceOfMerchant(PlayerColor playerColor) {
        for (Place[] places : layout) {
            for (Place place : places) {
                if (place.getMerchants().stream().anyMatch(merchant -> merchant.getColor() == playerColor)) {
                    return place;
                }
            }
        }
        throw new IstanbulException(IstanbulError.NOT_AT_PLACE);
    }

    Place currentPlaceOfFamilyMember(Player player) {
        for (Place[] places : layout) {
            for (Place place : places) {
                if (place.getFamilyMembers().contains(player)) {
                    return place;
                }
            }
        }
        throw new IstanbulException(IstanbulError.NOT_AT_PLACE);
    }

    private <T extends Place> T place(Class<T> clazz) {
        for (Place[] places : layout) {
            for (Place place : places) {
                if (clazz == place.getClass()) {
                    return clazz.cast(place);
                }
            }
        }
        throw new IllegalArgumentException("Place not found: " + clazz);
    }

    public Place.GreatMosque getGreatMosque() {
        return place(Place.GreatMosque.class);
    }

    public Place.SmallMosque getSmallMosque() {
        return place(Place.SmallMosque.class);
    }

    public Place.LargeMarket getLargeMarket() {
        return place(Place.LargeMarket.class);
    }

    public Place.SmallMarket getSmallMarket() {
        return place(Place.SmallMarket.class);
    }

    public Place.SultansPalace getSultansPalace() {
        return place(Place.SultansPalace.class);
    }

    public Place.GemstoneDealer getGemstoneDealer() {
        return place(Place.GemstoneDealer.class);
    }

    public Place.TeaHouse getTeaHouse() {
        return place(Place.TeaHouse.class);
    }

    public Place.BlackMarket getBlackMarket() {
        return place(Place.BlackMarket.class);
    }

    public Place.Fountain getFountain() {
        return place(Place.Fountain.class);
    }

    public Place.Caravansary getCaravansary() {
        return place(Place.Caravansary.class);
    }

    public Place.PostOffice getPostOffice() {
        return place(Place.PostOffice.class);
    }

    public Place.Wainwright getWainwright() {
        return place(Place.Wainwright.class);
    }

    public Place.SpiceWarehouse getSpiceWarehouse() {
        return place(Place.SpiceWarehouse.class);
    }

    public Place.FruitWarehouse getFruitWarehouse() {
        return place(Place.FruitWarehouse.class);
    }

    public Place.FabricWarehouse getFabricWarehouse() {
        return place(Place.FabricWarehouse.class);
    }

    public Place.PoliceStation getPoliceStation() {
        return place(Place.PoliceStation.class);
    }

    public Set<Place> getPlaces() {
        return Arrays.stream(layout).flatMap(Arrays::stream).collect(Collectors.toSet());
    }

    JsonObject serialize(JsonBuilderFactory factory) {
        var serializer = JsonSerializer.forFactory(factory);
        return factory.createObjectBuilder()
                .add("layout", serializer.fromStream(Arrays.stream(layout), column ->
                        serializer.fromStream(Arrays.stream(column), place -> place.serialize(factory).build())))
                .build();
    }

    static Layout deserialize(Map<String, Player> playerMap, JsonObject jsonObject) {
        return new Layout(jsonObject.getJsonArray("layout").stream()
                .map(JsonValue::asJsonArray)
                .map(column -> column.stream()
                        .map(JsonValue::asJsonObject)
                        .map(place -> Place.deserialize(playerMap, place))
                        .toArray(Place[]::new))
                .toArray(Place[][]::new));
    }
}
