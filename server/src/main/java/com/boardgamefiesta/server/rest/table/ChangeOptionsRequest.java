package com.boardgamefiesta.server.rest.table;

import lombok.Data;

import java.util.Map;

@Data
public class ChangeOptionsRequest {
    private Map<String, Object> options;
}
