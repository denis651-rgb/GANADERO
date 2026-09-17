import { app, BrowserWindow, dialog } from 'electron'
import fs from 'node:fs/promises'
import path from 'node:path'
import { FRONTEND_SCHEME } from './frontend-protocol'
import { DEV_SERVER_URL } from './constants'

export interface ManualExportResult {
  ok: boolean
  cancelled?: boolean
  path?: string
  message?: string
}

const LOAD_TIMEOUT_MS = 20_000
const DEFAULT_VERSION = '1.0.0'

function footerTemplate(): string {
  // Clases documentadas por Electron/CDP: la sustitución real la hace Chromium al generar el PDF.
  return `
    <div style="width:100%; font-size:8px; color:#667; text-align:center; padding:0 12mm; font-family:sans-serif;">
      Manual de usuario de Ganadero &middot; <span class="date"></span> &middot;
      Página <span class="pageNumber"></span> de <span class="totalPages"></span>
    </div>
  `
}

/** Espera a que las fuentes y todas las <img> terminen de cargar (o fallen) antes de imprimir. */
async function waitForContentReady(win: BrowserWindow): Promise<void> {
  await win.webContents.executeJavaScript(`
    (async () => {
      if (document.fonts && document.fonts.ready) { await document.fonts.ready }
      const imgs = Array.from(document.images)
      await Promise.all(imgs.map((img) => img.complete ? Promise.resolve() : new Promise((resolve) => {
        img.addEventListener('load', resolve, { once: true })
        img.addEventListener('error', resolve, { once: true })
      })))
    })()
  `)
}

function resolvePrintUrl(): string {
  return app.isPackaged ? `${FRONTEND_SCHEME}://index.html/manual/imprimir` : `${DEV_SERVER_URL}/manual/imprimir`
}

function loadPrintWindow(win: BrowserWindow): Promise<void> {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      reject(new Error('La ventana de impresión no respondió a tiempo.'))
    }, LOAD_TIMEOUT_MS)
    win.webContents.once('did-finish-load', () => {
      clearTimeout(timeout)
      resolve()
    })
    win.webContents.once('did-fail-load', (_event, _code, description) => {
      clearTimeout(timeout)
      reject(new Error(`No se pudo cargar el manual para imprimir (${description}).`))
    })
    win.loadURL(resolvePrintUrl()).catch((error: unknown) => {
      clearTimeout(timeout)
      reject(error instanceof Error ? error : new Error(String(error)))
    })
  })
}

/**
 * Genera el PDF completo del manual: abre una ventana oculta, carga /manual/imprimir (ver
 * ManualPrintPage.tsx), espera imágenes y fuentes, y usa printToPDF(). El renderer nunca escribe
 * el archivo — solo pide la operación por IPC (ver preload.ts `window.ganadero.manual.exportPdf`).
 */
export async function exportManualPdf(getBackendPort: () => number): Promise<ManualExportResult> {
  const win = new BrowserWindow({
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      additionalArguments: [
        `--ganadero-api-base-url=http://127.0.0.1:${getBackendPort()}`,
        '--ganadero-backend-status=ok',
      ],
      contextIsolation: true,
      nodeIntegration: false,
    },
  })

  try {
    await loadPrintWindow(win)
    await waitForContentReady(win)

    const version = win.webContents.getTitle().match(/v([\d.]+)\s*$/)?.[1] ?? DEFAULT_VERSION

    const pdfBuffer = await win.webContents.printToPDF({
      printBackground: true,
      pageSize: 'A4',
      margins: { top: 0.6, bottom: 0.6, left: 0.6, right: 0.6 },
      displayHeaderFooter: true,
      headerTemplate: '<span></span>',
      footerTemplate: footerTemplate(),
    })

    const { canceled, filePath } = await dialog.showSaveDialog({
      title: 'Guardar manual en PDF',
      defaultPath: `Manual-Ganadero-v${version}.pdf`,
      filters: [{ name: 'PDF', extensions: ['pdf'] }],
    })
    if (canceled || !filePath) return { ok: false, cancelled: true }

    await fs.writeFile(filePath, pdfBuffer)
    return { ok: true, path: filePath }
  } catch (error) {
    return { ok: false, message: error instanceof Error ? error.message : 'No se pudo generar el PDF del manual.' }
  } finally {
    win.destroy()
  }
}
