package com.glossup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada unico de la aplicacion GlossUP.
 *
 * <p>GlossUP es un monolito modular con arquitectura hexagonal (ADR-01). Cada modulo de negocio
 * (usuarios, perfil, catalogo, compatibilidad, recomendacion) se organiza en las capas
 * domain / application / infrastructure y se comunica con el exterior unicamente a traves de
 * puertos (interfaces) y adaptadores.</p>
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class GlossupApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlossupApplication.class, args);
    }
}
