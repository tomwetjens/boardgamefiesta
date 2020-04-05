package com.wetjens.gwt.server.repository;

import lombok.Builder;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Getter
public class DynamoDbConfig {

    @ConfigProperty(name = "gwt.dynamodb.tableSuffix", defaultValue = "")
    private String tableSuffix;

}
