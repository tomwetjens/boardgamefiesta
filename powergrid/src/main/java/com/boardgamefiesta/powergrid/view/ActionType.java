package com.boardgamefiesta.powergrid.view;

import com.boardgamefiesta.powergrid.logic.Action;
import com.boardgamefiesta.powergrid.logic.PowerGrid;
import com.boardgamefiesta.powergrid.logic.PowerPlant;
import com.boardgamefiesta.powergrid.logic.ResourceType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.json.JsonException;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum ActionType {

    START_AUCTION(Action.StartAuction.class),
    PLACE_BID(Action.PlaceBid.class),
    REMOVE_POWER_PLANT(Action.RemovePowerPlant.class),
    BUY_RESOURCE(Action.BuyResource.class),
    CONNECT_CITY(Action.ConnectCity.class),
    PRODUCE_POWER(Action.ProducePower.class);

    private final Class<? extends Action> actionClass;

    public static Action toAction(JsonObject jsonObject, PowerGrid powerGrid) {
        var actionType = ActionType.valueOf(jsonObject.getString("type"));

        switch (actionType) {
            case START_AUCTION:
                return new Action.StartAuction(PowerPlant.valueOf(jsonObject.getString("powerPlant")));
            case PLACE_BID:
                return new Action.PlaceBid(jsonObject.getInt("bid"));
            case REMOVE_POWER_PLANT:
                return new Action.RemovePowerPlant(PowerPlant.valueOf(jsonObject.getString("powerPlant")));
            case BUY_RESOURCE:
                return new Action.BuyResource(ResourceType.valueOf(jsonObject.getString("resourceType")), jsonObject.getInt("amount"));
            case CONNECT_CITY:
                return new Action.ConnectCity(powerGrid.getMap().getCity(jsonObject.getString("city")));
            case PRODUCE_POWER:
                var resources = jsonObject.getJsonObject("resources");
                return new Action.ProducePower(resources.keySet().stream()
                        .collect(Collectors.toMap(ResourceType::valueOf, resources::getInt)));
            default:
                throw new JsonException("Unsupported action type: " + actionType);
        }
    }

    public static ActionType from(Class<? extends Action> actionClass) {
        return Arrays.stream(values())
                .filter(actionType -> actionType.actionClass == actionClass)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported action: " + actionClass));
    }
}
