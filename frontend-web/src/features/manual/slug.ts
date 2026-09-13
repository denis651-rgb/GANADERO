/** Slug estable para anclar encabezados: minúsculas, sin tildes, con guiones. */
export function slugify(text: string): string {
  return text
    .toLocaleLowerCase('es-BO')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9\s-]/g, '')
    .trim()
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
}
