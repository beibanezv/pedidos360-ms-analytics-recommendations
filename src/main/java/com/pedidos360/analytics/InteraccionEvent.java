package com.pedidos360.analytics;

import java.time.Instant;
import java.util.UUID;

record InteraccionEvent(UUID eventoId, String usuarioId, String tipo, UUID productoId, Instant ocurrioEn) {}
