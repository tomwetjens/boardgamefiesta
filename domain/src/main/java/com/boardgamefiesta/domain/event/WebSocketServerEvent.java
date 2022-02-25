/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
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

package com.boardgamefiesta.domain.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketServerEvent {

    private Type type;
    private String tableId;
    private String userId;

    public enum Type {
        STARTED,
        ENDED,
        INVITED,
        ACCEPTED,
        REJECTED,
        STATE_CHANGED,
        UNINVITED,
        LEFT,
        PROPOSED_TO_LEAVE,
        AGREED_TO_LEAVE,
        KICKED,
        OPTIONS_CHANGED,
        COMPUTER_ADDED,
        ABANDONED,
        JOINED,
        VISIBILITY_CHANGED,
        ADDED_AS_FRIEND
    }
}
