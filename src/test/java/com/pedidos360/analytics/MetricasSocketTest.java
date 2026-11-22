package com.pedidos360.analytics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class MetricasSocketTest {
  @Test
  void soloEnviaMetricasDespuesDeConsumirUnTicketValido() throws Exception {
    var tickets = new TicketsMetricas();
    var repo = mock(EventoRepository.class);
    when(repo.metricas(any())).thenReturn(new Metricas(1, 2, 3, 4));
    var socket = new MetricasSocket(tickets, repo, new ObjectMapper());
    var cliente = mock(WebSocketSession.class);
    when(cliente.getId()).thenReturn("cliente");
    when(cliente.isOpen()).thenReturn(true);

    socket.afterConnectionEstablished(cliente);
    socket.actualizar();
    verify(cliente, never()).sendMessage(any());
    socket.handleTextMessage(cliente, new TextMessage(tickets.emitir(Instant.now().plusSeconds(60))));
    socket.actualizar();
    verify(cliente).sendMessage(new TextMessage(
        "{\"vistasProducto\":1,\"productosAgregados\":2,\"pagosAprobados\":3,\"alertasAntifraude\":4}"));
  }

  @Test
  void cierraSinMetricasSiElTicketEsInvalido() throws Exception {
    var socket = new MetricasSocket(new TicketsMetricas(), mock(EventoRepository.class), new ObjectMapper());
    var cliente = mock(WebSocketSession.class);
    when(cliente.getId()).thenReturn("cliente");

    socket.afterConnectionEstablished(cliente);
    socket.handleTextMessage(cliente, new TextMessage("ticket-invalido"));

    verify(cliente).close(CloseStatus.POLICY_VIOLATION);
    verify(cliente, never()).sendMessage(any());
  }
}
