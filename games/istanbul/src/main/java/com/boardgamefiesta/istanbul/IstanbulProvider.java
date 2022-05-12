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

package com.boardgamefiesta.istanbul;

import com.boardgamefiesta.api.command.ActionMapper;
import com.boardgamefiesta.api.domain.InGameEventListener;
import com.boardgamefiesta.api.domain.Options;
import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.api.query.ViewMapper;
import com.boardgamefiesta.api.repository.StateDeserializer;
import com.boardgamefiesta.api.repository.StateSerializer;
import com.boardgamefiesta.api.spi.GameProvider;
import com.boardgamefiesta.istanbul.logic.Action;
import com.boardgamefiesta.istanbul.logic.Automa;
import com.boardgamefiesta.istanbul.logic.Istanbul;
import com.boardgamefiesta.istanbul.logic.LayoutType;
import com.boardgamefiesta.istanbul.view.ActionView;
import com.boardgamefiesta.istanbul.view.IstanbulView;

import javax.json.JsonObject;
import java.time.Duration;
import java.util.Random;
import java.util.Set;

public class IstanbulProvider implements GameProvider<Istanbul> {

    public static final String ID = "big-bazar";

    private static final Duration DEFAULT_TIME_LIMIT = Duration.ofMinutes(10);

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Set<PlayerColor> getSupportedColors() {
        return Istanbul.SUPPORTED_COLORS;
    }

    @Override
    public int getMinNumberOfPlayers() {
        return 2;
    }

    @Override
    public int getMaxNumberOfPlayers() {
        return 5;
    }

    @Override
    public Istanbul start(Set<Player> players, Options options, InGameEventListener eventListener, Random random) {
        var layoutType = options.getEnum("layoutType", LayoutType.class, LayoutType.RANDOM);
        return Istanbul.start(players, layoutType, eventListener, random);
    }

    @Override
    public void executeAutoma(Istanbul state, Player player, Random random) {
        new Automa().execute(state, player, random);
    }

    @Override
    public ViewMapper<Istanbul> getViewMapper() {
        return IstanbulView::new;
    }

    @Override
    public StateSerializer<Istanbul> getStateSerializer() {
        return Istanbul::serialize;
    }

    @Override
    public StateDeserializer<Istanbul> getStateDeserializer() {
        return Istanbul::deserialize;
    }

    @Override
    public boolean hasAutoma() {
        return true;
    }

    @Override
    public ActionMapper<Istanbul> getActionMapper() {
        return this::toAction;
    }

    private Action toAction(JsonObject jsonObject, Istanbul state) {
        var type = ActionView.valueOf(jsonObject.getString("type"));
        return type.toAction(jsonObject, state);
    }
}
