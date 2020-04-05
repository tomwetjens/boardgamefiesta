package com.wetjens.gwt.server.rest.view.state;

import com.wetjens.gwt.Foresights;
import com.wetjens.gwt.KansasCitySupply;
import com.wetjens.gwt.Teepee;
import com.wetjens.gwt.Worker;
import lombok.Getter;
import lombok.Value;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class ForesightsView {

    List<Set<TileView>> choices;

    ForesightsView(Foresights foresights) {
        choices = Arrays.asList(
                foresights.choices(0).stream().map(TileView::new).collect(Collectors.toSet()),
                foresights.choices(1).stream().map(TileView::new).collect(Collectors.toSet()),
                foresights.choices(2).stream().map(TileView::new).collect(Collectors.toSet())
        );
    }

    @Getter
    public static class TileView {

        Type type;

        Worker worker;
        HazardView hazard;
        Teepee teepee;

        TileView(KansasCitySupply.Tile tile) {
            if (tile.getWorker() != null) {
                type = Type.WORKER;
                worker = tile.getWorker();
            } else if (tile.getHazard() != null) {
                type = Type.HAZARD;
                hazard = new HazardView(tile.getHazard());
            } else {
                type = Type.TEEPEE;
                teepee = tile.getTeepee();
            }
        }

        public enum Type {
            WORKER,
            HAZARD,
            TEEPEE;
        }
    }
}
