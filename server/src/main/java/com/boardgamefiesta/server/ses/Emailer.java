package com.boardgamefiesta.server.ses;

import com.boardgamefiesta.domain.user.User;
import lombok.NonNull;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Emailer {

    private final String from;
    private final SesClient sesClient;

    public Emailer(@ConfigProperty(name = "bgf.from") String from,
                   @NonNull SesClient sesClient) {
        this.from = from;
        this.sesClient = sesClient;
    }

    void sendEmailToUser(Message message, User user) {
        sesClient.sendEmail(SendEmailRequest.builder()
                .source(from)
                .destination(Destination.builder()
                        .toAddresses(user.getEmail())
                        .build())
                .message(message)
                .build());
    }
}
