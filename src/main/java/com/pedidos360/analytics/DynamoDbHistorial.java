package com.pedidos360.analytics;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/** Archivo persistente de eventos en DynamoDB, con inserción condicional por eventoId. */
@Component
class DynamoDbHistorial {
  private static final Logger log = LoggerFactory.getLogger(DynamoDbHistorial.class);
  private final DynamoDbClient client;
  private final String tabla;

  DynamoDbHistorial(DynamoDbClient client,
      @Value("${DYNAMODB_TABLE:p360.analytics.historial}") String tabla) {
    this.client = client;
    this.tabla = tabla;
  }

  /** Un duplicado condicional es éxito de archivo: la proyección PostgreSQL sigue su curso. */
  void registrar(InteraccionEvent evento) {
    Map<String, AttributeValue> item = new HashMap<>();
    item.put("eventoId", AttributeValue.fromS(evento.eventoId().toString()));
    item.put("usuarioId", AttributeValue.fromS(evento.usuarioId()));
    item.put("tipo", AttributeValue.fromS(evento.tipo()));
    item.put("ocurrioEn", AttributeValue.fromS(evento.ocurrioEn().toString()));
    if (evento.productoId() != null) {
      item.put("productoId", AttributeValue.fromS(evento.productoId().toString()));
    }
    try {
      client.putItem(PutItemRequest.builder()
          .tableName(tabla)
          .item(item)
          .conditionExpression("attribute_not_exists(eventoId)")
          .build());
    } catch (ConditionalCheckFailedException duplicado) {
      log.debug("Evento {} ya archivado en DynamoDB", evento.eventoId());
    }
  }
}
