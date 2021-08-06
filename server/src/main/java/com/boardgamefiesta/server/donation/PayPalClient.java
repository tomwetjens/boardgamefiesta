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

package com.boardgamefiesta.server.donation;

import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Map;

@ApplicationScoped
public class PayPalClient {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendInstant(0)
            .toFormatter();

    private final APIContext context;

    @Inject
    public PayPalClient(@ConfigProperty(name = "paypal.client-id") String clientId,
                        @ConfigProperty(name = "paypal.client-secret") String clientSecret,
                        @ConfigProperty(name = "paypal.mode") String mode) {
        context = new APIContext(clientId, clientSecret, mode);
    }

    public TransactionHistory listTransactions(Instant startDate, Instant endDate) throws PayPalRESTException {
        return TransactionHistory.list(context, Map.of(
                "start_date", DATE_TIME_FORMATTER.format(startDate),
                "end_date", DATE_TIME_FORMATTER.format(endDate)
        ));
    }

}
