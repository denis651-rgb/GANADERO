import { app, net, protocol } from 'electron'
import path from 'node:path'
import fs from 'node:fs'
import { pathToFileURL } from 'node:url'

export const FRONTEND_SCHEME = 'app'

/** Debe llamarse antes de app.whenReady(). */
export function registerFrontendScheme(): void {
  protocol.registerSchemesAsPrivileged([
    { scheme: FRONTEND_SCHEME, privileges: { standard: true, secure: true, supportFetchAPI: true, corsEnabled: true } },
  ])
}

/** Sirve el build estático del frontend (SPA) desde resources, con fallback a index.html. */
export function serveFrontend(): void {
  const root = app.isPackaged
    ? path.join(process.resourcesPath, 'frontend')
    : path.join(__dirname, '..', '..', 'frontend-web', 'dist')

  protocol.handle(FRONTEND_SCHEME, (request) => {
    const url = new URL(request.url)
    let filePath = path.join(root, decodeURIComponent(url.pathname))
    if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
      filePath = path.join(root, 'index.html')
    }
    return net.fetch(pathToFileURL(filePath).toString())
  })
}
