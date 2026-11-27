package com.pedidos360.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
