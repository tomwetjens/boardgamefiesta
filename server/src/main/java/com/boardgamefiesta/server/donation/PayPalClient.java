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
