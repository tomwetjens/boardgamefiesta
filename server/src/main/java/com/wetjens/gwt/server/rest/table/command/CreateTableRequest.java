package com.wetjens.gwt.server.rest.table.command;

import java.util.Map;
import java.util.Set;

import com.wetjens.gwt.server.domain.Table;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CreateTableRequest {

    @NotNull
    String game;

    @NotNull
    Table.Type type;

    Set<String> inviteUserIds;

    Map<String, Object> options;

}
