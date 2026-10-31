package com.glossup.recomendacion.infrastructure.out.documento;

import com.glossup.recomendacion.domain.model.Envio;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.port.out.ClientePort;
import com.glossup.recomendacion.domain.port.out.DocumentoComprobantePort;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/** Comprobante de compra en PDF (HU11) con OpenPDF, con los colores de la marca. */
@Component
public class ComprobantePdfAdapter implements DocumentoComprobantePort {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final ZoneId HORA_COLOMBIA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", COLOMBIA);
    private static final DateTimeFormatter FECHA_ENTREGA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", COLOMBIA);
    private static final Map<String, String> METODOS = Map.of("CARD", "Tarjeta", "PSE", "PSE", "NEQUI", "Nequi",
            "BANCOLOMBIA_TRANSFER", "Botón Bancolombia", "BANCOLOMBIA_QR", "QR Bancolombia", "DAVIPLATA", "Daviplata");

    private static final Color VINO = new Color(0x5e, 0x1a, 0x33);
    private static final Color ROSA = new Color(0xf7, 0xe9, 0xee);
    private static final Color BORDE = new Color(0xea, 0xd9, 0xe0);
    private static final Color GRIS = new Color(0x6b, 0x5a, 0x61);

    private static final Font MARCA = new Font(Font.HELVETICA, 24, Font.BOLD, VINO);
    private static final Font LEMA = new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS);
    private static final Font TITULO = new Font(Font.HELVETICA, 13, Font.BOLD, VINO);
    private static final Font ETIQUETA = new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS);
    private static final Font TEXTO = new Font(Font.HELVETICA, 10);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font CABECERA = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font TOTAL = new Font(Font.HELVETICA, 13, Font.BOLD, VINO);
    private static final Font NOTA = new Font(Font.HELVETICA, 8, Font.ITALIC, GRIS);

    @Override
    public byte[] generar(Pedido pedido, ClientePort.Cliente cliente, String numero, OffsetDateTime emitidoEn) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.LETTER, 50, 50, 50, 50);
        PdfWriter.getInstance(doc, salida);
        doc.addTitle("Comprobante de compra " + numero);
        doc.addAuthor("GlossUP");
        doc.open();

        PdfPTable encabezado = tabla(new float[]{1, 1});
        PdfPCell marca = celda();
        marca.addElement(new Paragraph("GlossUP", MARCA));
        marca.addElement(new Paragraph("Cosmética compatible con tu piel", LEMA));
        encabezado.addCell(marca);
        PdfPCell titulo = celda();
        titulo.addElement(alineado(new Paragraph("COMPROBANTE DE COMPRA", TITULO)));
        titulo.addElement(alineado(new Paragraph("N.º " + numero, NEGRITA)));
        titulo.addElement(alineado(new Paragraph("Emitido el " + fecha(emitidoEn), ETIQUETA)));
        encabezado.addCell(titulo);
        doc.add(encabezado);
        doc.add(separador());

        PdfPTable datos = tabla(new float[]{1, 1});
        datos.setSpacingBefore(10);
        datos.addCell(dato("Cliente", cliente.nombre()));
        datos.addCell(dato("Correo", cliente.email()));
        datos.addCell(dato("Pedido", "#" + pedido.getId()));
        datos.addCell(dato("Fecha de pago", pedido.getPagadoEn() != null ? fecha(pedido.getPagadoEn()) : "—"));
        datos.addCell(dato("Medio de pago", METODOS.getOrDefault(valor(pedido.getMetodoPago()), valor(pedido.getMetodoPago()))));
        datos.addCell(dato("Transacción Wompi", valor(pedido.getTransaccionId())));
        Envio envio = pedido.getEnvio();
        if (envio != null) {
            var d = envio.getDestino();
            datos.addCell(dato("Enviar a", d.nombreRecibe() + " · " + d.direccion() + ", " + d.barrio() + ", " + d.ciudad() + ", " + d.departamento()));
            datos.addCell(dato("Envío", envio.getMetodo().etiqueta() + " · llegada estimada " + FECHA_ENTREGA.format(envio.getFechaEstimada())));
        }
        doc.add(datos);

        PdfPTable detalle = new PdfPTable(new float[]{5, 1.2f, 2.2f, 2.2f});
        detalle.setWidthPercentage(100);
        detalle.setSpacingBefore(18);
        detalle.setHeaderRows(1);
        for (String columna : new String[]{"Producto", "Cant.", "Precio unitario", "Subtotal"}) {
            PdfPCell c = new PdfPCell(new Phrase(columna, CABECERA));
            c.setBackgroundColor(VINO);
            c.setBorder(Rectangle.NO_BORDER);
            c.setPadding(7);
            c.setHorizontalAlignment(columna.equals("Producto") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            detalle.addCell(c);
        }
        int fila = 0;
        for (PedidoItem item : pedido.getItems()) {
            Color fondo = fila++ % 2 == 0 ? Color.WHITE : ROSA;
            detalle.addCell(linea(item.nombre() != null ? item.nombre() : "Producto #" + item.productoId(), fondo, Element.ALIGN_LEFT));
            detalle.addCell(linea(String.valueOf(item.cantidad()), fondo, Element.ALIGN_RIGHT));
            detalle.addCell(linea(pesos(item.precioUnitario().monto()), fondo, Element.ALIGN_RIGHT));
            detalle.addCell(linea(pesos(item.subtotal().monto()), fondo, Element.ALIGN_RIGHT));
        }
        if (envio != null) {
            PdfPCell etiquetaEnvio = linea("Envío " + envio.getMetodo().etiqueta(), Color.WHITE, Element.ALIGN_RIGHT);
            etiquetaEnvio.setColspan(3);
            detalle.addCell(etiquetaEnvio);
            detalle.addCell(linea(envio.getCosto().monto().signum() == 0 ? "Gratis" : pesos(envio.getCosto().monto()), Color.WHITE, Element.ALIGN_RIGHT));
        }
        PdfPCell etiquetaTotal = new PdfPCell(new Phrase("Total pagado", TOTAL));
        etiquetaTotal.setColspan(3);
        PdfPCell valorTotal = new PdfPCell(new Phrase(pesos(pedido.getTotal().monto()), TOTAL));
        for (PdfPCell c : new PdfPCell[]{etiquetaTotal, valorTotal}) {
            c.setBorder(Rectangle.TOP);
            c.setBorderColor(VINO);
            c.setBorderWidth(1.5f);
            c.setPaddingTop(9);
            c.setHorizontalAlignment(Element.ALIGN_RIGHT);
            detalle.addCell(c);
        }
        doc.add(detalle);

        Paragraph nota = new Paragraph("Soporte digital del pago de tu pedido en GlossUP, procesado por Wompi. "
                + "Consérvalo para cualquier reclamo o devolución. Este documento no reemplaza la factura electrónica de venta.", NOTA);
        nota.setSpacingBefore(28);
        doc.add(nota);
        doc.close();
        return salida.toByteArray();
    }

    private static PdfPTable tabla(float[] anchos) {
        PdfPTable t = new PdfPTable(anchos);
        t.setWidthPercentage(100);
        return t;
    }

    private static PdfPCell celda() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingBottom(6);
        return c;
    }

    private static Paragraph alineado(Paragraph p) {
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private static PdfPTable separador() {
        PdfPTable t = tabla(new float[]{1});
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(BORDE);
        c.setBorderWidth(1.5f);
        t.addCell(c);
        return t;
    }

    private static PdfPCell dato(String etiqueta, String valor) {
        PdfPCell c = celda();
        c.addElement(new Paragraph(etiqueta, ETIQUETA));
        c.addElement(new Paragraph(valor, TEXTO));
        return c;
    }

    private static PdfPCell linea(String texto, Color fondo, int alineacion) {
        PdfPCell c = new PdfPCell(new Phrase(texto, TEXTO));
        c.setBackgroundColor(fondo);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(BORDE);
        c.setPadding(7);
        c.setHorizontalAlignment(alineacion);
        return c;
    }

    private static String fecha(OffsetDateTime f) {
        return FECHA.format(f.atZoneSameInstant(HORA_COLOMBIA));
    }

    private static String pesos(BigDecimal monto) {
        NumberFormat cop = NumberFormat.getCurrencyInstance(COLOMBIA);
        cop.setMaximumFractionDigits(0);
        return cop.format(monto);
    }

    private static String valor(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }
}
