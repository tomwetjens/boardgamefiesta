package com.boardgamefiesta.server.rest.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ChangePasswordRequest {
    @NotBlank
    private String password;
}
