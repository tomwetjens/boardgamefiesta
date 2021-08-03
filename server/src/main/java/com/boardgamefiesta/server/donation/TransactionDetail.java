package com.boardgamefiesta.server.donation;

import com.paypal.base.rest.PayPalModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TransactionDetail extends PayPalModel {
    TransactionInfo transactionInfo;
}
