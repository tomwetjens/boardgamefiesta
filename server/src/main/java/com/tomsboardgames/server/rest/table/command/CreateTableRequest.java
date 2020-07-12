package com.boardgamefiesta.server.rest.table.command;

import com.boardgamefiesta.server.domain.Table;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

@Data
public class CreateTableRequest {

    @NotNull
    String game;

    @NotNull
    Table.Type type;

    @NotNull
    Table.Mode mode;

    Set<String> inviteUserIds;

    Map<String, Object> options;

}
