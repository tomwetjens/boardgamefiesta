package com.boardgamefiesta.server.donation;

import com.paypal.api.payments.Currency;
import com.paypal.base.rest.PayPalModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TransactionInfo extends PayPalModel {
    String transactionId;
    Currency transactionAmount;
}
