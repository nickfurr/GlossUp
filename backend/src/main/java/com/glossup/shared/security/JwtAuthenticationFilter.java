package com.glossup.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que toma el token de la cookie de sesion del navegador (o del encabezado "Authorization: Bearer" de un
 * cliente de la API), lo valida y, si es correcto, establece la autenticacion en el contexto de seguridad.
 * Es stateless: no mantiene sesion en el servidor; la revocacion la da {@link SesionVigentePort}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectProvider<SesionVigentePort> sesiones;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectProvider<SesionVigentePort> sesiones) {
        this.sesiones = sesiones;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : CookieSesion.leer(request);
        if (token != null) {
            boolean valido;
            try {
                valido = jwtService.esValido(token) && sesionVigente(token);
            } catch (DataAccessResourceFailureException e) {
                // Este filtro corre antes que los controladores, asi que el 503 de GlobalExceptionHandler
                // no lo alcanza: sin esto la BD caida respondia 500 (auditoria R-16).
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
            if (valido) {
                String usuario = jwtService.extraerSubject(token);
                List<SimpleGrantedAuthority> authorities = jwtService.extraerRoles(token).stream()
                        .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                        .toList();
                var auth = new UsernamePasswordAuthenticationToken(usuario, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean sesionVigente(String token) {
        SesionVigentePort puerto = sesiones.getIfAvailable();
        return puerto == null || puerto.vigente(jwtService.extraerSubject(token), jwtService.extraerEmision(token));
    }
}
