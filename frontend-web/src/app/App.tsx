import { BrowserRouter } from 'react-router'
import { AppProviders } from '@/app/providers/AppProviders'
import { AppRouter } from '@/app/router'
import { ConnectivityBanner } from '@/offline/components/ConnectivityBanner'
import { PwaUpdatePrompt } from '@/offline/components/PwaUpdatePrompt'

export function App() {
  return (
    <BrowserRouter>
      <AppProviders>
        <ConnectivityBanner />
        <AppRouter />
        <PwaUpdatePrompt />
      </AppProviders>
    </BrowserRouter>
  )
}
