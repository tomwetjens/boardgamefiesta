package com.wetjens.gwt.server.automa;

import io.quarkus.arc.config.ConfigProperties;
import lombok.Data;

@ConfigProperties(prefix = "gwt.sqs")
@Data
public class SqsConfiguration {

    String queueUrl;

}
