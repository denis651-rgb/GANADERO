import { dialog } from 'electron'
import ExcelJS from 'exceljs'

export interface PlanillaSanitariaAnimal {
  codigo: string
  nombre?: string
  sexo: 'MACHO' | 'HEMBRA'
  edadTexto?: string
}

export interface PlanillaSanitariaInput {
  actividad: string
  fecha: string
  propiedad: string
  potrero?: string
  lote?: string
  producto?: string
  dosisTexto?: string
  viaAdministracion?: string
  lugarAplicacion?: string
  instrucciones?: string
  retiroCarneDias?: number
  retiroLecheDias?: number
  animales: PlanillaSanitariaAnimal[]
}

export type ExportarPlanillaResult = { cancelado: true } | { cancelado: false; path: string }

const COLUMNAS = ['N°', 'Código', 'Nombre', 'Sexo', 'Edad', 'Aplicado', 'Dosis aplicada', 'Observaciones']
const ANCHOS = [5, 14, 20, 10, 16, 11, 16, 30]

function unir(partes: Array<string | undefined | false>): string {
  return partes.filter((parte): parte is string => Boolean(parte)).join('    ')
}

/** Construye el libro de la planilla de campo — sin tocar disco, para poder probarlo sin Electron. */
export function construirPlanillaSanitaria(input: PlanillaSanitariaInput): ExcelJS.Workbook {
  const workbook = new ExcelJS.Workbook()
  const hoja = workbook.addWorksheet('Planilla de campo', {
    pageSetup: { orientation: 'landscape', fitToPage: true, fitToWidth: 1, fitToHeight: 0 },
  })
  // Solo el ancho: si `columns` trae `header`, ExcelJS escribe esos títulos en la fila 1 por su
  // cuenta, antes de que lleguemos al título y a las líneas de la jornada.
  hoja.columns = ANCHOS.map((width) => ({ width }))

  const titulo = hoja.addRow([`Planilla de campo — ${input.actividad}`])
  hoja.mergeCells(titulo.number, 1, titulo.number, COLUMNAS.length)
  titulo.font = { bold: true, size: 14 }

  const lineas = [
    unir(['Fecha: ' + input.fecha, 'Propiedad: ' + input.propiedad,
      input.potrero && 'Potrero: ' + input.potrero, input.lote && 'Lote: ' + input.lote]),
    unir([input.producto && 'Producto: ' + input.producto, input.dosisTexto && 'Dosis: ' + input.dosisTexto,
      input.viaAdministracion && 'Vía: ' + input.viaAdministracion, input.lugarAplicacion && 'Lugar: ' + input.lugarAplicacion]),
    unir([input.retiroCarneDias ? `Retiro de carne: ${input.retiroCarneDias} días` : undefined,
      input.retiroLecheDias ? `Retiro de leche: ${input.retiroLecheDias} días` : undefined]),
    input.instrucciones && `Instrucciones: ${input.instrucciones}`,
  ].filter((linea): linea is string => Boolean(linea))

  for (const linea of lineas) {
    const fila = hoja.addRow([linea])
    hoja.mergeCells(fila.number, 1, fila.number, COLUMNAS.length)
  }

  hoja.addRow([])
  const encabezado = hoja.addRow(COLUMNAS)
  encabezado.font = { bold: true }
  encabezado.eachCell((celda) => {
    celda.border = { bottom: { style: 'thin' } }
  })

  input.animales.forEach((animal, index) => {
    hoja.addRow([index + 1, animal.codigo, animal.nombre ?? '', animal.sexo === 'HEMBRA' ? 'Hembra' : 'Macho',
      animal.edadTexto ?? '', '☐', '', ''])
  })

  hoja.addRow([])
  const firma = hoja.addRow(['Responsable: _______________________________     Firma: _______________________________'])
  hoja.mergeCells(firma.number, 1, firma.number, COLUMNAS.length)

  return workbook
}

function nombreArchivoSugerido(input: PlanillaSanitariaInput): string {
  const slug = input.actividad
    .toLocaleLowerCase('es-BO')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
  return `planilla-${slug || 'jornada'}-${input.fecha}.xlsx`
}

/** Arma la planilla y la guarda donde el usuario elija (diálogo nativo). Solo disponible en Ganadero Desktop. */
export async function exportarPlanillaSanitaria(input: PlanillaSanitariaInput): Promise<ExportarPlanillaResult> {
  const result = await dialog.showSaveDialog({
    title: 'Guardar planilla de campo',
    defaultPath: nombreArchivoSugerido(input),
    filters: [{ name: 'Excel', extensions: ['xlsx'] }],
  })
  if (result.canceled || !result.filePath) return { cancelado: true }

  const workbook = construirPlanillaSanitaria(input)
  await workbook.xlsx.writeFile(result.filePath)
  return { cancelado: false, path: result.filePath }
}
