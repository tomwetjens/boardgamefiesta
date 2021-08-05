package com.boardgamefiesta.server.donation;

import com.paypal.api.payments.Currency;
import com.paypal.base.rest.PayPalRESTException;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Path("/donations")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class DonationController {

    private static final double ESTIMATED_MONTHLY_COST = 140;

    private final PayPalClient payPalClient;

    private Double totalAmountReceived;
    private Instant lastUpdated;

    @Inject
    DonationController(PayPalClient payPalClient) {
        this.payPalClient = payPalClient;
    }

    @GET
    @Path("/status")
    public DonationStatusView status() {
        update();

        return totalAmountReceived != null
                ? new DonationStatusView(Math.max(0, ESTIMATED_MONTHLY_COST - totalAmountReceived), Math.min(100, (double) Math.round((totalAmountReceived / ESTIMATED_MONTHLY_COST) * 100)))
                : new DonationStatusView(null, null);
    }

    private void update() {
        if (totalAmountReceived == null || lastUpdated.isBefore(Instant.now().minusSeconds(3600))) {
            totalAmountReceived = getTotalAmountReceivedFromPayPal();
            lastUpdated = Instant.now();
        }
    }

    private Double getTotalAmountReceivedFromPayPal() {
        var endDate = Instant.now();
        var startDate = endDate.minus(31, ChronoUnit.DAYS);

        try {
            return payPalClient.listTransactions(startDate, endDate).getTransactionDetails().stream()
                    .map(TransactionDetail::getTransactionInfo)
                    .map(TransactionInfo::getTransactionAmount)
                    .map(Currency::getValue)
                    .mapToDouble(Double::parseDouble)
                    .filter(amount -> amount > 0)
                    .sum();
        } catch (PayPalRESTException e) {
            log.error("Could not retrieve donations", e);
            return null;
        }
    }

}
