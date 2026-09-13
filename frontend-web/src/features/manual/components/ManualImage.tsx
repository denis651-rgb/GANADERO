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
      <button type="button" className="manual-image-trigger" onClick={() => setOpen(true)}>
        <img src={src} alt={alt ?? ''} loading="lazy" />
      </button>
      <Modal open={open} title={alt || 'Imagen ampliada'} onClose={() => setOpen(false)} wide>
        <img src={src} alt={alt ?? ''} className="manual-image-full" />
      </Modal>
    </>
  )
}
