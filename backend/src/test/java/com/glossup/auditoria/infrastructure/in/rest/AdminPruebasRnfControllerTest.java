package com.glossup.auditoria.infrastructure.in.rest;

import com.glossup.auditoria.domain.model.ReporteCoberturaRnf24;
import com.glossup.auditoria.domain.model.ReporteConsolidadoRnf;
import com.glossup.auditoria.domain.model.ReporteInvimaRnf05c;
import com.glossup.auditoria.domain.model.ReportePciRnf13;
import com.glossup.auditoria.domain.port.in.EjecutarPruebasRnfUseCase;
import com.glossup.shared.config.SecurityConfig;
import com.glossup.shared.exception.GlobalExceptionHandler;
import com.glossup.shared.security.JwtAuthenticationFilter;
import com.glossup.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminPruebasRnfController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, AdminPruebasRnfControllerTest.Config.class})
class AdminPruebasRnfControllerTest {

    @TestConfiguration
    static class Config {
        @Bean
        JwtService jwtService() {
            return new JwtService("secreto-de-prueba-con-longitud-suficiente-32+", 3600000L);
        }
    }

    @Autowired private MockMvc mockMvc;
    @MockitoBean private EjecutarPruebasRnfUseCase pruebasUseCase;

    @Test
    void sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get("/api/admin/pruebas-rnf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "10", roles = "CLIENTE")
    void conRolCliente_retorna403() throws Exception {
        mockMvc.perform(get("/api/admin/pruebas-rnf"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void conRolAdmin_ejecutarTodas_retorna200ConConsolidado() throws Exception {
        ReporteCoberturaRnf24 rnf24 = new ReporteCoberturaRnf24(
                "RNF-24", true, 85.0, 92.0, 89.0, 890, 110, 1000, 6, 6, "JACOCO", List.of()
        );
        ReportePciRnf13 rnf13 = new ReportePciRnf13(
                "RNF-13", true, OffsetDateTime.now(), 20, 120, List.of(), List.of("pago_transaccion_id"), 15, 0, 1, 0, "OK"
        );
        ReporteInvimaRnf05c rnf05c = new ReporteInvimaRnf05c(
                "RNF-05c", true, OffsetDateTime.now(), "DATOS_GOV_CO", 40, 40, 100.0, List.of(), List.of()
        );
        ReporteConsolidadoRnf consolidado = new ReporteConsolidadoRnf(OffsetDateTime.now(), true, rnf24, rnf13, rnf05c);

        when(pruebasUseCase.evaluarTodos(false)).thenReturn(consolidado);

        mockMvc.perform(get("/api/admin/pruebas-rnf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todosAprobados").value(true))
                .andExpect(jsonPath("$.rnf24.requisito").value("RNF-24"))
                .andExpect(jsonPath("$.rnf13.requisito").value("RNF-13"))
                .andExpect(jsonPath("$.rnf05c.requisito").value("RNF-05c"));

        verify(pruebasUseCase).evaluarTodos(false);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void conRolAdmin_evaluarRnf24_retorna200() throws Exception {
        ReporteCoberturaRnf24 rnf24 = new ReporteCoberturaRnf24(
                "RNF-24", true, 85.0, 95.0, 88.0, 880, 120, 1000, 6, 6, "JACOCO", List.of()
        );
        when(pruebasUseCase.evaluarRnf24(anyBoolean())).thenReturn(rnf24);

        mockMvc.perform(get("/api/admin/pruebas-rnf/rnf-24").param("ejecutarMaven", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requisito").value("RNF-24"))
                .andExpect(jsonPath("$.cumple").value(true))
                .andExpect(jsonPath("$.porcentajeCoberturaPerfil").value(95.0));

        verify(pruebasUseCase).evaluarRnf24(true);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void conRolAdmin_evaluarRnf13_retorna200() throws Exception {
        ReportePciRnf13 rnf13 = new ReportePciRnf13(
                "RNF-13", true, OffsetDateTime.now(), 18, 95, List.of(), List.of(), 5, 0, 1, 0, "OK"
        );
        when(pruebasUseCase.evaluarRnf13()).thenReturn(rnf13);

        mockMvc.perform(get("/api/admin/pruebas-rnf/rnf-13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requisito").value("RNF-13"))
                .andExpect(jsonPath("$.cumple").value(true));

        verify(pruebasUseCase).evaluarRnf13();
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void conRolAdmin_evaluarRnf05c_retorna200() throws Exception {
        ReporteInvimaRnf05c rnf05c = new ReporteInvimaRnf05c(
                "RNF-05c", true, OffsetDateTime.now(), "DATOS_GOV_CO", 10, 10, 100.0, List.of(), List.of()
        );
        when(pruebasUseCase.evaluarRnf05c()).thenReturn(rnf05c);

        mockMvc.perform(get("/api/admin/pruebas-rnf/rnf-05c"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requisito").value("RNF-05c"))
                .andExpect(jsonPath("$.cumple").value(true))
                .andExpect(jsonPath("$.porcentajeCumplimiento").value(100.0));

        verify(pruebasUseCase).evaluarRnf05c();
    }
}
