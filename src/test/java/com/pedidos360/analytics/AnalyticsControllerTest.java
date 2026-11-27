package com.pedidos360.analytics;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@WebMvcTest(AnalyticsController.class)
@Import({IdentityConfig.class, TicketsMetricas.class})
class AnalyticsControllerTest {
  @Autowired MockMvc mvc;
  @MockBean RecomendacionService service;
  @MockBean DynamoDbClient dynamo;

  @Test
  void recomendacionesRequierenJwtYSeCalculanParaSuDueno() throws Exception {
    var producto = new CatalogoClient.Producto(UUID.randomUUID(), "Nocturno", "Ana", "vinilo", 18990, 1, null);
    when(service.para("dueño")).thenReturn(List.of(producto));

    mvc.perform(get("/recomendaciones")).andExpect(status().isUnauthorized());
    mvc.perform(get("/recomendaciones").param("usuarioId", "otro")
        .with(jwt().jwt(j -> j.claim("oid", "dueño"))))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(producto.id().toString()));
  }

  @Test
  void soloAdminObtieneTicket() throws Exception {
    mvc.perform(post("/metricas/ticket").with(jwt())).andExpect(status().isForbidden());
    mvc.perform(post("/metricas/ticket").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))
        .jwt(j -> j.expiresAt(Instant.now().plusSeconds(60)))))
        .andExpect(status().isOk()).andExpect(jsonPath("$.ticket").isNotEmpty());
  }
}
