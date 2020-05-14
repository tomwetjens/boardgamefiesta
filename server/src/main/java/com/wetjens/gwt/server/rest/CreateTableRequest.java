package com.wetjens.gwt.server.rest;

import java.util.Map;
import java.util.Set;

import com.wetjens.gwt.server.domain.Table;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class CreateTableRequest {

    @NotNull
    String game;

    @NotNull
    Table.Type type;

    Set<String> inviteUserIds;

    Map<String, Object> options;

}
