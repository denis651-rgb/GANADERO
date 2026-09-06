export interface Proveedor {
  id: string
  nombre: string
  telefono?: string
  documento?: string
  direccion?: string
  correo?: string
  observaciones?: string
  activo: boolean
  version: number
}

export interface ProveedorInput {
  nombre: string
  telefono?: string
  documento?: string
  direccion?: string
  correo?: string
  observaciones?: string
  version?: number
}

export interface ProveedorNuevoInput {
  nombre: string
  telefono?: string
  documento?: string
  direccion?: string
  correo?: string
}

/** Selección hecha desde un formulario de compra: un proveedor existente por id, o los datos para crear uno nuevo. */
export interface ProveedorSeleccion {
  proveedorId?: string
  proveedorNuevo?: ProveedorNuevoInput
  etiqueta?: string
}
