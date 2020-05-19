package com.tomsboardgames.server.rest.table.command;

import com.tomsboardgames.server.domain.Table;
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

    Set<String> inviteUserIds;

    Map<String, Object> options;

}
