# Pedidos360 · Recomendaciones y métricas

Spring Boot 3.3, Java 17, PostgreSQL y Kafka. En local, crea una base
`pedidos360_analytics` en el PostgreSQL del Compose de ms-pagos y levanta Kafka
desde `pedidos360-ms-admin/docker/kafka-cluster`. Luego: `mvn spring-boot:run`.

| Topic | Grupo lector | Uso |
|---|---|---|
| `p360.interacciones.v1` | `p360-recomendaciones-v1` | Historial por usuario y recuento de fichas y adiciones. |
| `p360.pagos.v1` | `p360-metricas-v1` | Recuento de pagos aprobados y bloqueos. |

- `GET /recomendaciones`: devuelve hasta cuatro productos disponibles, de la
  categoría vista más recientemente, excluyendo los que ya vio el usuario
  identificado por su JWT. No expone el historial de otra persona.
- `POST /metricas/ticket`: solo Azure Admin. Emite una credencial de un uso y
  30 segundos. En `ws://localhost:8090/ws/metricas` se presenta como primer
  mensaje; el servidor devuelve cuatro métricas globales de las últimas 24 h.
  Usa WSS en nube y no envíes JWT ni ticket en la URL.

Configura `DATASOURCE_URL`, `DATASOURCE_USER`, `DATASOURCE_PASSWORD`,
`KAFKA_BOOTSTRAP_SERVERS`, `PRODUCTOS_URL` y `CORS_ALLOWED_ORIGINS`. Para las
pruebas PostgreSQL ejecuta `mvn test` con `P360_TEST_ANALYTICS_URL` apuntando
a una base de test dedicada. Los eventos con ID duplicado no se cuentan de nuevo.
Los eventos inválidos se descartan con un log de tópico, partición y offset.
Si falla la persistencia, el consumidor reintenta sin confirmar el evento;
el grupo puede acumular lag hasta recuperar PostgreSQL.
Con `P360_TEST_KAFKA=true`, la suite también produce a los tópicos del clúster
local, consume por los dos grupos y lee métricas por un WebSocket real.
Detén la instancia local de Analytics antes de probar: sus consumidores
comparten los grupos con la instancia de integración. No lo ejecutes con Kafka
de producción: escribe eventos de prueba.
