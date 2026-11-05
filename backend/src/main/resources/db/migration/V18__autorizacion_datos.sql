-- Prueba de la autorización del titular (Ley 1581 art. 9 y 17 b; Decreto 1377 art. 7 y 8; auditoría NC-T11).
-- Las cuentas creadas antes de este cambio quedan en NULL: no hay prueba de que hayan aceptado.

-- Política de tratamiento aceptada al registrarse: cuándo y qué versión.
ALTER TABLE usuario
    ADD COLUMN politica_aceptada_en TIMESTAMPTZ,
    ADD COLUMN politica_version     VARCHAR(20);

-- Autorización explícita y facultativa para los datos de salud (alergias, afecciones, biotipo).
ALTER TABLE perfil_dermatologico
    ADD COLUMN autorizacion_sensibles_en TIMESTAMPTZ;
