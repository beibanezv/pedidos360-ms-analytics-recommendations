package com.pedidos360.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@DataJpaTest(properties = "spring.datasource.url=${P360_TEST_ANALYTICS_URL}")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "P360_TEST_ANALYTICS_URL", matches = ".+")
@Import({KafkaEventos.class, DynamoDbHistorial.class})
class KafkaEventosPostgresTest {
  @Autowired KafkaEventos eventos;
  @Autowired EventoRepository repo;
  @MockBean DynamoDbClient dynamoDb;

  @Test
  void contabilizaAmbosTopicsSinDuplicar() {
    Instant inicio = Instant.now().minusSeconds(2);
    var usuario = UUID.randomUUID().toString();
    UUID producto = UUID.randomUUID();
    var vista = new InteraccionEvent(UUID.randomUUID(), usuario, "DETALLE_PRODUCTO", producto, Instant.now());
    var pago = new PagoEvent(UUID.randomUUID(), usuario, UUID.randomUUID(), "BLOQUEADO", 18990, Instant.now());

    var antes = repo.metricas(inicio);
    eventos.interaccion(vista);
    eventos.interaccion(vista);
    eventos.pago(pago);
    eventos.pago(pago);

    assertThat(repo.productosVistos(usuario)).containsExactly(producto);
    assertThat(repo.metricas(inicio)).isEqualTo(new Metricas(antes.vistasProducto() + 1,
        antes.productosAgregados(), antes.pagosAprobados(), antes.alertasAntifraude() + 1));
  }

  @Test
  void rechazaEventoDeOtroDominio() {
    var evento = new InteraccionEvent(UUID.randomUUID(), "cliente", "APROBADO", UUID.randomUUID(), Instant.now());
    assertThatIllegalArgumentException().isThrownBy(() -> eventos.interaccion(evento));
  }
}
