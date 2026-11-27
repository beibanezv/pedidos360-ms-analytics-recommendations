package com.pedidos360.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

class DynamoDbHistorialTest {
  private final DynamoDbClient client = mock(DynamoDbClient.class);
  private final DynamoDbHistorial historial = new DynamoDbHistorial(client, "tabla-test");
  private final UUID id = UUID.randomUUID();

  private InteraccionEvent evento() {
    return new InteraccionEvent(id, "cliente", "DETALLE_PRODUCTO", UUID.randomUUID(), Instant.now());
  }

  @Test
  void archivaConInsercionCondicionalPorEventoId() {
    historial.registrar(evento());

    ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
    verify(client).putItem(captor.capture());
    assertThat(captor.getValue().tableName()).isEqualTo("tabla-test");
    assertThat(captor.getValue().conditionExpression()).isEqualTo("attribute_not_exists(eventoId)");
    assertThat(captor.getValue().item()).containsKeys("eventoId", "usuarioId", "tipo", "productoId", "ocurrioEn");
  }

  @Test
  void duplicadoCondicionalNoSePropaga() {
    when(client.putItem(any(PutItemRequest.class)))
        .thenThrow(ConditionalCheckFailedException.builder().message("duplicado").build());

    historial.registrar(evento());
  }

  @Test
  void otroFalloSiSePropagaParaNoConfirmarOffset() {
    when(client.putItem(any(PutItemRequest.class))).thenThrow(new IllegalStateException("sin permiso"));

    assertThatThrownBy(() -> historial.registrar(evento())).isInstanceOf(IllegalStateException.class);
  }
}
