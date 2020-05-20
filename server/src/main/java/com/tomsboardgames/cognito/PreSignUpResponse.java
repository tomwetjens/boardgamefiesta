package com.tomsboardgames.cognito;

import lombok.Data;

@Data
public class PreSignUpResponse {

    private boolean autoConfirmUser;
    private boolean autoVerifyPhone;
    private boolean autoVerifyEmail;

}
