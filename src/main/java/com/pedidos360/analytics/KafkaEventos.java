package com.pedidos360.analytics;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class KafkaEventos {
  private static final Logger log = LoggerFactory.getLogger(KafkaEventos.class);
  private static final Set<String> CON_PRODUCTO = Set.of("DETALLE_PRODUCTO", "AGREGAR_CARRITO");
  private static final Set<String> SIN_PRODUCTO = Set.of("CUPON_INGRESADO", "ENVIO_CONFIRMADO");
  private static final Set<String> PAGO = Set.of("APROBADO", "RECHAZADO", "BLOQUEADO");
  private final EventoRepository repo;

  KafkaEventos(EventoRepository repo) { this.repo = repo; }

  @KafkaListener(topics = "p360.interacciones.v1", groupId = "p360-recomendaciones-v1",
      containerFactory = "interaccionesFactory")
  void interaccion(InteraccionEvent e) {
    validar(e.eventoId(), e.usuarioId(), e.ocurrioEn());
    if (e.tipo() == null || !(CON_PRODUCTO.contains(e.tipo()) && e.productoId() != null
        || SIN_PRODUCTO.contains(e.tipo()) && e.productoId() == null)) {
      throw new IllegalArgumentException("Tipo o producto de interacción inválido");
    }
    registrar(e.eventoId(), e.usuarioId(), e.tipo(), e.productoId(), null, null, e.ocurrioEn());
  }

  @KafkaListener(topics = "p360.pagos.v1", groupId = "p360-metricas-v1", containerFactory = "pagosFactory")
  void pago(PagoEvent e) {
    validar(e.eventoId(), e.usuarioId(), e.ocurrioEn());
    if (e.ordenId() == null || e.totalClp() <= 0 || e.resultado() == null || !PAGO.contains(e.resultado())) {
      throw new IllegalArgumentException("Orden, monto o resultado de pago inválido");
    }
    registrar(e.eventoId(), e.usuarioId(), e.resultado(), null, e.ordenId(), e.totalClp(), e.ocurrioEn());
  }

  private void registrar(UUID id, String usuario, String tipo, UUID producto, UUID orden, Long total, Instant cuando) {
    if (repo.registrar(id, usuario, tipo, producto, orden, total, cuando) == 1) {
      log.info("Evento Kafka registrado tipo={} id={}", tipo, id);
    }
  }

  private void validar(UUID id, String usuario, Instant cuando) {
    if (id == null || cuando == null || cuando.isAfter(Instant.now().plusSeconds(60))
        || usuario == null || usuario.isBlank() || usuario.length() > 160) {
      throw new IllegalArgumentException("Identidad o fecha de evento inválida");
    }
  }
}
