import { Navigate, Route, Routes } from 'react-router'
import { AppShell } from '@/app/layout/AppShell'
import { DashboardPage } from '@/features/dashboard/pages/DashboardPage'
import { PropiedadesPage } from '@/features/propiedades/pages/PropiedadesPage'
import { ConfiguracionGeneralPage } from '@/features/propiedades/pages/ConfiguracionGeneralPage'
import { RespaldosPage } from '@/features/propiedades/pages/RespaldosPage'
import { CategoriasEdadPage } from '@/features/propiedades/pages/CategoriasEdadPage'
import { GoogleCalendarPage } from '@/features/propiedades/pages/GoogleCalendarPage'
import { PotrerosPage } from '@/features/potreros/pages/PotrerosPage'
import { AnimalesPage } from '@/features/animales/pages/AnimalesPage'
import { NuevoAnimalPage } from '@/features/animales/pages/NuevoAnimalPage'
import { IngresoLotePage } from '@/features/animales/pages/IngresoLotePage'
import { AnimalDetailPage } from '@/features/animales/pages/AnimalDetailPage'
import { EditarAnimalPage } from '@/features/animales/pages/EditarAnimalPage'
import { DeclararHistorialLotePage } from '@/features/animales/pages/DeclararHistorialLotePage'
import { QrScannerPage } from '@/features/animales/qr/pages/QrScannerPage'
import { QrPrintPage } from '@/features/animales/qr/pages/QrPrintPage'
import { LotesPage } from '@/features/lotes/pages/LotesPage'
import { LoteDetailPage } from '@/features/lotes/pages/LoteDetailPage'
import { MovimientosPage } from '@/features/movimientos/pages/MovimientosPage'
import { AuditoriaPage } from '@/features/auditoria/pages/AuditoriaPage'
import { PesajesPage } from '@/features/pesajes/pages/PesajesPage'
import { PesajeDetailPage } from '@/features/pesajes/pages/PesajeDetailPage'
import { ComprasPage } from '@/features/compras/pages/ComprasPage'
import { CompraDetailPage } from '@/features/compras/pages/CompraDetailPage'
import { ReproduccionPage } from '@/features/reproduccion/pages/ReproduccionPage'
import { SanidadPage } from '@/features/sanidad/pages/SanidadPage'
import { VentasPage } from '@/features/ventas/pages/VentasPage'
import { AlertasPage } from '@/features/alertas/pages/AlertasPage'
import { ReportesPage } from '@/features/reportes/pages/ReportesPage'
import { NotFoundPage } from '@/shared/pages/NotFoundPage'

export function AppRouter() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<DashboardPage />} />
        <Route path="propiedades" element={<PropiedadesPage />} />
        <Route path="propiedades/configuracion" element={<ConfiguracionGeneralPage />} />
        <Route path="propiedades/respaldos" element={<RespaldosPage />} />
        <Route path="propiedades/categorias-edad" element={<CategoriasEdadPage />} />
        <Route path="propiedades/google-calendar" element={<GoogleCalendarPage />} />
        <Route path="potreros" element={<PotrerosPage />} />
        <Route path="animales" element={<AnimalesPage />} />
        <Route path="animales/nuevo" element={<NuevoAnimalPage />} />
        <Route path="animales/ingreso-lote" element={<IngresoLotePage />} />
        <Route path="animales/:id" element={<AnimalDetailPage />} />
        <Route path="animales/:id/editar" element={<EditarAnimalPage />} />
        <Route path="animales/declarar-historial" element={<DeclararHistorialLotePage />} />
        <Route path="lotes" element={<LotesPage />} />
        <Route path="lotes/:id" element={<LoteDetailPage />} />
        <Route path="movimientos" element={<MovimientosPage />} />
        <Route path="auditoria" element={<AuditoriaPage />} />
        <Route path="pesajes" element={<PesajesPage />} />
        <Route path="pesajes/:id" element={<PesajeDetailPage />} />
        <Route path="compras" element={<ComprasPage />} />
        <Route path="compras/:id" element={<CompraDetailPage />} />
        <Route path="reproduccion" element={<ReproduccionPage />} />
        <Route path="sanidad" element={<SanidadPage />} />
        <Route path="ventas" element={<VentasPage />} />
        <Route path="alertas" element={<AlertasPage />} />
        <Route path="reportes" element={<ReportesPage />} />
        <Route path="animales/qr/imprimir" element={<QrPrintPage />} />
        <Route path="qr/escanear" element={<QrScannerPage />} />
      </Route>

      <Route path="/inicio" element={<Navigate to="/" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
