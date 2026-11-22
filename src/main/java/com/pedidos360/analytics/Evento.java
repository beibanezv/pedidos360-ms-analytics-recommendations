package com.pedidos360.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics_eventos", indexes = {
    @Index(name = "idx_analytics_historial", columnList = "usuario_id,tipo,ocurrio_en"),
    @Index(name = "idx_analytics_fecha", columnList = "ocurrio_en")})
class Evento {
  @Id private UUID id;
  @Column(nullable = false) private String usuarioId;
  @Column(nullable = false) private String tipo;
  private UUID productoId;
  private UUID ordenId;
  private Long totalClp;
  @Column(nullable = false) private Instant ocurrioEn;

  protected Evento() {}
}
