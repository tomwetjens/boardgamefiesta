package com.boardgamefiesta.istanbul.view;

import com.boardgamefiesta.api.domain.PlayerColor;
import com.boardgamefiesta.istanbul.logic.Merchant;
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
