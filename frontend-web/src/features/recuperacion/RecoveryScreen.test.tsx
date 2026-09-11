import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { BackupInfo, BackupsDesktopBridge } from '@/shared/api/http'
import { RecoveryScreen } from './RecoveryScreen'

const settings = {
  version: 1 as const, automatico: false, frecuencia: 'DIARIA' as const, hora: '20:00',
  destinoLocal: 'C:\\backups', destinoExterno: null,
  retencionDiarios: 7, retencionSemanales: 4, retencionMensuales: 12, ultimoRespaldo: null,
}

const respaldoValido: BackupInfo = {
  nombreArchivo: 'Ganadero_2026-09-06_20-00-00.ganadero-backup', fechaCreacion: '2026-09-06T20:00:00Z',
  hashSha256: 'abc', estado: 'CREADO_LOCALMENTE', integridad: 'DESCONOCIDA',
}

function mockBridge(overrides: Partial<BackupsDesktopBridge> = {}): BackupsDesktopBridge {
  return {
    backendStatus: 'failed',
    getSettings: vi.fn().mockResolvedValue(settings),
    saveSettings: vi.fn(),
    selectExternalFolder: vi.fn(),
    createNow: vi.fn(),
    copyExternal: vi.fn(),
    list: vi.fn().mockResolvedValue([respaldoValido]),
    verify: vi.fn().mockResolvedValue({ ...respaldoValido, integridad: 'VALIDA' }),
    deleteBackup: vi.fn(),
    selectRestoreFile: vi.fn().mockResolvedValue(null),
    inspectRestoreFile: vi.fn(),
    restore: vi.fn().mockResolvedValue({ ok: true, mensaje: 'Base restaurada correctamente.' }),
    openLocalFolder: vi.fn().mockResolvedValue(undefined),
    openExternalFolder: vi.fn(),
    ...overrides,
  }
}

function renderScreen(bridge: BackupsDesktopBridge) {
  ;(window as unknown as { ganadero: unknown }).ganadero = { backups: bridge }
  return render(<RecoveryScreen />)
}

afterEach(() => {
  ;(window as unknown as { ganadero: unknown }).ganadero = undefined
  vi.restoreAllMocks()
})

describe('RecoveryScreen', () => {
  it('busca y restaura el último respaldo con integridad válida, previa confirmación', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const bridge = mockBridge()
    renderScreen(bridge)

    fireEvent.click(await screen.findByRole('button', { name: /Restaurar el último respaldo válido/ }))

    await waitFor(() => expect(bridge.restore).toHaveBeenCalledWith(`C:\\backups\\${respaldoValido.nombreArchivo}`))
    expect(await screen.findByText('Base restaurada correctamente.')).toBeInTheDocument()
  })

  it('no restaura si el usuario cancela la confirmación nativa', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    const bridge = mockBridge()
    renderScreen(bridge)

    fireEvent.click(await screen.findByRole('button', { name: /Restaurar el último respaldo válido/ }))

    await waitFor(() => expect(bridge.verify).toHaveBeenCalled())
    expect(bridge.restore).not.toHaveBeenCalled()
  })

  it('avisa cuando no hay ningún respaldo con integridad válida', async () => {
    const bridge = mockBridge({ verify: vi.fn().mockResolvedValue({ ...respaldoValido, integridad: 'INVALIDA' }) })
    renderScreen(bridge)

    fireEvent.click(await screen.findByRole('button', { name: /Restaurar el último respaldo válido/ }))

    expect(await screen.findByText(/no se encontró ningún respaldo con integridad válida/i)).toBeInTheDocument()
    expect(bridge.restore).not.toHaveBeenCalled()
  })

  it('cierra Ganadero de verdad, no solo oculta la ventana', async () => {
    const quit = vi.fn()
    const bridge = mockBridge()
    renderScreen(bridge)
    ;(window.ganadero as unknown as { quit: () => void }).quit = quit

    fireEvent.click(await screen.findByRole('button', { name: /Cerrar Ganadero/ }))

    expect(quit).toHaveBeenCalled()
  })
})
