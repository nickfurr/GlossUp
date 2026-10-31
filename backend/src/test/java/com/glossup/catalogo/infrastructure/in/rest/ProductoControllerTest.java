package com.glossup.catalogo.infrastructure.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glossup.catalogo.domain.model.ComposicionProducto;
import com.glossup.catalogo.domain.model.ImagenProducto;
import com.glossup.catalogo.domain.model.Inci;
import com.glossup.catalogo.domain.model.Ingrediente;
import com.glossup.catalogo.domain.model.Producto;
import com.glossup.catalogo.domain.port.in.ActualizarProductoCommand;
import com.glossup.catalogo.domain.port.in.GestionarProductoUseCase;
import com.glossup.catalogo.domain.port.in.RegistrarProductoCommand;
import com.glossup.catalogo.domain.port.in.RegistrarProductoUseCase;
import com.glossup.catalogo.domain.port.out.ProductoRepositoryPort;
import com.glossup.catalogo.infrastructure.in.rest.dto.ActualizarProductoRequest;
import com.glossup.catalogo.infrastructure.in.rest.dto.IngredienteRequest;
import com.glossup.catalogo.infrastructure.in.rest.dto.RegistrarProductoRequest;
import com.glossup.shared.config.SecurityConfig;
import com.glossup.shared.domain.Dinero;
import com.glossup.shared.domain.ReglaNegocioException;
import com.glossup.shared.exception.GlobalExceptionHandler;
import com.glossup.shared.exception.NotFoundException;
import com.glossup.shared.security.JwtAuthenticationFilter;
import com.glossup.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class, ProductoControllerTest.TestSecurityConfig.class})
class ProductoControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestSecurityConfig {
        @org.springframework.context.annotation.Bean
        public JwtService jwtService() {
            return new JwtService("secreto-de-prueba-con-longitud-suficiente-32+", 3600000L);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrarProductoUseCase registrarProductoUseCase;

    @MockBean
    private GestionarProductoUseCase gestionarProductoUseCase;

    @MockBean
    private ProductoRepositoryPort productoRepository;

    @MockBean
    private com.glossup.catalogo.domain.port.in.ExtraerIngredientesImagenUseCase extraerIngredientesImagenUseCase;

    private RegistrarProductoRequest crearRequestValido() {
        return new RegistrarProductoRequest(
                "Serum Vitamina C",
                "GlossLab",
                "Serum Facial",
                BigDecimal.valueOf(85000.0),
                50,
                3.5,
                "INVIMA-2024",
                List.of("https://cdn.glossup.co/img1.webp", "https://cdn.glossup.co/img2.webp"),
                List.of(
                        new IngredienteRequest("Ascorbic Acid", 15.0),
                        new IngredienteRequest("Aqua", null)
                )
        );
    }

    private ActualizarProductoRequest crearActualizarRequestValido() {
        return new ActualizarProductoRequest(
                "Serum Vitamina C Renovado",
                "GlossLab",
                "Serum Facial",
                BigDecimal.valueOf(90000.0),
                40,
                3.6,
                "INVIMA-2024",
                List.of("https://cdn.glossup.co/img-new.webp"),
                List.of(new IngredienteRequest("Ascorbic Acid", 15.0))
        );
    }

    @Test
    void sinToken_retorna401Unauthorized() throws Exception {
        RegistrarProductoRequest request = crearRequestValido();

        mockMvc.perform(post("/api/catalogo/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "CLIENTE")
    void conRolCliente_retorna403Forbidden() throws Exception {
        RegistrarProductoRequest request = crearRequestValido();

        mockMvc.perform(post("/api/catalogo/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "5", roles = "VENDEDOR")
    void conRolVendedor_bodyCompleto_retorna201Created() throws Exception {
        RegistrarProductoRequest request = crearRequestValido();

        List<ImagenProducto> imagenes = List.of(
                new ImagenProducto("https://cdn.glossup.co/img1.webp"),
                new ImagenProducto("https://cdn.glossup.co/img2.webp")
        );
        List<ComposicionProducto> ingredientes = List.of(
                new ComposicionProducto(new Ingrediente(1L, new Inci("Ascorbic Acid"), "Vitamina C"), 15.0),
                new ComposicionProducto(new Ingrediente(2L, new Inci("Aqua"), null), null)
        );

        Producto productoGuardado = new Producto(
                100L, 5L, "Serum Vitamina C", "GlossLab", "Serum Facial",
                Dinero.de(85000.0), 50, 3.5, "INVIMA-2024", true,
                imagenes, ingredientes, null
        );

        when(registrarProductoUseCase.registrar(any(RegistrarProductoCommand.class)))
                .thenReturn(productoGuardado);

        mockMvc.perform(post("/api/catalogo/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.vendedorId").value(5))
                .andExpect(jsonPath("$.nombre").value("Serum Vitamina C"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.imagenes.length()").value(2))
                .andExpect(jsonPath("$.ingredientes.length()").value(2));
    }

    @Test
    @WithMockUser(username = "5", roles = "VENDEDOR")
    void conRolVendedor_sinIngredientes_retorna400BadRequest() throws Exception {
        RegistrarProductoRequest requestSinIngredientes = new RegistrarProductoRequest(
                "Serum Vitamina C",
                "GlossLab",
                "Serum Facial",
                BigDecimal.valueOf(85000.0),
                50,
                3.5,
                "INVIMA-2024",
                List.of("https://cdn.glossup.co/img1.webp"),
                List.of() // Vacío intencionalmente
        );

        mockMvc.perform(post("/api/catalogo/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestSinIngredientes)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Error de validacion"));
    }

    @Test
    @WithMockUser(username = "5", roles = "VENDEDOR")
    void conRolVendedor_nombreEnBlanco_retorna400BadRequest() throws Exception {
        RegistrarProductoRequest requestInvalido = new RegistrarProductoRequest(
                "", // Nombre inválido
                "GlossLab",
                "Serum Facial",
                BigDecimal.valueOf(85000.0),
                50,
                3.5,
                "INVIMA-2024",
                List.of("https://cdn.glossup.co/img1.webp"),
                List.of(new IngredienteRequest("Aqua", null))
        );

        mockMvc.perform(post("/api/catalogo/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "5", roles = "VENDEDOR")
    void conRolVendedor_precioNegativo_retorna400BadRequest() throws Exception {
        RegistrarProductoRequest requestInvalido = new RegistrarProductoRequest(
                "Serum Vitamina C",
                "GlossLab",
                "Serum Facial",
                BigDecimal.valueOf(-10.0), // Precio inválido
                50,
                3.5,
                "INVIMA-2024",
                List.of("https://cdn.glossup.co/img1.webp"),
                List.of(new IngredienteRequest("Aqua", null))
        );

        mockMvc.perform(post("/api/catalogo/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "5", roles = "VENDEDOR")
    void conRolVendedor_cuandoCasoDeUsoLanzaReglaNegocio_retorna422UnprocessableEntity() throws Exception {
        RegistrarProductoRequest request = crearRequestValido();

        when(registrarProductoUseCase.registrar(any(RegistrarProductoCommand.class)))
                .thenThrow(new ReglaNegocioException("El ingrediente ya está en la ficha: ascorbic acid"));

        mockMvc.perform(post("/api/catalogo/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("El ingrediente ya está en la ficha: ascorbic acid"));
    }

    @Test
    @WithMockUser(username = "5", roles = "VENDEDOR")
    void extraerIngredientes_conRolVendedor_retornaIngredientes() throws Exception {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "imagen", "etiqueta.jpg", "image/jpeg", "dummy-content".getBytes()
        );

        when(extraerIngredientesImagenUseCase.extraer(any(), any()))
                .thenReturn(List.of("aqua", "glycerin", "niacinamide"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/catalogo/productos/extraer-ingredientes")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredientes[0]").value("aqua"))
                .andExpect(jsonPath("$.ingredientes[1]").value("glycerin"))
                .andExpect(jsonPath("$.ingredientes[2]").value("niacinamide"));
    }

    @Test
    @WithMockUser(username = "5", roles = "VENDEDOR")
    void extraerIngredientes_marcaSoloLosQueCoincidenExactoConElCatalogo() throws Exception {
        when(extraerIngredientesImagenUseCase.extraer(any(), any())).thenReturn(List.of("glycerin", "agua de rosas"));
        when(gestionarProductoUseCase.buscarIngredientes("glycerin")).thenReturn(List.of(
                new com.glossup.catalogo.domain.model.Ingrediente(1L, new com.glossup.catalogo.domain.model.Inci("polyglycerin-3"), "Otro"),
                new com.glossup.catalogo.domain.model.Ingrediente(2L, new com.glossup.catalogo.domain.model.Inci("glycerin"), "Glicerina")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/catalogo/productos/extraer-ingredientes")
                        .file(new org.springframework.mock.web.MockMultipartFile("imagen", "e.jpg", "image/jpeg", "x".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enCatalogo.length()").value(1))
                .andExpect(jsonPath("$.enCatalogo[0].inci").value("glycerin"));
    }

    @Test
    void extraerIngredientes_sinAutenticacion_retorna401() throws Exception {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "imagen", "etiqueta.jpg", "image/jpeg", "dummy-content".getBytes()
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/catalogo/productos/extraer-ingredientes")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "5", roles = "VENDEDOR")
    void actualizarProducto_conRolVendedorPropietario_retorna200Ok() throws Exception {
        ActualizarProductoRequest request = crearActualizarRequestValido();

        Producto actualizado = new Producto(
                100L, 5L, "Serum Vitamina C Renovado", "GlossLab", "Serum Facial",
                Dinero.de(90000.0), 40, 3.6, "INVIMA-2024", true,
                List.of(new ImagenProducto("https://cdn.glossup.co/img-new.webp")),
                List.of(new ComposicionProducto(new Ingrediente(1L, new Inci("Ascorbic Acid"), "Vitamina C"), 15.0)),
                null
        );

        when(gestionarProductoUseCase.actualizar(any(ActualizarProductoCommand.class)))
                .thenReturn(actualizado);

        mockMvc.perform(put("/api/catalogo/productos/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.nombre").value("Serum Vitamina C Renovado"))
                .andExpect(jsonPath("$.precio").value(90000.0))
                .andExpect(jsonPath("$.stock").value(40));
    }

    @Test
    @WithMockUser(username = "99", roles = "VENDEDOR")
    void actualizarProducto_deOtroVendedor_retorna403Forbidden() throws Exception {
        ActualizarProductoRequest request = crearActualizarRequestValido();

        when(gestionarProductoUseCase.actualizar(any(ActualizarProductoCommand.class)))
                .thenThrow(new AccessDeniedException("Acceso denegado: no puedes modificar un producto de otro vendedor"));

        mockMvc.perform(put("/api/catalogo/productos/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void buscar_sinToken_retorna401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/catalogo/productos/buscar").param("q", "serum"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void buscar_conToken_retornaCoincidencias() throws Exception {
        Producto encontrado = new Producto(
                100L, 5L, "Serum Vitamina C", "GlossLab", "Serum Facial",
                Dinero.de(85000.0), 50, 3.5, "INVIMA-2024", true,
                List.of(new ImagenProducto("https://cdn.glossup.co/img1.webp")),
                List.of(new ComposicionProducto(new Ingrediente(1L, new Inci("Aqua"), null), null)),
                null
        );
        when(productoRepository.buscarPorPalabraClave("serum")).thenReturn(List.of(encontrado));

        mockMvc.perform(get("/api/catalogo/productos/buscar").param("q", "serum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Serum Vitamina C"));
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void listarCatalogo_ocultaAgotadosEInactivos() throws Exception {
        when(productoRepository.listarTodos()).thenReturn(List.of(
                productoCatalogo(1L, "Con stock", 5, true),
                productoCatalogo(2L, "Agotado", 0, true),
                productoCatalogo(3L, "Despublicado", 9, false)));

        mockMvc.perform(get("/api/catalogo/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Con stock"));
    }

    private static Producto productoCatalogo(Long id, String nombre, int stock, boolean activo) {
        return new Producto(id, 5L, nombre, "GlossLab", "Sérums", Dinero.de(10000.0), stock, null, null, activo,
                List.of(), List.of(new ComposicionProducto(new Ingrediente(1L, new Inci("Aqua"), null), null)), null);
    }

    @Test
    @WithMockUser(username = "7", roles = "CLIENTE")
    void buscar_palabraClaveEnBlanco_retornaListaVacia() throws Exception {
        mockMvc.perform(get("/api/catalogo/productos/buscar").param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "5", roles = "VENDEDOR")
    void eliminarProducto_conRolVendedorPropietario_retorna204NoContent() throws Exception {
        doNothing().when(gestionarProductoUseCase).eliminar(100L, 5L);

        mockMvc.perform(delete("/api/catalogo/productos/100"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "5", roles = "VENDEDOR")
    void eliminarProducto_inexistente_retorna404NotFound() throws Exception {
        doThrow(new NotFoundException("Producto no encontrado con ID: 999"))
                .when(gestionarProductoUseCase).eliminar(999L, 5L);

        mockMvc.perform(delete("/api/catalogo/productos/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

