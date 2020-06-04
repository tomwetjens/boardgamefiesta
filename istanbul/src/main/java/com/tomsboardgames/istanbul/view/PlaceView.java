package com.tomsboardgames.istanbul.view;

import com.tomsboardgames.api.Player;
import com.tomsboardgames.api.PlayerColor;
import com.tomsboardgames.istanbul.logic.Place;
import lombok.Getter;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class PlaceView {

    private final int number;
    private final List<MerchantView> merchants;
    private final List<PlayerColor> familyMembers;
    private final Map<PlayerColor, Integer> assistants;
    private final Boolean governor;
    private final Boolean smuggler;

    PlaceView(Place place) {
        this.number = place.getNumber();

        this.merchants = nullIfEmpty(place.getMerchants().stream()
                .map(MerchantView::new)
                .sorted(Comparator.comparing(MerchantView::getColor))
                .collect(Collectors.toList()));
        this.assistants = nullIfEmpty(place.getAssistants().entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        this.familyMembers = nullIfEmpty(place.getFamilyMembers().stream()
                .map(Player::getColor)
                .sorted()
                .collect(Collectors.toList()));
        this.governor = nullIfFalse(place.isGovernor());
        this.smuggler = nullIfFalse(place.isSmuggler());
    }

    static PlaceView of(Place place) {
        if (place instanceof Place.Caravansary) {
            return new CaravansaryView((Place.Caravansary) place);
        } else if (place instanceof Place.GemstoneDealer) {
            return new GemstoneDealerView((Place.GemstoneDealer) place);
        } else if (place instanceof Place.GreatMosque) {
            return new MosqueView((Place.GreatMosque) place);
        } else if (place instanceof Place.LargeMarket) {
            return new MarketView((Place.LargeMarket) place);
        } else if (place instanceof Place.PostOffice) {
            return new PostOfficeView((Place.PostOffice) place);
        } else if (place instanceof Place.SmallMarket) {
            return new MarketView((Place.SmallMarket) place);
        } else if (place instanceof Place.SmallMosque) {
            return new MosqueView((Place.SmallMosque) place);
        } else if (place instanceof Place.SultansPalace) {
            return new SultansPalaceView((Place.SultansPalace) place);
        } else {
            return new PlaceView(place);
        }
    }

    static Boolean nullIfFalse(boolean b) {
        return b ? true : null;
    }

    static <T, C extends Collection<T>> C nullIfEmpty(C collection) {
        return collection.isEmpty() ? null : collection;
    }

    static <K, V, M extends Map<K, V>> M nullIfEmpty(M map) {
        return map.isEmpty() ? null : map;
    }
}
