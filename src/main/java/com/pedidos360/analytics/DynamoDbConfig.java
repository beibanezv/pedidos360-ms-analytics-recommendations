package com.pedidos360.analytics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** Cliente único con cadena de credenciales estándar (rol de instancia cuando el lab lo permita). */
@Configuration
class DynamoDbConfig {
  @Bean
  DynamoDbClient dynamoDb(@Value("${AWS_REGION:us-east-1}") String region) {
    return DynamoDbClient.builder().region(Region.of(region)).build();
  }
}
