package com.pedidos360.analytics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
class CatalogoClient {
  private final RestClient client;

  CatalogoClient(RestClient.Builder builder, @Value("${PRODUCTOS_URL:http://localhost:8081}") String url) {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(2));
    factory.setReadTimeout(Duration.ofSeconds(3));
    client = builder.baseUrl(url).requestFactory(factory).build();
  }

  List<Producto> productos() {
    try {
      var body = client.get().uri("/productos").retrieve().body(Producto[].class);
      return body == null ? List.of() : Arrays.asList(body);
    } catch (RestClientException error) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Catálogo no disponible");
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Producto(UUID id, String nombre, String artistaOMarca, String categoria, long precioClp,
                  int stock, String descripcion) {}
}
