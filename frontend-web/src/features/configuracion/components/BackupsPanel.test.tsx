import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { BackupInfo, BackupSettings, BackupsDesktopBridge, DiagnosticsDesktopBridge } from '@/shared/api/http'
import { BackupsPanel } from './BackupsPanel'

vi.mock('@/shared/toast/useToast', () => ({ useToast: () => ({ showToast: vi.fn() }) }))

const settings: BackupSettings = {
  version: 1, automatico: true, frecuencia: 'DIARIA', hora: '20:00',
  destinoLocal: 'C\\Users\\test\\AppData\\Roaming\\Ganadero\\backups', destinoExterno: null,
  retencionDiarios: 7, retencionSemanales: 4, retencionMensuales: 12, ultimoRespaldo: null,
}

const backup: BackupInfo = {
  nombreArchivo: 'Ganadero_2026-09-06_20-00-00.ganadero-backup', fechaCreacion: '2026-09-06T20:00:00Z',
  tamanoBytes: 2048, hashSha256: 'abc', estado: 'CREADO_LOCALMENTE', integridad: 'VALIDA',
}

function mockBridge(overrides: Partial<BackupsDesktopBridge> = {}): BackupsDesktopBridge {
  return {
    backendStatus: 'ok',
    getSettings: vi.fn().mockResolvedValue(settings),
    saveSettings: vi.fn().mockResolvedValue(settings),
    selectExternalFolder: vi.fn().mockResolvedValue(null),
    createNow: vi.fn().mockResolvedValue(backup),
    copyExternal: vi.fn().mockResolvedValue(undefined),
    list: vi.fn().mockResolvedValue([backup]),
    verify: vi.fn().mockResolvedValue(backup),
    deleteBackup: vi.fn().mockResolvedValue(undefined),
    selectRestoreFile: vi.fn().mockResolvedValue(null),
    inspectRestoreFile: vi.fn().mockResolvedValue({ manifest: { formato: 'GANADERO_BACKUP', versionFormato: 1, versionAplicacion: '0.0.1', versionBaseDatos: '37', fechaCreacion: '2026-09-06T20:00:00Z', zonaHoraria: 'America/La_Paz', empresaId: null, archivoInterno: 'database/ganadero.db', tamanoBytes: 2048, hashSha256: 'abc' } }),
    restore: vi.fn().mockResolvedValue({ ok: true, mensaje: 'Restaurado.' }),
    openLocalFolder: vi.fn().mockResolvedValue(undefined),
    openExternalFolder: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  }
}

function mockDiagnosticsBridge(overrides: Partial<DiagnosticsDesktopBridge> = {}): DiagnosticsDesktopBridge {
  return {
    export: vi.fn().mockResolvedValue({ cancelado: false, path: 'C:\\diagnostico.zip' }),
    openLogsFolder: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  }
}

function renderPanel(bridge?: BackupsDesktopBridge, diagnostics: DiagnosticsDesktopBridge = mockDiagnosticsBridge()) {
  ;(window as unknown as { ganadero: unknown }).ganadero = bridge ? { backups: bridge, diagnostics } : undefined
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(<QueryClientProvider client={client}><BackupsPanel /></QueryClientProvider>)
}

afterEach(() => {
  ;(window as unknown as { ganadero: unknown }).ganadero = undefined
})

describe('BackupsPanel', () => {
  it('muestra un aviso cuando no corre dentro de Ganadero Desktop', () => {
    renderPanel(undefined)
    expect(screen.getByText(/Ganadero Desktop/)).toBeInTheDocument()
  })

  it('carga la configuración y el historial', async () => {
    renderPanel(mockBridge())
    await screen.findByText(backup.nombreArchivo)
    expect(screen.getByText('Diaria a las 20:00')).toBeInTheDocument()
  })

  it('guarda la configuración al enviar el formulario', async () => {
    const bridge = mockBridge()
    renderPanel(bridge)
    await screen.findByText(backup.nombreArchivo)

    fireEvent.click(screen.getByRole('button', { name: 'Guardar configuración' }))

    await waitFor(() => expect(bridge.saveSettings).toHaveBeenCalledWith(expect.objectContaining({ hora: '20:00' })))
  })

  it('crea un respaldo al hacer click en "Crear respaldo ahora"', async () => {
    const bridge = mockBridge()
    renderPanel(bridge)
    await screen.findByText(backup.nombreArchivo)

    fireEvent.click(screen.getByRole('button', { name: /Crear respaldo ahora/ }))

    await waitFor(() => expect(bridge.createNow).toHaveBeenCalled())
  })

  it('exige escribir RESTAURAR antes de habilitar la confirmación de restauración', async () => {
    const bridge = mockBridge({ selectRestoreFile: vi.fn().mockResolvedValue('C:\\backups\\x.ganadero-backup') })
    renderPanel(bridge)
    await screen.findByText(backup.nombreArchivo)

    fireEvent.click(screen.getByRole('button', { name: /Restaurar respaldo/ }))
    await screen.findByText('Restaurar base de datos')

    const confirmar = screen.getByRole('button', { name: 'Restaurar' })
    expect(confirmar).toBeDisabled()

    fireEvent.change(screen.getByLabelText(/Escribe RESTAURAR/), { target: { value: 'RESTAURAR' } })
    expect(confirmar).not.toBeDisabled()

    fireEvent.click(confirmar)
    await waitFor(() => expect(bridge.restore).toHaveBeenCalledWith('C:\\backups\\x.ganadero-backup'))
  })

  it('elimina un respaldo desde el historial tras confirmar', async () => {
    const bridge = mockBridge()
    renderPanel(bridge)
    await screen.findByText(backup.nombreArchivo)

    fireEvent.click(screen.getByRole('button', { name: `Eliminar ${backup.nombreArchivo}` }))
    await screen.findByText('Eliminar respaldo')
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar' }))

    await waitFor(() => expect(bridge.deleteBackup).toHaveBeenCalledWith(backup.nombreArchivo))
  })

  it('exporta la información de diagnóstico y muestra dónde se guardó', async () => {
    const diagnostics = mockDiagnosticsBridge()
    renderPanel(mockBridge(), diagnostics)
    await screen.findByText(backup.nombreArchivo)

    fireEvent.click(screen.getByRole('button', { name: /Exportar información de diagnóstico/ }))

    await waitFor(() => expect(diagnostics.export).toHaveBeenCalled())
    await screen.findByText(/C:\\diagnostico\.zip/)
  })

  it('abre la carpeta de logs al hacer click', async () => {
    const diagnostics = mockDiagnosticsBridge()
    renderPanel(mockBridge(), diagnostics)
    await screen.findByText(backup.nombreArchivo)

    fireEvent.click(screen.getByRole('button', { name: /Abrir carpeta de logs/ }))

    await waitFor(() => expect(diagnostics.openLogsFolder).toHaveBeenCalled())
  })
})
