package com.glossup.recomendacion.infrastructure.in.rest;

import com.glossup.recomendacion.domain.model.Comprobante;
import com.glossup.recomendacion.domain.model.EstadoPedido;
import com.glossup.recomendacion.domain.model.Pedido;
import com.glossup.recomendacion.domain.model.PedidoItem;
import com.glossup.recomendacion.domain.model.TransaccionPago;
import com.glossup.recomendacion.domain.port.in.ComprobanteUseCase;
import com.glossup.recomendacion.domain.port.in.PagarPedidoUseCase;
import com.glossup.shared.config.SecurityConfig;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.GlobalExceptionHandler;
import com.glossup.shared.security.JwtAuthenticationFilter;
import com.glossup.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PagoController.class, EventosWompiController.class, ComprobanteController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, PagoControllerTest.TestSecurityConfig.class})
class PagoControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestSecurityConfig {
        @org.springframework.context.annotation.Bean
        public JwtService jwtService() {
            return new JwtService("secreto-de-prueba-con-longitud-suficiente-32+", 3600000L);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PagarPedidoUseCase pagos;

    @MockBean
    private ComprobanteUseCase comprobantes;

    private static Pedido pedido(EstadoPedido estado) {
        return new Pedido(55L, 7L, estado, List.of(new PedidoItem(1L, 1, Dinero.de(25000.0))), OffsetDateTime.now());
    }

    @Test
    void webhook_esPublicoYRespondeSegunLaFirma() throws Exception {
        when(pagos.procesarEvento("{\"ok\":1}")).thenReturn(true);
        when(pagos.procesarEvento("{\"ok\":0}")).thenReturn(false);

        mockMvc.perform(post("/api/pagos/wompi/eventos").contentType(MediaType.APPLICATION_JSON).content("{\"ok\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/pagos/wompi/eventos").contentType(MediaType.APPLICATION_JSON).content("{\"ok\":0}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void iniciarPago_sinTokenEs401() throws Exception {
        mockMvc.perform(post("/api/pedidos/55/pago")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "7", roles = "VENDEDOR")
    void iniciarPago_soloElCliente() throws Exception {
        mockMvc.perform(post("/api/pedidos/55/pago")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void iniciarPago_devuelveLaUrlDeWompi() throws Exception {
        when(pagos.iniciarPago(7L, 55L)).thenReturn("https://checkout.wompi.co/p/?reference=GLOSSUP-55-1");
        mockMvc.perform(post("/api/pedidos/55/pago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://checkout.wompi.co/p/?reference=GLOSSUP-55-1"));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void confirmarPago_devuelveElPedidoYElEstadoDeLaTransaccion() throws Exception {
        Pedido pagado = pedido(EstadoPedido.CREADO);
        pagado.registrarPago("tx-9", "NEQUI", OffsetDateTime.now());
        when(pagos.confirmarPago(7L, 55L, "tx-9"))
                .thenReturn(new PagarPedidoUseCase.ResultadoPago(pagado, TransaccionPago.Estado.APROBADA));

        mockMvc.perform(get("/api/pedidos/55/pago/tx-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoTransaccion").value("APROBADA"))
                .andExpect(jsonPath("$.pedido.estado").value("PAGADO"))
                .andExpect(jsonPath("$.pedido.metodoPago").value("NEQUI"))
                .andExpect(jsonPath("$.pedido.expiraEn").doesNotExist());
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void misPedidos_muestraElPlazoDePagoDeLosPendientes() throws Exception {
        when(pagos.misPedidos(7L)).thenReturn(List.of(pedido(EstadoPedido.CREADO)));
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("CREADO"))
                .andExpect(jsonPath("$[0].expiraEn").exists());
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void comprobante_seDescargaComoPdf() throws Exception {
        byte[] pdf = "%PDF-1.4 prueba".getBytes();
        when(comprobantes.descargar(7L, 55L)).thenReturn(new Comprobante(55L, "GU-0000055", pdf, OffsetDateTime.now()));
        mockMvc.perform(get("/api/pedidos/55/comprobante"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"comprobante-GU-0000055.pdf\""))
                .andExpect(content().bytes(pdf));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void comprobante_deUnPedidoSinPagarEs422() throws Exception {
        when(comprobantes.descargar(7L, 55L)).thenThrow(new ReglaNegocioException("El pedido #55 no está pagado"));
        mockMvc.perform(get("/api/pedidos/55/comprobante")).andExpect(status().isUnprocessableEntity());
    }
}
