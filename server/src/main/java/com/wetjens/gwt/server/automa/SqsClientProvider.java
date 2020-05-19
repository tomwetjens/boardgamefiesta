package com.wetjens.gwt.server.automa;

import software.amazon.awssdk.services.sqs.SqsClient;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class SqsClientProvider {

    @Produces
    public SqsClient provideSqsClient() {
        return SqsClient.create();
    }

}
