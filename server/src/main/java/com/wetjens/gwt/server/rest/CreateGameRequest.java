package com.wetjens.gwt.server.rest;

import java.util.Set;

import lombok.Data;

@Data
public class CreateGameRequest {

    Set<String> inviteUserIds;

}
