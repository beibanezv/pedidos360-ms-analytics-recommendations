package com.pedidos360.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
class MetricasSocket extends TextWebSocketHandler {
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(MetricasSocket.class);
  private final TicketsMetricas tickets;
  private final EventoRepository eventos;
  private final ObjectMapper json;
  private final Map<String, Conexion> clientes = new ConcurrentHashMap<>();

  MetricasSocket(TicketsMetricas tickets, EventoRepository eventos, ObjectMapper json) {
    this.tickets = tickets;
    this.eventos = eventos;
    this.json = json;
  }

  @Override
  public synchronized void afterConnectionEstablished(WebSocketSession session) throws IOException {
    if (clientes.size() >= 50) {
      session.close(CloseStatus.POLICY_VIOLATION);
      return;
    }
    session.setTextMessageSizeLimit(128);
    clientes.put(session.getId(), new Conexion(session, Instant.now().plusSeconds(10), false));
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
    var conexion = clientes.get(session.getId());
    Instant vence = conexion != null && !conexion.autorizada() && conexion.vence().isAfter(Instant.now())
        && message.getPayloadLength() == 36 ? tickets.consumir(message.getPayload()) : null;
    if (vence == null || !clientes.replace(session.getId(), conexion, new Conexion(session, vence, true))) {
      clientes.remove(session.getId());
      session.close(CloseStatus.POLICY_VIOLATION);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    clientes.remove(session.getId());
  }

  @Scheduled(fixedDelay = 2000)
  void actualizar() {
    Instant ahora = Instant.now();
    var autorizadas = clientes.values().stream().filter(c -> {
      if (c.vence().isAfter(ahora) && c.session().isOpen()) return c.autorizada();
      if (clientes.remove(c.session().getId(), c) && c.session().isOpen()) cerrar(c.session());
      return false;
    }).toList();
    if (autorizadas.isEmpty()) return;
    try {
      var mensaje = new TextMessage(json.writeValueAsString(eventos.metricas(ahora.minus(1, ChronoUnit.DAYS))));
      for (var c : autorizadas) {
        if (clientes.get(c.session().getId()) != c || !c.vence().isAfter(Instant.now())) continue;
        try {
          c.session().sendMessage(mensaje);
        } catch (IOException | IllegalStateException e) {
          clientes.remove(c.session().getId(), c);
          cerrar(c.session());
        }
      }
    } catch (Exception e) {
      log.warn("No se pudieron actualizar las métricas ({})", e.getClass().getSimpleName());
    }
  }

  private void cerrar(WebSocketSession session) {
    try {
      session.close(CloseStatus.POLICY_VIOLATION);
    } catch (IOException e) {
      log.debug("No se pudo cerrar el socket de métricas", e);
    }
  }

  private record Conexion(WebSocketSession session, Instant vence, boolean autorizada) {}
}
