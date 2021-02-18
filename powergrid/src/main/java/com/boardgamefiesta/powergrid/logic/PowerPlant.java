package com.boardgamefiesta.powergrid.logic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.ImmutableSet;

import java.util.*;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum PowerPlant {

    P4(4, EnumSet.of(ResourceType.COAL), 2, 1),
    P8(8, EnumSet.of(ResourceType.COAL), 3, 2),
    P10(10, EnumSet.of(ResourceType.COAL), 2, 2),
    P15(15, EnumSet.of(ResourceType.COAL), 2, 3),
    P20(20, EnumSet.of(ResourceType.COAL), 3, 5),
    P25(25, EnumSet.of(ResourceType.COAL), 2, 5),
    P31(31, EnumSet.of(ResourceType.COAL), 3, 6),
    P36(36, EnumSet.of(ResourceType.COAL), 3, 7),
    P42(42, EnumSet.of(ResourceType.COAL), 2, 6),

    P6(6, EnumSet.of(ResourceType.BIO_MASS), 1, 1),
    P14(14, EnumSet.of(ResourceType.BIO_MASS), 2, 2),
    P19(19, EnumSet.of(ResourceType.BIO_MASS), 2, 3),
    P24(24, EnumSet.of(ResourceType.BIO_MASS), 2, 4),
    P30(30, EnumSet.of(ResourceType.BIO_MASS), 3, 6),
    P38(38, EnumSet.of(ResourceType.BIO_MASS), 3, 7),

    P11(11, EnumSet.of(ResourceType.URANIUM), 1, 2),
    P17(17, EnumSet.of(ResourceType.URANIUM), 1, 2),
    P23(23, EnumSet.of(ResourceType.URANIUM), 1, 3),
    P28(28, EnumSet.of(ResourceType.URANIUM), 1, 4),
    P34(34, EnumSet.of(ResourceType.URANIUM), 1, 5),
    P39(39, EnumSet.of(ResourceType.URANIUM), 1, 6),

    P3(3, EnumSet.of(ResourceType.OIL), 2, 1),
    P7(7, EnumSet.of(ResourceType.OIL), 3, 2),
    P9(9, EnumSet.of(ResourceType.OIL), 1, 1),
    P16(16, EnumSet.of(ResourceType.OIL), 2, 3),
    P26(26, EnumSet.of(ResourceType.OIL), 2, 5),
    P32(32, EnumSet.of(ResourceType.OIL), 3, 6),
    P35(35, EnumSet.of(ResourceType.OIL), 1, 5),
    P40(40, EnumSet.of(ResourceType.OIL), 2, 6),

    P5(5, EnumSet.of(ResourceType.COAL, ResourceType.OIL), 2, 1),
    P12(12, EnumSet.of(ResourceType.COAL, ResourceType.OIL), 2, 2),
    P21(21, EnumSet.of(ResourceType.COAL, ResourceType.OIL), 2, 4),
    P29(29, EnumSet.of(ResourceType.COAL, ResourceType.OIL), 1, 3),
    P96(96, EnumSet.of(ResourceType.COAL, ResourceType.OIL), 3, 7),

    P13(13, Collections.emptySet(), 0, 1),
    P18(18, Collections.emptySet(), 0, 2),
    P22(22, Collections.emptySet(), 0, 2),
    P27(27, Collections.emptySet(), 0, 3),
    P33(33, Collections.emptySet(), 0, 4),
    P37(37, Collections.emptySet(), 0, 4),
    P44(44, Collections.emptySet(), 0, 5),

    P50(50, Collections.emptySet(), 0, 6);

    @Getter
    private final int cost;
    @Getter
    private final Set<ResourceType> consumes;
    @Getter
    private final int requires;
    @Getter
    private final int powers;

    static PossibleProduce maximizePowered(ImmutableSet<PowerPlant> powerPlants, ImmutableMap<ResourceType, Integer> resources) {
        return possibleProduce(powerPlants, new Resources(resources))
                .max(Comparator.comparingInt(PossibleProduce::getPowered))
                .orElse(PossibleProduce.ZERO);
    }

    @Value
    static class Resources {

        static final Resources ZERO = new Resources(Maps.immutable.empty());

        ImmutableMap<ResourceType, Integer> map;

        Resources minus(Resources b) {
            return null;
        }
    }

    static Stream<PossibleProduce> possibleProduce(ImmutableSet<PowerPlant> powerPlants,Resources resources) {
        if (powerPlants.isEmpty()) {
            return Stream.empty();
        }

// TODO
        return null;
    }

    @Value
    static class PossibleProduce {

        static final PossibleProduce ZERO = new PossibleProduce(0, Resources.ZERO);

        int powered;
        Resources consumed;

        PossibleProduce add(PossibleProduce b) {
            return null;
        }
    }
}
