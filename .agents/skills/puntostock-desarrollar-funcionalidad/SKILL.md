---
name: puntostock-desarrollar-funcionalidad
description: Implementa o corrige funcionalidades de PuntoStock siguiendo sus capas Spring, contratos y componentes Angular. Úsalo para cambios de comportamiento, pantallas o API del proyecto, especialmente catálogo, ventas e inventario.
---

# Desarrollar funcionalidades de PuntoStock

## Ubicar el cambio

- Lee la implementación actual y las pruebas relacionadas antes de diseñar. Consulta docs/api.md, docs/arquitectura.md o docs/caja.md solo cuando correspondan a la funcionalidad.
- Sigue la separación existente: controllers HTTP en backend/src/main/java/sv/puntostock/api, contratos de aplicación en application, casos de uso en service y persistencia en repository/entity. No recrees OperacionService ni ApiController por aparecer en documentación antigua.
- En Angular reutiliza servicios de dominio y componentes existentes; mantén contratos de API compatibles y estados de carga, vacío y error recuperable.
- Mantén el cálculo monetario definitivo en servidor, las transacciones y los mecanismos de reintento de cobro. No añadas otra vía independiente de actualización de existencias.
- Para cambios de esquema, sigue la ubicación y numeración de Flyway vigentes. No edites migraciones ya aplicadas para introducir un cambio nuevo.

## Regla de stock mínimo

- Mostrar bajo stock cuando un artículo tenga existencias menores o iguales a su stockMinimo. El valor inicial actual de artículos es 5: con ese mínimo, 5, 4 y 0 deben aparecer en la alerta; 6 no.
- Respetar el mínimo configurado de cada producto. No sustituirlo por un 5 fijo en filtros o cálculos. Los servicios no participan en alertas de existencias.
- Ya existe lógica de bajo stock en frontend/src/app/features/dashboard.component.ts. Reutilizarla o ajustarla dentro del alcance solicitado, sin crear avisos duplicados.
- Esta regla describe comportamiento que debe implementarse y comprobarse en código; el archivo de skill no genera alertas durante el uso de la aplicación.

## Venta por caja y por unidad

- Cuando un producto permita vender cajas completas, calcular las cajas disponibles como la parte entera de las unidades disponibles divididas entre las unidades por caja. Descontar también las unidades y cajas del mismo producto ya incluidas en el carrito.
- Permitir agregar o cobrar una caja solo si quedan suficientes unidades para completarla. Ejemplo: con 12 unidades por caja y 12 unidades disponibles se puede vender una caja; al vender o reservar una unidad quedan 11 y solo se permite vender por unidad. Con 24 unidades, vender una unidad deja una caja completa y 11 unidades sueltas.
- Recalcular la disponibilidad al modificar el carrito y después de las ventas o entradas de inventario. Validar nuevamente en el servidor con el stock vigente y dentro de la transacción; no confiar únicamente en el botón deshabilitado.
- Mantener un inventario común expresado en unidades individuales, sin duplicar existencias para cajas. Comprobar los límites de una caja exacta, una unidad menos, ventas mixtas y recuperación de disponibilidad al quitar unidades del carrito.

## Cerrar el cambio

- Añade o ajusta pruebas de comportamiento cuando el cambio lo justifique, siguiendo las herramientas actuales. Comprueba los límites relevantes; para modificar la alerta, incluir mínimo distinto de 5 y exclusión de servicios.
- Actualiza contratos o documentación afectados. Resume qué cambió, cómo se comprobó y qué quedó sin verificar.
- No conviertas una funcionalidad concreta en una reescritura o cambio de infraestructura no solicitado.
