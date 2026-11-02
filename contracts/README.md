# Contrato multiplataforma

`openapi.yaml` describe un contrato HTTP neutral para catálogo. Sirve de referencia compartida entre Android, iOS, web y backend. En el laboratorio, el SDK Retrofit habla con DummyJSON y adapta su respuesta al modelo de dominio Android; el YAML propone el contrato estable que un backend propio podría ofrecer.

## Cómo usar el contrato en un equipo

1. El equipo acuerda rutas, nombres, paginación, campos requeridos/nullable, formatos, errores y reglas de compatibilidad en esta especificación.
2. Backend revisa que los endpoints implementen el contrato. Android e iOS pueden generar interfaces/modelos con una herramienta compatible con OpenAPI o implementar sus propios DTOs.
3. Cada plataforma mantiene un mapper desde su DTO de transporte a su modelo de dominio. No se comparte el DTO de Retrofit como modelo de UI.
4. CI valida que el YAML sea válido y puede comparar cambios para detectar breaking changes antes de publicar.
5. Cambios aditivos compatibles se documentan y prueban; eliminar/renombrar campos o cambiar su tipo requiere estrategia de compatibilidad, una versión nueva o migración coordinada.

Los clientes generados no sustituyen la revisión del diseño: revisa nullability, tipos numéricos, fechas, enums y errores para que el resultado sea idiomático en Kotlin y Swift. Para producción, cambia `api.example.com`, alinea el schema con el backend real y decide autenticación, límites, retries y política de deprecación. El contrato del ejemplo no representa una API ya desplegada.

## Mapeo conceptual

| OpenAPI | Android del laboratorio | Otro cliente |
|---|---|---|
| `ProductPage` | DTO de respuesta de data | modelo de respuesta Swift/web |
| `Product` | DTO y luego `domain.Product` | DTO/modelo de dominio de esa plataforma |
| `ApiError` | error normalizado del data layer | error de aplicación específico |
| `limit`, `skip` | parámetros Retrofit | parámetros equivalentes |
