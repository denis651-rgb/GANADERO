import { Navigate, Route, Routes } from 'react-router'
import { AppShell } from '@/app/layout/AppShell'
import { ProtectedRoute } from '@/app/routes/ProtectedRoute'
import { LoginPage } from '@/auth/pages/LoginPage'
import { ForgotPasswordPage } from '@/auth/pages/ForgotPasswordPage'
import { PasswordActionPage } from '@/auth/pages/PasswordActionPage'
import { PerfilPage } from '@/features/perfil/pages/PerfilPage'
import { DashboardPage } from '@/features/dashboard/pages/DashboardPage'
import { EmpresaPage } from '@/features/empresa/pages/EmpresaPage'
import { UsuariosPage } from '@/features/usuarios/pages/UsuariosPage'
import { RolesPage } from '@/features/roles/pages/RolesPage'
import { PropiedadesPage } from '@/features/propiedades/pages/PropiedadesPage'
import { PotrerosPage } from '@/features/potreros/pages/PotrerosPage'
import { AnimalesPage } from '@/features/animales/pages/AnimalesPage'
import { NuevoAnimalPage } from '@/features/animales/pages/NuevoAnimalPage'
import { AnimalDetailPage } from '@/features/animales/pages/AnimalDetailPage'
import { EditarAnimalPage } from '@/features/animales/pages/EditarAnimalPage'
import { LotesPage } from '@/features/lotes/pages/LotesPage'
import { LoteDetailPage } from '@/features/lotes/pages/LoteDetailPage'
import { MovimientosPage } from '@/features/movimientos/pages/MovimientosPage'
import { AuditoriaPage } from '@/features/auditoria/pages/AuditoriaPage'
import { PesajesPage } from '@/features/pesajes/pages/PesajesPage'
import { PesajeDetailPage } from '@/features/pesajes/pages/PesajeDetailPage'
import { ReproduccionPage } from '@/features/reproduccion/pages/ReproduccionPage'
import { SanidadPage } from '@/features/sanidad/pages/SanidadPage'
import { InventarioPage } from '@/features/inventario/pages/InventarioPage'
import { AlimentacionPage } from '@/features/alimentacion/pages/AlimentacionPage'
import { ComercialPage } from '@/features/comercial/pages/ComercialPage'
import { FinanzasPage } from '@/features/finanzas/pages/FinanzasPage'
import { AlertasPage } from '@/features/alertas/pages/AlertasPage'
import { ReportesPage } from '@/features/reportes/pages/ReportesPage'
import { SyncPage } from '@/sync/pages/SyncPage'
import { NotFoundPage } from '@/shared/pages/NotFoundPage'

export function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/recuperar-contrasena" element={<ForgotPasswordPage />} />
      <Route path="/auth/recuperar-contrasena" element={<ForgotPasswordPage />} />
      <Route path="/auth/restablecer-contrasena" element={<PasswordActionPage />} />
      <Route path="/auth/aceptar-invitacion" element={<PasswordActionPage invitation />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="empresa" element={<EmpresaPage />} />
          <Route path="perfil" element={<PerfilPage />} />
          <Route path="usuarios" element={<UsuariosPage />} />
          <Route path="roles" element={<RolesPage />} />
          <Route path="propiedades" element={<PropiedadesPage />} />
          <Route path="potreros" element={<PotrerosPage />} />
          <Route path="animales" element={<AnimalesPage />} />
          <Route path="animales/nuevo" element={<NuevoAnimalPage />} />
          <Route path="animales/:id" element={<AnimalDetailPage />} />
          <Route path="animales/:id/editar" element={<EditarAnimalPage />} />
          <Route path="lotes" element={<LotesPage />} />
          <Route path="lotes/:id" element={<LoteDetailPage />} />
          <Route path="movimientos" element={<MovimientosPage />} />
          <Route path="auditoria" element={<AuditoriaPage />} />
          <Route path="pesajes" element={<PesajesPage />} />
          <Route path="pesajes/:id" element={<PesajeDetailPage />} />
          <Route path="reproduccion" element={<ReproduccionPage />} />
          <Route path="sanidad" element={<SanidadPage />} />
          <Route path="inventario" element={<InventarioPage />} />
          <Route path="alimentacion" element={<AlimentacionPage />} />
          <Route path="comercial" element={<ComercialPage />} />
          <Route path="finanzas" element={<FinanzasPage />} />
          <Route path="alertas" element={<AlertasPage />} />
          <Route path="reportes" element={<ReportesPage />} />
          <Route path="sincronizacion" element={<SyncPage />} />
        </Route>
      </Route>

      <Route path="/inicio" element={<Navigate to="/" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
