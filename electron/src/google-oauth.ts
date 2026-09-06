import { app, safeStorage, shell } from 'electron'
import { createHash, randomBytes, timingSafeEqual } from 'node:crypto'
import { promises as fs } from 'node:fs'
import { createServer, type Server } from 'node:http'
import path from 'node:path'

const AUTH_URL = 'https://accounts.google.com/o/oauth2/v2/auth'
const TOKEN_URL = 'https://oauth2.googleapis.com/token'
const REVOKE_URL = 'https://oauth2.googleapis.com/revoke'
const USERINFO_URL = 'https://openidconnect.googleapis.com/v1/userinfo'
const SCOPES = ['openid', 'email', 'https://www.googleapis.com/auth/calendar.app.created']
// Google admite el puerto dinámico para clientes Desktop, pero valida la ruta del
// loopback. La raíz coincide con el redirect URI recomendado para apps instaladas.
const CALLBACK_PATH = '/'
const FLOW_TIMEOUT_MS = 5 * 60_000

interface OAuthClientConfig { clientId: string; clientSecret?: string }
interface StoredTokens {
  accessToken: string
  refreshToken?: string
  expiresAt: number
  scope: string
  tokenType: string
  email: string
  subject?: string
}
interface TokenResponse {
  access_token: string
  refresh_token?: string
  expires_in: number
  scope: string
  token_type: string
}

export interface GoogleOAuthStatus {
  available: boolean
  connected: boolean
  email?: string
  expiresAt?: string
  message?: string
}

export class GoogleOAuthManager {
  private activeFlow: Promise<GoogleOAuthStatus> | null = null

  constructor(private readonly getBackendPort: () => number) {}

  async status(): Promise<GoogleOAuthStatus> {
    const config = await this.readClientConfig()
    if (!config) return { available: false, connected: false, message: this.configurationHelp() }
    const tokens = await this.loadTokens()
    return tokens
      ? { available: true, connected: true, email: tokens.email, expiresAt: new Date(tokens.expiresAt).toISOString() }
      : { available: true, connected: false }
  }

  connect(): Promise<GoogleOAuthStatus> {
    if (this.activeFlow) return this.activeFlow
    this.activeFlow = this.authorize().finally(() => { this.activeFlow = null })
    return this.activeFlow
  }

  async importClientConfig(jsonText: string, fileName: string): Promise<GoogleOAuthStatus> {
    if (await this.loadTokens())
      throw new Error('Revoca la cuenta conectada antes de reemplazar el cliente OAuth.')
    if (!fileName.toLocaleLowerCase().endsWith('.json'))
      throw new Error('Selecciona un archivo con extensión .json.')
    if (Buffer.byteLength(jsonText, 'utf8') > 64 * 1024)
      throw new Error('El archivo OAuth supera el tamaño permitido de 64 KB.')
    let parsed:Record<string,unknown>
    try { parsed=JSON.parse(jsonText) as Record<string,unknown> }
    catch { throw new Error('El archivo seleccionado no contiene JSON válido.') }
    if (parsed.web && !parsed.installed)
      throw new Error('Este cliente OAuth es de tipo Web. Descarga uno de tipo Aplicación de escritorio.')
    const installed=(parsed.installed ?? parsed) as Record<string,unknown>
    const clientId=typeof installed.client_id==='string'?installed.client_id.trim():''
    const clientSecret=typeof installed.client_secret==='string'?installed.client_secret.trim():''
    if(!clientId.endsWith('.apps.googleusercontent.com'))
      throw new Error('El JSON no corresponde a un cliente OAuth de escritorio de Google.')
    const normalized={ installed:{ client_id:clientId,...(clientSecret?{client_secret:clientSecret}:{}) } }
    const target=path.join(app.getPath('userData'),'google-oauth-client.json')
    const temporary=`${target}.tmp`
    await fs.mkdir(path.dirname(target),{ recursive:true })
    await fs.writeFile(temporary,JSON.stringify(normalized,null,2),{ encoding:'utf8',mode:0o600 })
    await fs.copyFile(temporary,target)
    await fs.rm(temporary,{ force:true })
    return { available:true,connected:false,message:'Cliente OAuth importado correctamente. Ya puedes conectar Google.' }
  }

