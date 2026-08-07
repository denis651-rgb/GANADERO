export interface CompressedImage {
  blob: Blob
  fileName: string
  mimeType: string
}

/**
 * Comprime una fotografía en el cliente (Tarea 9.2).
 *
 * - Decodifica con `createImageBitmap` (asíncrono, fuera del hilo principal
 *   y respetando la orientación EXIF de la cámara).
 * - Redimensiona al máximo de ~1280 px en su lado más largo.
 * - Re-encodifica a WebP (o JPEG si el navegador no soporta WebP) con calidad
 *   controlada. Reduce el peso sin perder legibilidad.
 */
export async function compressImage(file: File, maxDimension = 1280, quality = 0.82): Promise<CompressedImage> {
  const bitmap = await createImageBitmap(file)
  try {
    const { width, height } = bitmap
    const scale = Math.min(1, maxDimension / Math.max(width, height))
    const targetWidth = Math.max(1, Math.round(width * scale))
    const targetHeight = Math.max(1, Math.round(height * scale))
    const canvas = document.createElement('canvas')
    canvas.width = targetWidth
    canvas.height = targetHeight
    const context = canvas.getContext('2d')
    if (!context) throw new Error('Canvas no disponible')
    context.drawImage(bitmap, 0, 0, targetWidth, targetHeight)

    const supportsWebp = canvas.toDataURL('image/webp', quality).startsWith('data:image/webp')
    const mimeType = supportsWebp ? 'image/webp' : 'image/jpeg'
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, mimeType, quality))
    if (!blob) throw new Error('No se pudo comprimir la imagen')
    const extension = mimeType === 'image/webp' ? 'webp' : 'jpg'
    const fileName = `${file.name.replace(/\.[^.]+$/, '')}.${extension}`
    return { blob, fileName, mimeType }
  } finally {
    bitmap.close()
  }
}

export async function compressImages(files: File[], maxDimension = 1280, quality = 0.82): Promise<CompressedImage[]> {
  const compressed: CompressedImage[] = []
  for (const file of files) {
    compressed.push(await compressImage(file, maxDimension, quality))
  }
  return compressed
}
