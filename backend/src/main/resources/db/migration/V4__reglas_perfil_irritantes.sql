-- HU6 · Motor de compatibilidad producto–perfil (alergias e irritantes).
--
-- Las reglas de irritantes son conocimiento dermatológico, NO inventario: una regla
-- ("el retinol irrita la piel sensible") es válida aunque hoy ningún producto use ese
-- ingrediente. Por eso la regla referencia el INCI por nombre y no por FK a producto.
-- La tabla regla_perfil se creó vacía en V1 con una FK a ingrediente(id); la migramos a
-- nombre INCI (operación segura: no hay filas ni código que dependan de la columna vieja).
ALTER TABLE regla_perfil DROP CONSTRAINT IF EXISTS regla_perfil_ingrediente_id_fkey;
ALTER TABLE regla_perfil DROP COLUMN ingrediente_id;
ALTER TABLE regla_perfil ADD COLUMN ingrediente_inci VARCHAR(200) NOT NULL DEFAULT '';
ALTER TABLE regla_perfil ALTER COLUMN ingrediente_inci DROP DEFAULT;

CREATE INDEX idx_regla_perfil_condicion ON regla_perfil (condicion);

-- Base curada de irritantes conocidos. INCI y condición en minúsculas para cruce exacto con la
-- ficha del producto y con el perfil (tipo de piel: normal/seca/grasa/mixta/sensible; o afección).
INSERT INTO regla_perfil (ingrediente_inci, condicion, severidad, motivo) VALUES
 ('retinol', 'sensible', 'ALTA', 'El retinol puede causar enrojecimiento, descamación e irritación en piel sensible.'),
 ('retinol', 'rosacea',  'ALTA', 'El retinol suele agravar los brotes y el enrojecimiento de la rosácea.'),
 ('retinol', 'dermatitis','ALTA','El retinol irrita la piel con dermatitis activa y puede empeorar el cuadro.'),
 ('glycolic acid', 'sensible', 'ALTA', 'El ácido glicólico es un exfoliante fuerte que irrita la piel sensible.'),
 ('glycolic acid', 'rosacea',  'ALTA', 'Los ácidos AHA como el glicólico pueden desencadenar brotes de rosácea.'),
 ('salicylic acid', 'seca', 'MEDIA', 'El ácido salicílico reseca aún más la piel seca; usar con moderación.'),
 ('salicylic acid', 'sensible', 'MEDIA', 'El ácido salicílico puede irritar la piel sensible.'),
 ('benzoyl peroxide', 'sensible', 'ALTA', 'El peróxido de benzoílo irrita y reseca la piel sensible.'),
 ('benzoyl peroxide', 'rosacea', 'ALTA', 'El peróxido de benzoílo puede empeorar el enrojecimiento de la rosácea.'),
 ('sodium lauryl sulfate', 'seca', 'ALTA', 'El SLS elimina los lípidos naturales y reseca la piel seca.'),
 ('sodium lauryl sulfate', 'sensible', 'ALTA', 'El SLS es un tensioactivo agresivo que irrita la piel sensible.'),
 ('sodium lauryl sulfate', 'eczema', 'ALTA', 'El SLS es un irritante conocido que agrava el eczema.'),
 ('alcohol denat', 'seca', 'MEDIA', 'El alcohol desnaturalizado reseca la piel seca.'),
 ('alcohol denat', 'sensible', 'ALTA', 'El alcohol desnaturalizado irrita y deshidrata la piel sensible.'),
 ('parfum', 'sensible', 'ALTA', 'Las fragancias son una causa frecuente de irritación en piel sensible.'),
 ('parfum', 'dermatitis', 'ALTA', 'Las fragancias pueden desencadenar dermatitis de contacto.'),
 ('parfum', 'eczema', 'ALTA', 'Las fragancias suelen empeorar el eczema.'),
 ('limonene', 'sensible', 'MEDIA', 'El limoneno es un alérgeno de fragancia que puede irritar la piel sensible.'),
 ('linalool', 'sensible', 'MEDIA', 'El linalool es un alérgeno de fragancia que puede irritar la piel sensible.'),
 ('menthol', 'rosacea', 'ALTA', 'El mentol provoca vasodilatación y enrojecimiento en pieles con rosácea.'),
 ('menthol', 'sensible', 'MEDIA', 'El mentol puede causar sensación de escozor en piel sensible.');
