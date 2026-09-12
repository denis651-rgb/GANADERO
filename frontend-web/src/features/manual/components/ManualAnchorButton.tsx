import { Link as LinkIcon } from 'lucide-react'
import { useToast } from '@/shared/toast/useToast'

interface ManualAnchorButtonProps {
  path: string
}

/** Botón "copiar enlace directo a esta sección", compartido por encabezados normales y por cabeceras de acordeón. */
export function ManualAnchorButton({ path }: ManualAnchorButtonProps) {
  const { showToast } = useToast()

  function copyLink() {
    if (!navigator.clipboard) {
      showToast('No se pudo copiar el enlace.', 'danger')
      return
    }
    navigator.clipboard.writeText(path).then(
      () => showToast('Enlace de la sección copiado.'),
      () => showToast('No se pudo copiar el enlace.', 'danger'),
    )
  }

  return (
    <button type="button" className="manual-heading-anchor" onClick={copyLink} aria-label="Copiar enlace a esta sección">
      <LinkIcon size={14} aria-hidden="true" />
    </button>
  )
}
