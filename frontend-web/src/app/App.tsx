import { BrowserRouter } from 'react-router'
import { AppProviders } from '@/app/providers/AppProviders'
import { AppRouter } from '@/app/router'

export function App() {
  return (
    <BrowserRouter>
      <AppProviders>
        <AppRouter />
      </AppProviders>
    </BrowserRouter>
  )
}
