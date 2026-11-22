package com.pedidos360.analytics;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@EnabledIfEnvironmentVariable(named = "P360_TEST_KAFKA", matches = "true")
@EnabledIfEnvironmentVariable(named = "P360_TEST_ANALYTICS_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=${P360_TEST_ANALYTICS_URL}",
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
    "spring.kafka.producer.properties.spring.json.add.type.headers=false"
})
class FlujoKafkaWebSocketTest {
  @LocalServerPort int port;
  @Autowired KafkaTemplate<String, Object> kafka;
  @Autowired EventoRepository eventos;
  @Autowired ObjectMapper json;
  @MockBean JwtDecoder decoder;

  @Test
  void entregaMetricasPorWebsocketDespuesDeConsumirAmbosTopics() throws Exception {
    var usuario = UUID.randomUUID().toString();
    var producto = UUID.randomUUID();
    var inicio = Instant.now().minusSeconds(1);
    var antes = eventos.metricas(Instant.now().minusSeconds(86400));
    var vista = new InteraccionEvent(UUID.randomUUID(), usuario, "DETALLE_PRODUCTO", producto, Instant.now());
    var alerta = new PagoEvent(UUID.randomUUID(), usuario, UUID.randomUUID(), "BLOQUEADO", 18990, Instant.now());
    kafka.send("p360.interacciones.v1", usuario, vista).get(10, SECONDS);
    kafka.send("p360.pagos.v1", usuario, alerta).get(10, SECONDS);

    when(decoder.decode("admin-de-prueba")).thenReturn(Jwt.withTokenValue("admin-de-prueba")
        .header("alg", "RS256").subject(usuario).claim("roles", List.of("Admin"))
        .expiresAt(Instant.now().plusSeconds(60)).build());
    var http = HttpClient.newHttpClient();
    var respuesta = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/metricas/ticket"))
        .header("Authorization", "Bearer admin-de-prueba").POST(HttpRequest.BodyPublishers.noBody()).build(),
        HttpResponse.BodyHandlers.ofString());
    assertThat(respuesta.statusCode()).isEqualTo(200);

    var recibidos = new LinkedBlockingQueue<String>();
    var socket = http.newWebSocketBuilder().header("Origin", "http://localhost:4200")
        .buildAsync(URI.create("ws://localhost:" + port + "/ws/metricas"), new WebSocket.Listener() {
          private final StringBuilder texto = new StringBuilder();
          @Override public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            texto.append(data);
            if (last) {
              recibidos.add(texto.toString());
              texto.setLength(0);
            }
            ws.request(1);
            return null;
          }
        }).get(10, SECONDS);
    try {
      assertThat(recibidos.poll(3, SECONDS)).isNull();
      socket.sendText(json.readTree(respuesta.body()).get("ticket").asText(), true).get(5, SECONDS);
      Metricas ultima = null;
      for (int i = 0; i < 4; i++) {
        String mensaje = recibidos.poll(3, SECONDS);
        if (mensaje != null) {
          ultima = json.readValue(mensaje, Metricas.class);
          if (ultima.vistasProducto() > antes.vistasProducto() && ultima.alertasAntifraude() > antes.alertasAntifraude()) break;
        }
      }
      assertThat(ultima).isNotNull();
      assertThat(ultima.vistasProducto()).isGreaterThan(antes.vistasProducto());
      assertThat(ultima.alertasAntifraude()).isGreaterThan(antes.alertasAntifraude());
      assertThat(eventos.productosVistos(usuario)).containsExactly(producto);
      assertThat(eventos.metricas(inicio).alertasAntifraude()).isPositive();
    } finally {
      socket.abort();
    }
  }
}
