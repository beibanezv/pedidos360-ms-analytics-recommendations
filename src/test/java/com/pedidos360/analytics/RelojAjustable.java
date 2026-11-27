package com.pedidos360.analytics;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj mutable para avanzar el tiempo en los tests sin esperar segundos reales. */
final class RelojAjustable extends Clock {
  private Instant ahora;
  private final ZoneId zona;

  RelojAjustable(Instant inicio) {
    this.ahora = inicio;
    this.zona = ZoneOffset.UTC;
  }

  void avanzarSegundos(long segundos) {
    ahora = ahora.plusSeconds(segundos);
  }

  @Override
  public Instant instant() {
    return ahora;
  }

  @Override
  public ZoneId getZone() {
    return zona;
  }

  @Override
  public Clock withZone(ZoneId zona) {
    return this;
  }
}
