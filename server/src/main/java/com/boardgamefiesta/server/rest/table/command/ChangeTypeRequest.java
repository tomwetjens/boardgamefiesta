package com.boardgamefiesta.server.rest.table.command;

import com.boardgamefiesta.server.domain.table.Table;
import lombok.Data;

@Data
public class ChangeTypeRequest {
    Table.Type type;
}
