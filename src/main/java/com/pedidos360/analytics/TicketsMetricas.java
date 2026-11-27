package com.pedidos360.analytics;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@Component
class TicketsMetricas {
  private final ConcurrentHashMap<String, Ticket> pendientes = new ConcurrentHashMap<>();
  private final Clock reloj;
  private final DynamoDbClient dynamo;
  private final String tabla;

  TicketsMetricas() {
    this(Clock.systemUTC());
  }

  TicketsMetricas(Clock reloj) {
    this(null, "p360.analytics.tickets", reloj);
  }

  @Autowired
  TicketsMetricas(DynamoDbClient dynamo, @Value("${TICKETS_TABLE:p360.analytics.tickets}") String tabla) {
    this(dynamo, tabla, Clock.systemUTC());
  }

  TicketsMetricas(DynamoDbClient dynamo, String tabla, Clock reloj) {
    this.dynamo = dynamo;
    this.tabla = tabla;
    this.reloj = reloj;
  }

  synchronized String emitir(Instant venceJwt) {
    Instant ahora = reloj.instant();
    if (venceJwt == null || !venceJwt.isAfter(ahora)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    pendientes.values().removeIf(t -> !t.validoHasta().isAfter(ahora));
    if (pendientes.size() >= 256) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
    String id = UUID.randomUUID().toString();
    Instant validoHasta = ahora.plusSeconds(30);
    Instant sesionHasta = venceJwt.isBefore(ahora.plusSeconds(300)) ? venceJwt : ahora.plusSeconds(300);
    pendientes.put(id, new Ticket(validoHasta, sesionHasta));
    if (dynamo != null) {
      dynamo.putItem(PutItemRequest.builder().tableName(tabla).item(Map.of(
          "ticketId", AttributeValue.builder().s(id).build(),
          "expiraEn", AttributeValue.builder().n(Long.toString(validoHasta.getEpochSecond())).build(),
          "sesionHasta", AttributeValue.builder().n(Long.toString(sesionHasta.getEpochSecond())).build())).build());
    }
    return id;
  }

  Instant consumir(String id) {
    Ticket ticket = pendientes.remove(id);
    Instant ahora = reloj.instant();
    return ticket != null && ticket.validoHasta().isAfter(ahora) && ticket.sesionHasta().isAfter(ahora)
        ? ticket.sesionHasta() : null;
  }

  private record Ticket(Instant validoHasta, Instant sesionHasta) {}
}
