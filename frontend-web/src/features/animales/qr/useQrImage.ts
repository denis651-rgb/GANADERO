import { useEffect, useState } from 'react'
import { normalizeApiError } from '@/shared/api/errors'
import { fetchQrImage } from '@/features/animales/qr/qr-api'
import type { QrImageFormat, QrImageSize } from '@/features/animales/qr/qr-types'

interface QrImageState {
  loading: boolean
  blobUrl: string | null
  error: string | null
}

export function useQrImage(animalId: string, identificadorId: string, format: QrImageFormat = 'png', size: QrImageSize = 512) {
  const [state, setState] = useState<QrImageState>({ loading: true, blobUrl: null, error: null })

  useEffect(() => {
    let cancelled = false
    let objectUrl: string | null = null
    fetchQrImage(animalId, identificadorId, { format, size })
      .then((blob) => {
        if (cancelled) return
        objectUrl = URL.createObjectURL(blob)
        setState({ loading: false, blobUrl: objectUrl, error: null })
      })
      .catch((reason) => {
        if (!cancelled) setState({ loading: false, blobUrl: null, error: normalizeApiError(reason).message })
      })
    return () => {
      cancelled = true
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [animalId, identificadorId, format, size])

  return state
}
