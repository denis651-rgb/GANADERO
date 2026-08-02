import { Outlet } from 'react-router-dom'
import { Sidebar } from '@/app/layout/Sidebar'
import { Header } from '@/app/layout/Header'
import { MobileNav } from '@/app/layout/MobileNav'

export function AppShell() {
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="app-main">
        <Header />
        <main className="page-container">
          <Outlet />
        </main>
      </div>
      <MobileNav />
    </div>
  )
}
