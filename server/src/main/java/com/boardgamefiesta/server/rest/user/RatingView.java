package com.boardgamefiesta.server.rest.user;

import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.rating.Rating;
import com.boardgamefiesta.server.rest.user.view.UserView;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
public class RatingView {

    String userId;
    String gameId;
    String tableId;
    Instant timestamp;
    float rating;

    List<DeltaView> deltas;

    RatingView(@NonNull Rating rating, Map<User.Id, User> userMap) {
        this.userId = rating.getUserId().getId();
        this.gameId = rating.getGameId().getId();
        this.tableId = rating.getTableId().map(Table.Id::getId).orElse(null);
        this.timestamp = rating.getTimestamp();
        this.rating = rating.getRating();

        if (userMap != null) {
            this.deltas = rating.getDeltas().entrySet().stream()
                    .flatMap(entry -> Optional.ofNullable(userMap.get(entry.getKey()))
                            .map(user -> new DeltaView(user, entry.getValue()))
                            .stream())
                    .collect(Collectors.toList());
        } else {
            this.deltas = null;
        }
    }

    @Value
    public static class DeltaView {
        UserView user;
        float delta;

        private DeltaView(User user, float delta) {
            this.user = new UserView(user.getId(), user, null);
            this.delta = delta;
        }
    }
}
