package com.wetjens.gwt.view;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.wetjens.gwt.Foresights;
import com.wetjens.gwt.KansasCitySupply;
import com.wetjens.gwt.Teepee;
import com.wetjens.gwt.Worker;
import lombok.Getter;
import lombok.Value;

@Value
public class ForesightsView {

    List<List<TileView>> choices;

    ForesightsView(Foresights foresights) {
        choices = Arrays.asList(
                foresights.choices(0).stream().map(TileView::new).collect(Collectors.toList()),
                foresights.choices(1).stream().map(TileView::new).collect(Collectors.toList()),
                foresights.choices(2).stream().map(TileView::new).collect(Collectors.toList())
        );
    }

    @Getter
    public static class TileView {

        Worker worker;
        HazardView hazard;
        Teepee teepee;

        TileView(KansasCitySupply.Tile tile) {
            if (tile.getWorker() != null) {
                worker = tile.getWorker();
            } else if (tile.getHazard() != null) {
                hazard = new HazardView(tile.getHazard());
            } else {
                teepee = tile.getTeepee();
            }
        }
    }
}
