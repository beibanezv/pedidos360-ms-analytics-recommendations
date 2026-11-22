package com.pedidos360.analytics;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

interface EventoRepository extends JpaRepository<Evento, UUID> {
  @Modifying @Transactional
  @Query(value = """
      insert into analytics_eventos (id, usuario_id, tipo, producto_id, orden_id, total_clp, ocurrio_en)
      values (:id, :usuario, :tipo, :producto, :orden, :total, :cuando)
      on conflict (id) do nothing
      """, nativeQuery = true)
  int registrar(UUID id, String usuario, String tipo, UUID producto, UUID orden, Long total, Instant cuando);

  @Query(value = """
      select producto_id from analytics_eventos
      where usuario_id = :usuario and tipo = 'DETALLE_PRODUCTO' and producto_id is not null
      group by producto_id order by max(ocurrio_en) desc, producto_id
      """, nativeQuery = true)
  List<UUID> productosVistos(String usuario);

  @Query("""
      select new com.pedidos360.analytics.Metricas(
        count(case when e.tipo = 'DETALLE_PRODUCTO' then 1 end),
        count(case when e.tipo = 'AGREGAR_CARRITO' then 1 end),
        count(case when e.tipo = 'APROBADO' then 1 end),
        count(case when e.tipo = 'BLOQUEADO' then 1 end))
      from Evento e where e.ocurrioEn >= :desde
      """)
  Metricas metricas(Instant desde);
}
