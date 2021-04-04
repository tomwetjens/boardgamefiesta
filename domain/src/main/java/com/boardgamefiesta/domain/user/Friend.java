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
