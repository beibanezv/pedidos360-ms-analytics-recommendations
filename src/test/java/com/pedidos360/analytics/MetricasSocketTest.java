package com.pedidos360.analytics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class MetricasSocketTest {
  private static final String ORIGEN = "https://frontend.test";

  private WebSocketSession cliente(String id) {
    var session = mock(WebSocketSession.class);
    var headers = new HttpHeaders();
    headers.set("Origin", ORIGEN);
    when(session.getHandshakeHeaders()).thenReturn(headers);
    when(session.getId()).thenReturn(id);
    when(session.isOpen()).thenReturn(true);
    return session;
  }

  @Test
  void soloEnviaMetricasDespuesDeConsumirUnTicketValido() throws Exception {
    var tickets = new TicketsMetricas();
    var repo = mock(EventoRepository.class);
    when(repo.metricas(any())).thenReturn(new Metricas(1, 2, 3, 4));
    var socket = new MetricasSocket(tickets, repo, new ObjectMapper(), ORIGEN);
    var cliente = cliente("cliente");

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
    var socket = new MetricasSocket(new TicketsMetricas(), mock(EventoRepository.class),
        new ObjectMapper(), ORIGEN);
    var cliente = cliente("cliente");

    socket.afterConnectionEstablished(cliente);
    socket.handleTextMessage(cliente, new TextMessage("ticket-invalido"));

    verify(cliente).close(CloseStatus.POLICY_VIOLATION);
    verify(cliente, never()).sendMessage(any());
  }

  @Test
  void rechazaUnOrigenAjeno() throws Exception {
    var socket = new MetricasSocket(new TicketsMetricas(), mock(EventoRepository.class),
        new ObjectMapper(), ORIGEN);
    var cliente = mock(WebSocketSession.class);
    var headers = new HttpHeaders();
    headers.set("Origin", "https://otro-sitio.test");
    when(cliente.getHandshakeHeaders()).thenReturn(headers);

    socket.afterConnectionEstablished(cliente);

    verify(cliente).close(CloseStatus.POLICY_VIOLATION);
    verify(cliente, never()).sendMessage(any());
  }

  @Test
  void noAceptaUnTicketReutilizadoEnOtraConexion() throws Exception {
    var tickets = new TicketsMetricas();
    var socket = new MetricasSocket(tickets, mock(EventoRepository.class), new ObjectMapper(), ORIGEN);
    var primero = cliente("primero");
    var segundo = cliente("segundo");
    var ticket = tickets.emitir(Instant.now().plusSeconds(60));
    socket.afterConnectionEstablished(primero);
    socket.afterConnectionEstablished(segundo);

    socket.handleTextMessage(primero, new TextMessage(ticket));
    socket.handleTextMessage(segundo, new TextMessage(ticket));

    verify(primero, never()).close(CloseStatus.POLICY_VIOLATION);
    verify(segundo).close(CloseStatus.POLICY_VIOLATION);
  }

  @Test
  void cierraYLimpiaLaConexionSiVenceLaVentanaDeAutorizacion() throws Exception {
    var inicio = Instant.parse("2026-01-01T00:00:00Z");
    var reloj = new RelojAjustable(inicio);
    var tickets = new TicketsMetricas(reloj);
    var repo = mock(EventoRepository.class);
    when(repo.metricas(any())).thenReturn(new Metricas(1, 2, 3, 4));
    var socket = new MetricasSocket(tickets, repo, new ObjectMapper(), ORIGEN, reloj);
    var cliente = cliente("cliente");
    var ticket = tickets.emitir(inicio.plusSeconds(60));

    socket.afterConnectionEstablished(cliente);
    reloj.avanzarSegundos(11);
    socket.handleTextMessage(cliente, new TextMessage(ticket));
    socket.actualizar();

    verify(cliente).close(CloseStatus.POLICY_VIOLATION);
    verify(cliente, never()).sendMessage(any());
  }

  @Test
  void cierraSinMetricasSiElTicketVencioAlConsumirlo() throws Exception {
    var inicio = Instant.parse("2026-01-01T00:00:00Z");
    var reloj = new RelojAjustable(inicio);
    var tickets = new TicketsMetricas(reloj);
    var repo = mock(EventoRepository.class);
    when(repo.metricas(any())).thenReturn(new Metricas(1, 2, 3, 4));
    var socket = new MetricasSocket(tickets, repo, new ObjectMapper(), ORIGEN, reloj);
    var cliente = cliente("cliente");
    var ticket = tickets.emitir(inicio.plusSeconds(60));

    reloj.avanzarSegundos(25);
    socket.afterConnectionEstablished(cliente);
    reloj.avanzarSegundos(6);
    socket.handleTextMessage(cliente, new TextMessage(ticket));
    socket.actualizar();

    verify(cliente).close(CloseStatus.POLICY_VIOLATION);
    verify(cliente, never()).sendMessage(any());
  }

  @Test
  void rechazaLaConexion51() throws Exception {
    var reloj = new RelojAjustable(Instant.parse("2026-01-01T00:00:00Z"));
    var socket = new MetricasSocket(new TicketsMetricas(reloj), mock(EventoRepository.class),
        new ObjectMapper(), ORIGEN, reloj);
    var aceptados = new ArrayList<WebSocketSession>();
    for (int i = 1; i <= 50; i++) {
      var aceptado = cliente("cliente-" + i);
      aceptados.add(aceptado);
      socket.afterConnectionEstablished(aceptado);
    }
    var excedente = cliente("cliente-51");

    socket.afterConnectionEstablished(excedente);

    for (var aceptado : aceptados) {
      verify(aceptado, never()).close(any(CloseStatus.class));
    }
    verify(excedente).close(CloseStatus.POLICY_VIOLATION);
    verify(excedente, never()).sendMessage(any());
  }
}
