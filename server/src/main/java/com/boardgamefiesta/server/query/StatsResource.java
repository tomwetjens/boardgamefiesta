package com.boardgamefiesta.server.query;

import com.boardgamefiesta.api.domain.Stats;
import com.boardgamefiesta.server.domain.game.Game;
import com.boardgamefiesta.server.domain.table.Player;
import com.boardgamefiesta.server.domain.table.Table;
import com.boardgamefiesta.server.domain.table.Tables;
import lombok.NonNull;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Disabled because it took the server down due to memory issues
//@Path("/stats/{gameId}")
@ApplicationScoped
public class StatsResource {

    Tables tables;

    @Inject
    public StatsResource(@NonNull Tables tables) {
        this.tables = tables;
    }

    @GET
    @Produces("text/csv")
    public Response get(@PathParam("gameId") String gameId) {
        return Response.ok((StreamingOutput) outputStream -> {
            try (PrintWriter writer = new PrintWriter(outputStream)) {
                List<String> keys = new ArrayList<>();

                tables.findAll(Game.Id.of(gameId))
                        .filter(table -> table.getStatus() == Table.Status.ENDED)
                        .filter(table -> !table.hasComputerPlayers())
                        .forEach(table -> table.getPlayers().forEach(player -> {
                            var stats = table.getState().stats(table.getState().getPlayerByName(player.getId().getId()).orElseThrow());

                            if (keys.isEmpty()) {
                                keys.addAll(stats.keys());

                                writeHeader(writer, keys);
                            }

                            writeRow(writer, keys, table, player, stats);
                        }));
            }
        }).header("Content-Disposition", "attachment; filename=\"" + gameId + "_" + Instant.now().toString().replace(":", "") + ".csv\"").build();
    }

    private void writeHeader(PrintWriter writer, List<String> keys) {
        writer.print("tableId,time,score,winner");
        keys.forEach(key -> {
            writer.print(',');
            writer.print(key);
        });
        writer.println();
    }

    private void writeRow(PrintWriter writer, List<String> keys, Table table, Player player, Stats stats) {
        writer.print(table.getId().getId());
        writer.print(',');
        writer.print(Duration.between(table.getStarted(), table.getEnded()).toMinutes());
        writer.print(',');
        writer.print(player.getScore().map(score -> Integer.toString(score)).orElse(""));
        writer.print(',');
        writer.print(player.getWinner().map(winner -> winner ? "Y" : "N").orElse(""));
        keys.forEach(key -> {
            writer.print(',');
            writer.print(stats.value(key).orElse(""));
        });
        writer.println();
    }

}
