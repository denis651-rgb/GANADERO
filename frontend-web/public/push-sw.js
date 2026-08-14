self.addEventListener('push', (event) => {
  let payload = {}
  try { payload = event.data ? event.data.json() : {} } catch { payload = { body: event.data?.text() } }
  const title = payload.title || 'GANADERO'
  const options = {
    body: payload.body || 'Tienes una nueva alerta.',
    icon: payload.icon || '/icons/icon-192.png',
    badge: '/icons/icon-192.png',
    data: payload.data || { url: '/' },
    tag: payload.data?.alertaId || undefined,
  }
  event.waitUntil(self.registration.showNotification(title, options))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const target = new URL(event.notification.data?.url || '/', self.location.origin).href
  event.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clients) => {
    const existing = clients.find((client) => new URL(client.url).origin === self.location.origin)
    if (existing) { existing.navigate(target); return existing.focus() }
    return self.clients.openWindow(target)
  }))
})
