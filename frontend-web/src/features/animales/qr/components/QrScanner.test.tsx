import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { QrScanner } from './QrScanner'

const resolverQr = vi.fn()
vi.mock('@/features/animales/qr/qr-api', () => ({ resolverQr: (...args: unknown[]) => resolverQr(...args) }))
vi.mock('@/features/animales/qr/qr-offline', () => ({ parseQrPayload: vi.fn(), resolveQrOffline: vi.fn() }))

describe('QrScanner', () => {
  beforeEach(() => {
    resolverQr.mockReset()
    Object.defineProperty(HTMLMediaElement.prototype, 'play', { configurable: true, value: vi.fn().mockResolvedValue(undefined) })
  })

  it('muestra Escanear otro para un resultado válido y reinicia la cámara sin conservar el resultado', async () => {
    const stop = vi.fn()
    const stream = { getTracks: () => [{ stop }] } as unknown as MediaStream
    Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: { getUserMedia: vi.fn().mockResolvedValue(stream) } })
    resolverQr.mockResolvedValue({ valid: true, message: 'Animal encontrado', animal: { id: 'a-1', codigo: 'A-001', nombre: 'Luna', estado: 'ACTIVO' } })
    render(<MemoryRouter><QrScanner /></MemoryRouter>)

    fireEvent.change(screen.getByLabelText('Contenido del QR (manual)'), { target: { value: 'payload-válido' } })
    fireEvent.click(screen.getByRole('button', { name: 'Resolver QR' }))
    expect(await screen.findByText('Animal encontrado')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver ficha del animal' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Escanear otro' }))
    await waitFor(() => expect(navigator.mediaDevices.getUserMedia).toHaveBeenCalledOnce())
    expect(screen.queryByText('Animal encontrado')).not.toBeInTheDocument()
    expect(screen.getByText('Apuntando a un código QR de Ganadero…')).toBeInTheDocument()
  })
})
