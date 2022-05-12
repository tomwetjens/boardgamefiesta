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

package com.boardgamefiesta.istanbul.view;

import com.boardgamefiesta.api.domain.Player;
import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.istanbul.logic.Istanbul;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
public class IstanbulView {

    private final List<List<PlaceView>> layout;
    private final List<ActionView> actions;
    private final Map<PlayerColor, PlayerStateView> players;
    /**
     * Other players, in player order, relative to the viewer.
     */
    private final List<PlayerColor> otherPlayers;
    private final int bonusCards;
    private final int maxRubies;

    public IstanbulView(Istanbul state, Player viewer) {
        layout = IntStream.range(0, state.getLayout().width())
                .mapToObj(x -> IntStream.range(0, state.getLayout().height())
                        .mapToObj(y -> state.getLayout().place(x, y))
                        .map(PlaceView::of)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        actions = state.getCurrentPlayer() == viewer
                ? state.getPossibleActions().stream()
                .map(ActionView::of)
                .sorted()
                .collect(Collectors.toList())
                : null;

        var startPlayer = state.getPlayerOrder().get(0);
        players = state.getPlayers().stream().collect(Collectors.toMap(Player::getColor, player ->
                new PlayerStateView(player, state.getPlayerState(player),
                        player.equals(startPlayer),
                        player.equals(viewer), state.isEnded())));

        if (viewer != null) {
            // Other players in play order
            var viewingPlayerIndex = state.getPlayers().indexOf(viewer);
            var playerCount = state.getPlayers().size();
            otherPlayers =
                    // In order relative to the viewing player
                    IntStream.range(1, playerCount)
                            .map(i -> (viewingPlayerIndex + i) % playerCount)
                            .mapToObj(i -> state.getPlayers().get(i))
                            .map(Player::getColor)
                            .collect(Collectors.toList());
        } else {
            otherPlayers = state.getPlayers().stream()
                    .map(Player::getColor)
                    .collect(Collectors.toList());
        }

        bonusCards = state.getBonusCardsSize();

        maxRubies = state.getMaxRubies();
    }

}
