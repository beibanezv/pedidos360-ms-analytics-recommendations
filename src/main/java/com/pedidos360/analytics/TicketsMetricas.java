package com.pedidos360.analytics;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class TicketsMetricas {
  private final ConcurrentHashMap<String, Ticket> pendientes = new ConcurrentHashMap<>();

  synchronized String emitir(Instant venceJwt) {
    Instant ahora = Instant.now();
    if (venceJwt == null || !venceJwt.isAfter(ahora)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    pendientes.values().removeIf(t -> !t.validoHasta().isAfter(ahora));
    if (pendientes.size() >= 256) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
    String id = UUID.randomUUID().toString();
    pendientes.put(id, new Ticket(ahora.plusSeconds(30),
        venceJwt.isBefore(ahora.plusSeconds(300)) ? venceJwt : ahora.plusSeconds(300)));
    return id;
  }

  Instant consumir(String id) {
    Ticket ticket = pendientes.remove(id);
    Instant ahora = Instant.now();
    return ticket != null && ticket.validoHasta().isAfter(ahora) && ticket.sesionHasta().isAfter(ahora)
        ? ticket.sesionHasta() : null;
  }

  private record Ticket(Instant validoHasta, Instant sesionHasta) {}
}
