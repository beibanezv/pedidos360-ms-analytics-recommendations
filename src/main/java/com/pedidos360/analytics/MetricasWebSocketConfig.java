package com.pedidos360.analytics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@EnableScheduling
class MetricasWebSocketConfig implements WebSocketConfigurer {
  private final MetricasSocket socket;
  private final String[] origins;

  MetricasWebSocketConfig(MetricasSocket socket, @Value("${CORS_ALLOWED_ORIGINS:http://localhost:4200}") String[] origins) {
    this.socket = socket;
    this.origins = origins;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(socket, "/ws/metricas").setAllowedOrigins(origins);
  }
}
