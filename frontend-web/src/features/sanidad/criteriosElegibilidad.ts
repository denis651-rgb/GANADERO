/**
 * Nombres de las categorías a las que aplica una actividad del plan, para mostrarlos en los criterios de
 * elegibilidad de una jornada. Es la misma lista (`categoriasAplicables`) con la que el servidor decide
 * qué animales son elegibles; sin categorías elegidas la actividad aplica a todas.
 */
export function categoriasDeLaActividad(
  categoriaIds: readonly string[] | undefined,
  catalogo: ReadonlyArray<{ id: string; nombre: string }>,
): string[] {
  return (categoriaIds ?? []).map((id) => catalogo.find((categoria) => categoria.id === id)?.nombre ?? 'Categoría no disponible')
}
