package com.boardgamefiesta.server.donation;

import com.paypal.base.rest.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class TransactionHistory extends PayPalResource {
    List<TransactionDetail> transactionDetails;

    public static TransactionHistory list(APIContext apiContext, Map<String, String> params) throws PayPalRESTException {
        var pattern = "v1/reporting/transactions?start_date={start_date}&end_date={end_date}";
        var resourcePath = RESTUtil.formatURIPath(pattern, params);
        var payload = "";
        var transactionHistory = configureAndExecute(apiContext, HttpMethod.GET, resourcePath, payload, TransactionHistory.class);
        apiContext.setRequestId(null);
        return transactionHistory;
    }
}
