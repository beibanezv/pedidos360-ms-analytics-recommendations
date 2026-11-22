package com.pedidos360.analytics;

import java.util.HashMap;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
class KafkaEventosConfig {
  private static final Logger log = LoggerFactory.getLogger(KafkaEventosConfig.class);

  @Bean
  ConcurrentKafkaListenerContainerFactory<String, InteraccionEvent> interaccionesFactory(KafkaProperties properties) {
    return factory(properties, InteraccionEvent.class);
  }

  @Bean
  ConcurrentKafkaListenerContainerFactory<String, PagoEvent> pagosFactory(KafkaProperties properties) {
    return factory(properties, PagoEvent.class);
  }

  private <T> ConcurrentKafkaListenerContainerFactory<String, T> factory(KafkaProperties properties, Class<T> type) {
    var settings = new HashMap<>(properties.buildConsumerProperties(null));
    var consumers = new DefaultKafkaConsumerFactory<>(settings, StringDeserializer::new,
        () -> new ErrorHandlingDeserializer<>(new JsonDeserializer<>(type, false)));
    var errors = new DefaultErrorHandler((record, ex) -> log.error(
        "Kafka descartado por formato o validación topic={} partición={} offset={} error={}",
        record.topic(), record.partition(), record.offset(), ex.getClass().getSimpleName()),
        // SIMPLIFICADO: una falla de BD detiene el avance del grupo hasta recuperarse.
        // Si un flujo necesita seguir durante la caída, usar un tópico de reintentos persistente.
        new FixedBackOff(1000L, FixedBackOff.UNLIMITED_ATTEMPTS));
    errors.addNotRetryableExceptions(IllegalArgumentException.class);
    errors.setRetryListeners((record, ex, intento) -> log.warn("Kafka error topic={} offset={} intento={}",
        record.topic(), record.offset(), intento));
    var factory = new ConcurrentKafkaListenerContainerFactory<String, T>();
    factory.setConsumerFactory(consumers);
    factory.setCommonErrorHandler(errors);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    return factory;
  }
}
