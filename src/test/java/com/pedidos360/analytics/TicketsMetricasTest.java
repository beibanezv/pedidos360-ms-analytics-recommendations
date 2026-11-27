package com.pedidos360.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

class TicketsMetricasTest {
  @Test
  void ticketDeUnSoloUsoConLimiteDeLaSesion() {
    var tickets = new TicketsMetricas();
    Instant fin = Instant.now().plusSeconds(60);
    var ticket = tickets.emitir(fin);

    assertThat(tickets.consumir(ticket)).isEqualTo(fin);
    assertThat(tickets.consumir(ticket)).isNull();
  }

  @Test
  void noEmiteTicketSiLaSesionYaVencio() {
    var tickets = new TicketsMetricas();

    assertThatThrownBy(() -> tickets.emitir(Instant.now().minusSeconds(1)))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void expiraElTicketALos30Segundos() {
    var inicio = Instant.parse("2026-01-01T00:00:00Z");
    var reloj = new RelojAjustable(inicio);
    var tickets = new TicketsMetricas(reloj);
    var ticket = tickets.emitir(inicio.plusSeconds(60));

    reloj.avanzarSegundos(30);

    assertThat(tickets.consumir(ticket)).isNull();
  }

  @Test
  void ticketEmitidoSeGuardaEnDynamoParaLaLambda() {
    var inicio = Instant.parse("2026-01-01T00:00:00Z");
    var dynamo = Mockito.mock(DynamoDbClient.class);
    var tickets = new TicketsMetricas(dynamo, "p360.analytics.tickets", new RelojAjustable(inicio));

    tickets.emitir(inicio.plusSeconds(60));

    var captor = ArgumentCaptor.forClass(PutItemRequest.class);
    Mockito.verify(dynamo).putItem(captor.capture());
    Map<String, AttributeValue> item = captor.getValue().item();
    assertThat(captor.getValue().tableName()).isEqualTo("p360.analytics.tickets");
    assertThat(item.get("expiraEn").n()).isEqualTo("1767225630");
    assertThat(item.get("sesionHasta").n()).isEqualTo("1767225660");
    assertThat(item.get("ticketId").s()).isNotBlank();
  }

  @Test
  void rechazaElTicket257Con429() {
    var inicio = Instant.parse("2026-01-01T00:00:00Z");
    var tickets = new TicketsMetricas(new RelojAjustable(inicio));

    for (int i = 0; i < 256; i++) {
      tickets.emitir(inicio.plusSeconds(3600));
    }

    assertThatThrownBy(() -> tickets.emitir(inicio.plusSeconds(3600)))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
  }
}
