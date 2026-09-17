import { useState } from 'react'
import { Modal } from '@/shared/components/Modal'

interface ManualImageProps {
  src?: string
  alt?: string
}

/** Imagen del manual: clic para verla en tamaño completo. */
export function ManualImage({ src, alt }: ManualImageProps) {
  const [open, setOpen] = useState(false)
  if (!src) return null
  return (
    <>
      {/* Sin loading="lazy" a propósito: la ventana oculta que genera el PDF del manual
          (electron/src/manual-export.ts) no tiene viewport real, así que el lazy-load nativo
          del navegador nunca dispara ahí y la imagen queda sin cargar en el PDF. */}
      <button type="button" className="manual-image-trigger" onClick={() => setOpen(true)}>
        <img src={src} alt={alt ?? ''} />
      </button>
      <Modal open={open} title={alt || 'Imagen ampliada'} onClose={() => setOpen(false)} wide>
        <img src={src} alt={alt ?? ''} className="manual-image-full" />
      </Modal>
    </>
  )
}
