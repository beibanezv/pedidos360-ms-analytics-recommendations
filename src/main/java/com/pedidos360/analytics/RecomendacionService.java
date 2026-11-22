package com.pedidos360.analytics;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
class RecomendacionService {
  private final EventoRepository repository;
  private final CatalogoClient catalogo;

  RecomendacionService(EventoRepository repository, CatalogoClient catalogo) {
    this.repository = repository;
    this.catalogo = catalogo;
  }

  List<CatalogoClient.Producto> para(String usuario) {
    var historial = repository.productosVistos(usuario);
    if (historial.isEmpty()) return List.of();
    var productos = catalogo.productos();
    var categorias = new java.util.HashMap<java.util.UUID, String>();
    for (var p : productos) if (p.categoria() != null) categorias.put(p.id(), p.categoria());
    String categoria = historial.stream().map(categorias::get).filter(Objects::nonNull).findFirst().orElse(null);
    if (categoria == null) return List.of();
    var vistos = new HashSet<>(historial);
    return productos.stream().filter(p -> p.stock() > 0 && categoria.equals(p.categoria()) && !vistos.contains(p.id()))
        .sorted(Comparator.comparing(CatalogoClient.Producto::nombre, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(CatalogoClient.Producto::id))
        .limit(4).toList();
  }
}
