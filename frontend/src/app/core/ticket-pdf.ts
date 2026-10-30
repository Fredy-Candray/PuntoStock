import { jsPDF } from 'jspdf';
import type { Ticket } from './api.service';

export function crearComprobantePdf(ticket: Ticket): jsPDF {
  const pdf = new jsPDF({ unit: 'mm', format: 'a5', compress: true });
  const numero = 'V-' + String(ticket.id).padStart(6, '0');
  const margen = 12,
    ancho = 124,
    derecha = 136,
    limite = 190;
  let y = 16;
  const dinero = (valor: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(valor);
  const texto = (valor: string) =>
    valor.replace(/[\u0000-\u0008\u000b-\u001f\u007f]/g, '').replace(/\u00a0/g, ' ');
  const fuente = (size = 10, bold = false) => {
    pdf.setFont('helvetica', bold ? 'bold' : 'normal');
    pdf.setFontSize(size);
  };
  function espacio(alto: number) {
    if (y + alto <= limite) return;
    pdf.addPage();
    y = 16;
    fuente(9, true);
    pdf.setTextColor(18, 63, 74);
    pdf.text('PuntoStock | ' + numero + ' (continuación)', margen, y);
    y += 10;
    pdf.setTextColor(30, 45, 50);
  }
  function escribir(valor: string, size = 10, bold = false, maxWidth = ancho) {
    fuente(size, bold);
    const lineas = pdf.splitTextToSize(texto(valor), maxWidth) as string[];
    for (const linea of lineas) {
      espacio(5);
      fuente(size, bold);
      pdf.text(linea, margen, y);
      y += 5;
    }
  }
  function separador() {
    espacio(5);
    pdf.setDrawColor(205, 219, 213);
    pdf.line(margen, y, derecha, y);
    y += 6;
  }
  function pareja(etiqueta: string, valor: string, bold = false) {
    fuente(10, bold);
    const lineas = pdf.splitTextToSize(texto(valor), 66) as string[];
    espacio(5);
    fuente(10, bold);
    pdf.text(etiqueta, margen, y);
    for (const linea of lineas) {
      espacio(5);
      fuente(10, bold);
      pdf.text(linea, derecha, y, { align: 'right' });
      y += 5;
    }
  }
  pdf.setProperties({
    title: 'Comprobante ' + numero,
    subject: 'Venta registrada en PuntoStock',
    creator: 'PuntoStock',
  });
  pdf.setTextColor(18, 63, 74);
  fuente(20, true);
  pdf.text('PuntoStock', margen, y);
  y += 8;
  pdf.setTextColor(30, 45, 50);
  escribir('Comprobante de venta', 12, true);
  escribir('Venta ' + numero);
  const fecha = new Intl.DateTimeFormat('es-SV', {
    timeZone: 'America/El_Salvador',
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(ticket.fecha));
  escribir(fecha + ' (El Salvador)', 9);
  y += 2;
  separador();
  escribir('Atendió: ' + ticket.vendedor);
  escribir('Cliente: ' + (ticket.cliente || 'Público general'));
  y += 2;
  separador();
  pareja('Detalle', 'Importe', true);
  separador();
  for (const linea of ticket.lineas) {
    espacio(16);
    fuente(10, true);
    pdf.text(dinero(linea.subtotal), derecha, y, { align: 'right' });
    escribir(linea.nombre, 10, true, 88);
    if (linea.presentacion === 'CAJA')
      escribir('Caja de ' + linea.unidadesPorPresentacion + ' unidades', 9, false, 88);
    escribir(
      linea.sku + ' | ' + linea.cantidad + ' x ' + dinero(linea.precioUnitario),
      9,
      false,
      88,
    );
    if (linea.observaciones) escribir(linea.observaciones, 9, false, 88);
    y += 3;
  }
  separador();
  pareja('TOTAL', dinero(ticket.total), true);
  y += 3;
  const medio =
    (
      {
        EFECTIVO: 'Efectivo',
        TARJETA: 'Tarjeta',
        TRANSFERENCIA: 'Transferencia',
        SIN_REGISTRO: 'No registrado',
      } as Record<string, string>
    )[ticket.metodoPago] || ticket.metodoPago;
  pareja('Medio de pago', medio);
  if (ticket.recibido !== null)
    pareja(ticket.metodoPago === 'EFECTIVO' ? 'Recibido' : 'Pagado', dinero(ticket.recibido));
  if (ticket.metodoPago === 'EFECTIVO' && ticket.cambio !== null)
    pareja('Cambio', dinero(ticket.cambio), true);
  if (ticket.referenciaPago) escribir('Referencia: ' + ticket.referenciaPago, 9);
  y += 3;
  separador();
  escribir('Gracias por su compra.', 10, true);
  escribir('Comprobante de la operación registrada en PuntoStock.', 8);
  const paginas = pdf.getNumberOfPages();
  for (let pagina = 1; pagina <= paginas; pagina++) {
    pdf.setPage(pagina);
    fuente(8);
    pdf.setTextColor(100, 115, 110);
    pdf.text(numero + ' | ' + pagina + ' / ' + paginas, derecha, 202, { align: 'right' });
  }
  return pdf;
}
