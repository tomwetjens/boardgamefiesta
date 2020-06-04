package com.tomsboardgames.istanbul.view;

import com.tomsboardgames.api.PlayerColor;
import com.tomsboardgames.istanbul.logic.Merchant;
import lombok.Getter;

@Getter
public class MerchantView {

    private final int assistants;
    private final PlayerColor color;

    MerchantView(Merchant merchant) {
        this.color = merchant.getColor();
        this.assistants = merchant.getAssistants();
    }
}
