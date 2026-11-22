package com.pedidos360.analytics;

import java.time.Instant;
import java.util.UUID;

record PagoEvent(UUID eventoId, String usuarioId, UUID ordenId, String resultado, long totalClp, Instant ocurrioEn) {}
