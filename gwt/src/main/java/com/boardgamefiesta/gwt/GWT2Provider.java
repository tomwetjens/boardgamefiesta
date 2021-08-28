/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.gwt;

import com.boardgamefiesta.api.command.ActionMapper;
import com.boardgamefiesta.api.domain.EventListener;
import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.query.ViewMapper;
import com.boardgamefiesta.api.repository.StateDeserializer;
import com.boardgamefiesta.api.repository.StateSerializer;
import com.boardgamefiesta.api.spi.GameProvider;
import com.boardgamefiesta.gwt.logic.Automa;
import com.boardgamefiesta.gwt.logic.GWT;
import com.boardgamefiesta.gwt.logic.Garth;
import com.boardgamefiesta.gwt.view.ActionType;
import com.boardgamefiesta.gwt.view.StateView;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

public class GWT2Provider implements GameProvider<GWT> {

    public static final String ID = "gwt2";

    private static final Duration DEFAULT_TIME_LIMIT = Duration.of(10, ChronoUnit.MINUTES);

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Set<PlayerColor> getSupportedColors() {
        return EnumSet.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.GREEN);
    }

    @Override
    public int getMinNumberOfPlayers() {
        return 2;
    }

    @Override
    public int getMaxNumberOfPlayers() {
        return 4;
    }

    @Override
    public GWT start(Set<Player> players, Options options, EventListener eventListener, Random random) {
        return GWT.start(GWT.Edition.SECOND, players, GWT.Options.builder()
                .mode(options.getEnum("mode", GWT.Options.Mode.class, GWT.Options.Mode.ORIGINAL))
                .buildings(options.getEnum("buildings", GWT.Options.Buildings.class, GWT.Options.Buildings.RANDOMIZED))
                .playerOrder(options.getEnum("playerOrder", GWT.Options.PlayerOrder.class, GWT.Options.PlayerOrder.RANDOMIZED))
                .variant(options.getEnum("variant", GWT.Options.Variant.class, GWT.Options.Variant.ORIGINAL))
                .railsToTheNorth(options.getBoolean("railsToTheNorth", false))
                .simmental(options.getBoolean("simmental", true))
                .difficulty(options.getEnum("difficulty", Garth.Difficulty.class, Garth.Difficulty.EASY))
                .build(), eventListener, random);
    }

    @Override
    public void executeAutoma(GWT state, Player player, Random random) {
        var automaState = state.playerState(player).getAutomaState().orElseThrow();
        if (automaState != null) {
            automaState.execute(state, random);
        } else {
            // For backwards compatibility
            new Automa().execute(state, player, random);
        }
    }

    @Override
    public ActionMapper<GWT> getActionMapper() {
        return ActionType::toAction;
    }

    @Override
    public ViewMapper<GWT> getViewMapper() {
        return StateView::new;
    }

    @Override
    public StateSerializer<GWT> getStateSerializer() {
        return GWT::serialize;
    }

    @Override
    public StateDeserializer<GWT> getStateDeserializer() {
        return GWT::deserialize;
    }

    @Override
    public boolean hasAutoma() {
        return true;
    }

    @Override
    public Duration getTimeLimit(Options options) {
        return DEFAULT_TIME_LIMIT;
    }
}
