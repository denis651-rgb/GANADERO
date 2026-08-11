import { Outlet } from 'react-router'
import { Sidebar } from '@/app/layout/Sidebar'
import { Header } from '@/app/layout/Header'
import { MobileNav } from '@/app/layout/MobileNav'

export function AppShell() {
  return (
    <div className="app-shell">
      <a href="#main-content" className="skip-link">Saltar al contenido principal</a>
      <Sidebar />
      <div className="app-main">
        <Header />
        <main id="main-content" className="page-container" tabIndex={-1}>
          <Outlet />
        </main>
      </div>
      <MobileNav />
    </div>
  )
}
