package com.boardgamefiesta.server.rest.game;

import com.boardgamefiesta.api.domain.Stats;
import com.boardgamefiesta.domain.game.Game;
import com.boardgamefiesta.domain.rating.Rating;
import com.boardgamefiesta.domain.rating.Ratings;
import com.boardgamefiesta.domain.table.Player;
import com.boardgamefiesta.domain.table.Table;
import com.boardgamefiesta.domain.table.Tables;
import com.boardgamefiesta.domain.user.User;
import com.boardgamefiesta.domain.user.Users;
import lombok.NonNull;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

// Disabled because it took the server down due to memory issues
//@Path("/stats/{gameId}")
@ApplicationScoped
public class StatsResource {

    private final Tables tables;
    private final Users users;
    private final Ratings ratings;

    @Inject
    public StatsResource(@NonNull Tables tables,
                         @NonNull Users users,
                         @NonNull Ratings ratings) {
        this.tables = tables;
        this.users = users;
        this.ratings = ratings;
    }

    @GET
    @Produces("text/csv")
    public Response get(@PathParam("gameId") String gameId, @QueryParam("from") Instant from) {
        var to = Instant.now();

        return Response.ok((StreamingOutput) outputStream -> {
            var userMap = new HashMap<User.Id, User>();

            try (PrintWriter writer = new PrintWriter(outputStream)) {
                List<String> keys = new ArrayList<>();

                tables.findEnded(Game.Id.of(gameId), 99999999, from, Tables.MAX_TIMESTAMP, false)
                        .filter(table -> table.getStatus() == Table.Status.ENDED)
                        .filter(table -> !table.hasComputerPlayers())
                        .forEach(table -> table.getPlayers().forEach(player -> {
                            table.stats(player).ifPresent(stats -> {
                                if (keys.isEmpty()) {
                                    keys.addAll(stats.keys());
                                    Collections.sort(keys);

                                    writeHeader(writer, keys);
                                }

                                var userId = player.getUserId().get();

                                var user = userMap.computeIfAbsent(userId, k -> users.findById(userId).orElse(null));
                                var rating = ratings.findByTable(userId, table.getId()).orElse(null);

                                writeRow(writer, keys, table, player, user, stats, rating);
                            });
                        }));
            }
        }).header("Content-Disposition", "attachment; filename=\""
                + gameId + "_"
                + from.toString().replace(":", "")
                + "_"
                + to.toString().replace(":", "")
                + ".csv\"").build();
    }

    private void writeHeader(PrintWriter writer, List<String> keys) {
        writer.print("tableId,started,ended,time,userId,username,score,winner,rating");
        keys.forEach(key -> {
            writer.print(',');
            writer.print(key);
        });
        writer.println();
    }

    private void writeRow(PrintWriter writer, List<String> keys, Table table, Player player, User user, Stats stats, Rating rating) {
        writer.print(table.getId().getId());
        writer.print(',');
        writer.print(table.getStarted().toString());
        writer.print(',');
        writer.print(table.getEnded().toString());
        writer.print(',');
        writer.print(Duration.between(table.getStarted(), table.getEnded()).toMinutes());
        writer.print(',');
        writer.print(user.getId().getId());
        writer.print(',');
        writer.print(user.getUsername());
        writer.print(',');
        writer.print(player.getScore().map(score -> Integer.toString(score)).orElse(""));
        writer.print(',');
        writer.print(player.getWinner().map(winner -> winner ? "Y" : "N").orElse(""));
        writer.print(',');
        writer.print(rating != null ? rating.getRating() : "");
        keys.forEach(key -> {
            writer.print(',');
            writer.print(stats.value(key).orElse(""));
        });
        writer.println();
    }

}
