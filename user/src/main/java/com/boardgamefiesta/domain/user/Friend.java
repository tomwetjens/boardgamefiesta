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

package com.boardgamefiesta.domain.user;

import com.boardgamefiesta.domain.AggregateRoot;
import com.boardgamefiesta.domain.DomainEvent;
import lombok.*;

import java.time.Instant;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Friend implements AggregateRoot {

    @Getter
    @NonNull
    private final Id id;

    @Getter
    @NonNull
    private final Instant started;

    @Getter
    private Instant ended;

    public static Friend start(User.Id userId, User.Id otherUserId) {
        if (userId.equals(otherUserId)) {
            throw new IllegalArgumentException("User ids must be different");
        }

        var friend = new Friend(Id.of(userId, otherUserId), Instant.now(), null);

        new Started(friend.getId()).fire();

        return friend;
    }

    public void end() {
        ended = Instant.now();
    }

    public boolean isEnded() {
        return ended != null;
    }

    @Value(staticConstructor = "of")
    public static class Id {
        User.Id userId;
        User.Id otherUserId;
    }

    @Value
    public static class Started implements DomainEvent {
        @NonNull Friend.Id id;
    }

}
