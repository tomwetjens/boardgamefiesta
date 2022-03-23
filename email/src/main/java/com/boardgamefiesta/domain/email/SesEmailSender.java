/*
 * Board Game Fiesta
 * Copyright (C)  2022 Tom Wetjens <tomwetjens@gmail.com>
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

package com.boardgamefiesta.domain.email;

import com.boardgamefiesta.domain.user.User;
import lombok.NonNull;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class SesEmailSender {

    private final String from;
    private final SesClient sesClient;

    @Inject
    public SesEmailSender(@ConfigProperty(name = "bgf.from") String from,
                          @NonNull SesClient sesClient) {
        this.from = from;
        this.sesClient = sesClient;
    }

    public void sendEmailToUser(Message message, User user) {
        sesClient.sendEmail(SendEmailRequest.builder()
                .source(from)
                .destination(Destination.builder()
                        .toAddresses(user.getEmail())
                        .build())
                .message(software.amazon.awssdk.services.ses.model.Message.builder()
                        .subject(Content.builder()
                                .charset(StandardCharsets.UTF_8.name())
                                .data(message.getSubject())
                                .build())
                        .body(Body.builder()
                                .html(Content.builder()
                                        .charset(StandardCharsets.UTF_8.name())
                                        .data(message.getBody())
                                        .build())
                                .build())
                        .build())
                .build());
    }
}
