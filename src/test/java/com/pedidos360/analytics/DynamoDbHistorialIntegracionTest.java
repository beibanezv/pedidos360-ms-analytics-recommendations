package com.pedidos360.analytics;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;

/** Prueba real opcional: se activa solo con una tabla de test configurada por Benjamín. */
@EnabledIfEnvironmentVariable(named = "P360_TEST_DYNAMODB_TABLE", matches = ".+")
class DynamoDbHistorialIntegracionTest {
  @Test
  void escribeUnEventoDePruebaConIdUnico() {
    String tabla = System.getenv("P360_TEST_DYNAMODB_TABLE");
    String region = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
    try (DynamoDbClient client = DynamoDbClient.builder().region(Region.of(region)).build()) {
      var id = UUID.randomUUID();
      var evento = new InteraccionEvent(id, "prueba-" + id, "DETALLE_PRODUCTO", UUID.randomUUID(), Instant.now());
      new DynamoDbHistorial(client, tabla).registrar(evento);
      assertThatCode(() -> client.deleteItem(DeleteItemRequest.builder().tableName(tabla)
          .key(Map.of("eventoId", AttributeValue.fromS(id.toString()))).build())).doesNotThrowAnyException();
    }
  }
}
