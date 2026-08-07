import { BrowserRouter } from 'react-router'
import { AppProviders } from '@/app/providers/AppProviders'
import { AppRouter } from '@/app/router'
import { ConnectivityBanner } from '@/offline/components/ConnectivityBanner'
import { PwaInstallPrompt } from '@/offline/components/PwaInstallPrompt'
import { PwaUpdatePrompt } from '@/offline/components/PwaUpdatePrompt'
import { SessionBanner } from '@/offline/components/SessionBanner'

export function App() {
  return (
    <BrowserRouter>
      <AppProviders>
        <ConnectivityBanner />
        <SessionBanner />
        <AppRouter />
        <PwaInstallPrompt />
        <PwaUpdatePrompt />
      </AppProviders>
    </BrowserRouter>
  )
}
