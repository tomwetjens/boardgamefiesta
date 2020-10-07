package com.boardgamefiesta.server.domain.user;

import com.boardgamefiesta.server.domain.DomainEvent;
import lombok.*;

import java.time.Instant;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Friend {

    @Getter
    @NonNull
    private final Id id;

    @Getter
    @NonNull
    private final Instant started;

    @Getter
    private Instant ended;

    public static Friend start(User user, User otherUser) {
        if (user.getId().equals(otherUser.getId())) {
            throw new IllegalArgumentException("User ids must be different");
        }

        var friend = new Friend(Id.of(user.getId(), otherUser.getId()), Instant.now(), null);

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
