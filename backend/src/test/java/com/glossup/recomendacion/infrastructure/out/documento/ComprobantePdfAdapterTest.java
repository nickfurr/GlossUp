package com.glossup.recomendacion.infrastructure.out.documento;

import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.port.out.ClientePort;
import com.glossup.shared.domain.Dinero;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ComprobantePdfAdapterTest {

    @Test
    void elPdfLlevaNumeroClienteProductosYTotal() throws Exception {
        Pedido pedido = new Pedido(7L, 1L, EstadoPedido.CREADO, List.of(
                new PedidoItem(100L, "Sérum Niacinamida 10%", 2, Dinero.de(45900.0)),
                new PedidoItem(200L, "Protector solar FPS 50", 1, Dinero.de(62000.0))),
                OffsetDateTime.of(2026, 10, 28, 19, 0, 0, 0, ZoneOffset.UTC));
        pedido.registrarPago("1234-5678", "NEQUI", OffsetDateTime.of(2026, 10, 28, 19, 5, 0, 0, ZoneOffset.UTC));

        byte[] pdf = new ComprobantePdfAdapter().generar(pedido, new ClientePort.Cliente("Ana Gómez", "ana@correo.co"),
                "GU-0000007", OffsetDateTime.of(2026, 10, 28, 19, 6, 0, 0, ZoneOffset.UTC));

        String texto = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
        for (String esperado : List.of("GU-0000007", "Ana Gómez", "ana@correo.co", "Sérum Niacinamida 10%",
                "Protector solar FPS 50", "Nequi", "1234-5678", "153.800", "28 de octubre de 2026, 14:05")) {
            assertTrue(texto.contains(esperado), () -> "falta «" + esperado + "» en:\n" + texto);
        }
    }
}
