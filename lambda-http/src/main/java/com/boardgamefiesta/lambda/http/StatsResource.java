package com.boardgamefiesta.lambda.http;

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
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Path("/stats/{gameId}")
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
    public Response get(@PathParam("gameId") Game.Id gameId, @QueryParam("from") Instant from) {
        if (from == null) {
            throw new BadRequestException("'from' parameter missing");
        }

        var to = Instant.now();

        if (Duration.between(from, to).toDays() > 7) {
            throw new BadRequestException("You may not request data from more than 7 days ago");
        }

        var fileName = gameId.getId() + "_"
                + from.toString().replace(":", "")
                + "_"
                + to.toString().replace(":", "")
                + ".csv";

        return Response
                .ok((StreamingOutput) outputStream -> generateCsv(gameId, from, to, outputStream))
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .build();
    }

    private void generateCsv(Game.Id gameId, Instant from, Instant to, OutputStream outputStream) {
        var userMap = new HashMap<User.Id, User>();

        try (PrintWriter writer = new PrintWriter(outputStream)) {
            List<String> keys = new ArrayList<>();

            tables.findEnded(gameId, 999999, from, to, false)
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
