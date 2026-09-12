import { app, dialog } from 'electron'
import { autoUpdater } from 'electron-updater'

// Distingue la pasada silenciosa al arrancar (solo avisa si SÍ hay algo nuevo) de un click manual
// en "Buscar actualizaciones" del menú de la bandeja (avisa también si no hay nada o si falla).
let comprobacionManualEnCurso = false

/**
 * Publica el instalador en GitHub Releases (owner/repo en electron-builder.yml → publish); cada
 * cliente instalado lee esas releases públicas sin necesitar token. Antes de esto, cada versión
 * nueva exigía reinstalar a mano — ver electron-builder.yml para cómo se publica.
 */
export function initAutoUpdater(): void {
  // En dev no existe instalador ni latest.yml publicados; checkForUpdates() fallaría sin aportar nada.
  if (!app.isPackaged) return

  autoUpdater.autoDownload = true
  autoUpdater.autoInstallOnAppQuit = true

  autoUpdater.on('error', (error) => {
    console.error('[updater] error buscando actualizaciones:', error)
    if (comprobacionManualEnCurso) {
      comprobacionManualEnCurso = false
      void dialog.showMessageBox({ type: 'error', title: 'Buscar actualizaciones', message: 'No se pudo comprobar si hay una versión nueva.', detail: error.message })
    }
  })
  autoUpdater.on('update-available', (info) => console.log(`[updater] actualización disponible: ${info.version}`))
  autoUpdater.on('update-not-available', () => {
    console.log('[updater] no hay actualizaciones disponibles')
    if (comprobacionManualEnCurso) {
      comprobacionManualEnCurso = false
      void dialog.showMessageBox({ type: 'info', title: 'Buscar actualizaciones', message: 'Ya tienes la última versión de Ganadero instalada.' })
    }
  })
  autoUpdater.on('update-downloaded', (info) => {
    comprobacionManualEnCurso = false
    console.log(`[updater] actualización ${info.version} descargada`)
    void dialog.showMessageBox({
      type: 'info',
      title: 'Actualización disponible',
      message: `Ganadero ${info.version} está lista para instalarse.`,
      detail: 'Se instalará sola la próxima vez que cierres la aplicación, o puedes reiniciar ahora para aplicarla de una vez.',
      buttons: ['Reiniciar ahora', 'Más tarde'],
      defaultId: 1,
      cancelId: 1,
    }).then((resultado) => {
      if (resultado.response === 0) autoUpdater.quitAndInstall()
    })
  })

  verificarActualizaciones()
}

function verificarActualizaciones(): void {
  autoUpdater.checkForUpdates().catch((error) => console.error('[updater] no se pudo buscar actualizaciones:', error))
}

/** Para el ítem "Buscar actualizaciones" del menú de la bandeja: a diferencia de la comprobación
 * silenciosa al arrancar, esta siempre da una respuesta visible (encontró algo, no hay nada, o falló). */
export function buscarActualizacionesManualmente(): void {
  if (!app.isPackaged) {
    void dialog.showMessageBox({ type: 'info', title: 'Buscar actualizaciones', message: 'La búsqueda de actualizaciones solo está disponible en la aplicación instalada.' })
    return
  }
  comprobacionManualEnCurso = true
  verificarActualizaciones()
}
