package com.wetjens.gwt.server.repository;

import io.quarkus.arc.config.ConfigProperties;
import lombok.Data;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ConfigProperties(prefix = "gwt.dynamodb")
@Data
public class DynamoDbConfiguration {

    @ConfigProperty
    private Optional<String> tableSuffix;

}
