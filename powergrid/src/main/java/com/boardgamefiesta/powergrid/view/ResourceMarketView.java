package com.boardgamefiesta.powergrid.view;

import com.boardgamefiesta.powergrid.logic.ResourceMarket;
import com.boardgamefiesta.powergrid.logic.ResourceType;
import lombok.Value;

import java.util.Map;

@Value
public class ResourceMarketView {

    Map<ResourceType, Integer> available;

    public ResourceMarketView(ResourceMarket resourceMarket) {
        available = Map.of(
                ResourceType.COAL, resourceMarket.getAvailable(ResourceType.COAL),
                ResourceType.OIL, resourceMarket.getAvailable(ResourceType.OIL),
                ResourceType.BIO_MASS, resourceMarket.getAvailable(ResourceType.BIO_MASS),
                ResourceType.URANIUM, resourceMarket.getAvailable(ResourceType.URANIUM)
        );
    }
}
