package com.tomsboardgames.server.rest.table;

import lombok.Data;

import java.util.Map;

@Data
public class ChangeOptionsRequest {
    private Map<String, Object> options;
}
