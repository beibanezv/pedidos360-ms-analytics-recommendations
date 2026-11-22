package com.pedidos360.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RecomendacionServiceTest {
  @Test
  void recomiendaLaCategoriaRecienteSinRepetirVisitadosNiAgotados() {
    var visitado = producto("Primero", "vinilo", 4);
    var viejo = producto("Visto antes", "tornamesa", 4);
    var afin = producto("Otro vinilo", "vinilo", 9);
    var sinStock = producto("Agotado", "vinilo", 0);
    var otraCategoria = producto("Otro equipo", "tornamesa", 9);
    var repository = Mockito.mock(EventoRepository.class);
    var catalogo = Mockito.mock(CatalogoClient.class);
    when(repository.productosVistos("a")).thenReturn(List.of(visitado.id(), viejo.id()));
    when(repository.productosVistos("b")).thenReturn(List.of(viejo.id()));
    when(catalogo.productos()).thenReturn(List.of(visitado, viejo, afin, sinStock, otraCategoria));
    var servicio = new RecomendacionService(repository, catalogo);

    assertThat(servicio.para("a")).containsExactly(afin);
    assertThat(servicio.para("b")).containsExactly(otraCategoria);
    assertThat(servicio.para("nuevo")).isEmpty();
  }

  private static CatalogoClient.Producto producto(String nombre, String categoria, int stock) {
    return new CatalogoClient.Producto(UUID.randomUUID(), nombre, "Artista", categoria, 18990L, stock, null);
  }
}