  /** Devuelve un token vigente sólo al proceso principal; nunca se expone mediante preload/IPC. */
  async accessToken(): Promise<string> {
    const stored = await this.loadTokens()
    if (!stored) throw new Error('Google Calendar no está autorizado.')
    if (stored.expiresAt > Date.now() + 60_000) return stored.accessToken
    if (!stored.refreshToken) throw new Error('La autorización expiró y Google no entregó un token de renovación.')
    const config = await this.readClientConfig()
    if (!config) throw new Error(this.configurationHelp())
    const body = new URLSearchParams({
      client_id: config.clientId,
      refresh_token: stored.refreshToken,
      grant_type: 'refresh_token',
    })
    if (config.clientSecret) body.set('client_secret',config.clientSecret)
    const response = await fetch(TOKEN_URL,{
      method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body,
    })
    if (!response.ok) throw new Error(`Google rechazó la renovación OAuth (${response.status}).`)
    const refreshed = await response.json() as TokenResponse
    const updated: StoredTokens = {
      ...stored,
      accessToken:refreshed.access_token,
      refreshToken:refreshed.refresh_token ?? stored.refreshToken,
      expiresAt:Date.now()+refreshed.expires_in*1000,
      scope:refreshed.scope ?? stored.scope,
      tokenType:refreshed.token_type ?? stored.tokenType,
    }
    await this.saveTokens(updated)
    return updated.accessToken
  }

  async changeAccount(): Promise<GoogleOAuthStatus> {
    await this.revoke()
    return this.connect()
  }

