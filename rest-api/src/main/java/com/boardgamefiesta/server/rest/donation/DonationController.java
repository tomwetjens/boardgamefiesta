/*
 * Board Game Fiesta
 * Copyright (C)  2021 Tom Wetjens <tomwetjens@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.boardgamefiesta.server.rest.donation;

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
