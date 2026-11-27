package com.pedidos360.analytics;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class KafkaEventosDynamoTest {
  @Test
  void archivaEnDynamoAntesDeProyectarEnPostgres() {
    EventoRepository repo = mock(EventoRepository.class);
    DynamoDbHistorial historial = mock(DynamoDbHistorial.class);
    KafkaEventos consumer = new KafkaEventos(repo, historial);
    var evento = new InteraccionEvent(UUID.randomUUID(), "cliente", "DETALLE_PRODUCTO",
        UUID.randomUUID(), Instant.now());

    consumer.interaccion(evento);

    InOrder orden = inOrder(historial, repo);
    orden.verify(historial).registrar(evento);
    orden.verify(repo).registrar(evento.eventoId(), "cliente", "DETALLE_PRODUCTO",
        evento.productoId(), null, null, evento.ocurrioEn());
  }
}
