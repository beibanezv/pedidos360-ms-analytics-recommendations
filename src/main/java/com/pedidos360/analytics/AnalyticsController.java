package com.pedidos360.analytics;

import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AnalyticsController {
  private final RecomendacionService recomendaciones;
  private final TicketsMetricas tickets;

  AnalyticsController(RecomendacionService recomendaciones, TicketsMetricas tickets) {
    this.recomendaciones = recomendaciones;
    this.tickets = tickets;
  }

  @GetMapping("/recomendaciones")
  ResponseEntity<List<CatalogoClient.Producto>> recomendaciones(@AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(recomendaciones.para(IdentityConfig.usuario(jwt)));
  }

  @PostMapping("/metricas/ticket")
  ResponseEntity<Map<String, String>> ticket(@AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(Map.of("ticket", tickets.emitir(jwt.getExpiresAt())));
  }
}