  async revoke(): Promise<GoogleOAuthStatus> {
    const tokens = await this.loadTokens()
    let warning: string | undefined
    if (tokens) {
      const token = tokens.refreshToken ?? tokens.accessToken
      try {
        const response = await fetch(REVOKE_URL, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({ token }),
        })
        if (!response.ok && response.status !== 400) warning = `Google respondió ${response.status} al revocar.`
      } catch {
        warning = 'No se pudo contactar a Google; las credenciales locales fueron eliminadas.'
      }
    }
    await this.deleteTokens()
    await this.notifyBackend('/api/v1/integraciones/calendario/electron/oauth/revocada', {})
    return { available: Boolean(await this.readClientConfig()), connected: false, message: warning }
  }

  private async authorize(): Promise<GoogleOAuthStatus> {
    const config = await this.readClientConfig()
    if (!config) throw new Error(this.configurationHelp())
    if (!await safeStorage.isAsyncEncryptionAvailable())
      throw new Error('El almacenamiento seguro del sistema operativo no está disponible.')

    const verifier = base64Url(randomBytes(64))
    const challenge = base64Url(createHash('sha256').update(verifier).digest())
    const state = base64Url(randomBytes(32))
    const callback = await this.listenForCallback(state)
    const redirectUri = `http://127.0.0.1:${callback.port}${CALLBACK_PATH}`
    const url = new URL(AUTH_URL)
    url.search = new URLSearchParams({
      client_id: config.clientId,
      redirect_uri: redirectUri,
      response_type: 'code',
      scope: SCOPES.join(' '),
      code_challenge: challenge,
      code_challenge_method: 'S256',
      state,
      access_type: 'offline',
      prompt: 'consent select_account',
      include_granted_scopes: 'true',
    }).toString()

    try {
      await shell.openExternal(url.toString())
      const code = await callback.code
      const tokenBody = new URLSearchParams({
        client_id: config.clientId,
        code,
        code_verifier: verifier,
        grant_type: 'authorization_code',
        redirect_uri: redirectUri,
      })
      if (config.clientSecret) tokenBody.set('client_secret', config.clientSecret)
      const tokenResponse = await fetch(TOKEN_URL, {
        method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: tokenBody,
      })
      if (!tokenResponse.ok) throw new Error(`Google rechazó el intercambio OAuth (${tokenResponse.status}).`)
      const token = await tokenResponse.json() as TokenResponse
      const identityResponse = await fetch(USERINFO_URL, {
        headers: { Authorization: `Bearer ${token.access_token}` },
      })
      if (!identityResponse.ok) throw new Error('No se pudo identificar la cuenta de Google autorizada.')
      const identity = await identityResponse.json() as { email?: string; sub?: string }
      if (!identity.email) throw new Error('Google no devolvió el correo de la cuenta autorizada.')
      const stored: StoredTokens = {
        accessToken: token.access_token,
        refreshToken: token.refresh_token,
        expiresAt: Date.now() + token.expires_in * 1000,
        scope: token.scope,
        tokenType: token.token_type,
        email: identity.email,
        subject: identity.sub,
      }
      await this.saveTokens(stored)
      await this.notifyBackend('/api/v1/integraciones/calendario/electron/oauth/autorizada', {
        cuentaEmail: identity.email,
      })
      return { available: true, connected: true, email: identity.email, expiresAt: new Date(stored.expiresAt).toISOString() }
    } finally {
      callback.close()
    }
  }

  private async listenForCallback(expectedState: string): Promise<{ port: number; code: Promise<string>; close: () => void }> {
    let server: Server
    let settled = false
    let resolveCode!: (code: string) => void
    let rejectCode!: (error: Error) => void
    const code = new Promise<string>((resolve,reject) => { resolveCode=resolve; rejectCode=reject })
    server = createServer((request,response) => {
      const requestUrl = new URL(request.url ?? '/', 'http://127.0.0.1')
      if (requestUrl.pathname !== CALLBACK_PATH) { response.writeHead(404).end(); return }
      const receivedState = requestUrl.searchParams.get('state') ?? ''
      const validState = receivedState.length===expectedState.length &&
        timingSafeEqual(Buffer.from(receivedState),Buffer.from(expectedState))
      const error = requestUrl.searchParams.get('error')
      const authCode = requestUrl.searchParams.get('code')
      if (!validState || error || !authCode) {
        response.writeHead(400,{'Content-Type':'text/html; charset=utf-8'}).end(resultPage(false))
        if (!settled) { settled=true; rejectCode(new Error(error ? `Autorización cancelada: ${error}.` : 'La respuesta OAuth no es válida.')) }
        return
      }
      response.writeHead(200,{'Content-Type':'text/html; charset=utf-8'}).end(resultPage(true))
      if (!settled) { settled=true; resolveCode(authCode) }
    })
    await new Promise<void>((resolve,reject) => {
      server.once('error',reject)
      server.listen(0,'127.0.0.1',resolve)
    })
    const address = server.address()
    if (!address || typeof address==='string') { server.close(); throw new Error('No se pudo abrir el callback OAuth local.') }
    const timeout = setTimeout(() => {
      if (!settled) { settled=true; rejectCode(new Error('La autorización de Google expiró.')) }
      server.close()
    },FLOW_TIMEOUT_MS)
    return { port:address.port,code,close:()=>{ clearTimeout(timeout); server.close() } }
  }

  private async saveTokens(tokens: StoredTokens): Promise<void> {
    const encrypted = await safeStorage.encryptStringAsync(JSON.stringify(tokens))
    const target = this.tokenPath()
    const temporary = `${target}.tmp`
    await fs.mkdir(path.dirname(target),{ recursive:true })
    await fs.writeFile(temporary,encrypted.toString('base64'),{ encoding:'utf8',mode:0o600 })
    await fs.rename(temporary,target)
  }

  private async loadTokens(): Promise<StoredTokens | null> {
    try {
      if (!await safeStorage.isAsyncEncryptionAvailable()) return null
      const encrypted = Buffer.from(await fs.readFile(this.tokenPath(),'utf8'),'base64')
      const decrypted = await safeStorage.decryptStringAsync(encrypted)
      if (decrypted.shouldReEncrypt) await this.saveTokens(JSON.parse(decrypted.result) as StoredTokens)
      return JSON.parse(decrypted.result) as StoredTokens
    } catch { return null }
  }

  private async deleteTokens(): Promise<void> {
    await fs.rm(this.tokenPath(),{ force:true })
  }

  private async readClientConfig(): Promise<OAuthClientConfig | null> {
    const envId = process.env.GANADERO_GOOGLE_CLIENT_ID?.trim()
    if (envId) return { clientId:envId,clientSecret:process.env.GANADERO_GOOGLE_CLIENT_SECRET?.trim()||undefined }
    const candidates = [
      path.join(app.getPath('userData'),'google-oauth-client.json'),
      ...(app.isPackaged ? [path.join(process.resourcesPath,'google-oauth-client.json')] : []),
    ]
    for (const file of candidates) {
      try {
        const raw = JSON.parse(await fs.readFile(file,'utf8')) as Record<string,unknown>
        const desktop = (raw.installed ?? raw) as Record<string,unknown>
        if (typeof desktop.client_id==='string' && desktop.client_id.trim())
          return { clientId:desktop.client_id.trim(),clientSecret:typeof desktop.client_secret==='string'?desktop.client_secret.trim():undefined }
      } catch { /* prueba el siguiente origen */ }
    }
    return null
  }

  private tokenPath(): string { return path.join(app.getPath('userData'),'secrets','google-calendar.oauth') }
  private configurationHelp(): string {
    return `Falta el cliente OAuth de escritorio. Guarda google-oauth-client.json en ${app.getPath('userData')}.`
  }
  private async notifyBackend(endpoint:string,body:unknown): Promise<void> {
    const port=this.getBackendPort()
    const response=await fetch(`http://127.0.0.1:${port}${endpoint}`,{
      method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body),
    })
    if(!response.ok) throw new Error(`El backend local rechazó la actualización OAuth (${response.status}).`)
  }
}

function base64Url(value:Buffer):string { return value.toString('base64url') }
function resultPage(ok:boolean):string {
  return `<!doctype html><html lang="es"><meta charset="utf-8"><title>Ganadero</title><body style="font-family:system-ui;padding:3rem"><h1>${ok?'Cuenta conectada':'No se pudo conectar'}</h1><p>${ok?'Puedes cerrar esta pestaña y volver a Ganadero.':'Vuelve a Ganadero e intenta nuevamente.'}</p></body></html>`
}
