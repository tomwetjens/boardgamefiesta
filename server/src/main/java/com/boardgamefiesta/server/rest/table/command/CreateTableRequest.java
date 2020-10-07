package com.boardgamefiesta.server.rest.table.command;

import com.boardgamefiesta.server.domain.table.Table;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
public class CreateTableRequest {

    @NotNull
    String game;

    @NotNull
    Table.Type type;

    @NotNull
    Table.Mode mode;

    Map<String, Object> options;

}
