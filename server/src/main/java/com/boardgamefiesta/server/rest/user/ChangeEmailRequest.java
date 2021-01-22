package com.boardgamefiesta.server.rest.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ChangeEmailRequest {
    @NotBlank
    private String email;
}
