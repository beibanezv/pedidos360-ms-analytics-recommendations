package com.pedidos360.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
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
}
