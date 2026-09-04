import { contextBridge } from 'electron'

function readApiBaseUrl(): string | undefined {
  const flag = process.argv.find((arg) => arg.startsWith('--ganadero-api-base-url='))
  return flag?.slice('--ganadero-api-base-url='.length)
}

contextBridge.exposeInMainWorld('ganadero', {
  apiBaseUrl: readApiBaseUrl(),
})
