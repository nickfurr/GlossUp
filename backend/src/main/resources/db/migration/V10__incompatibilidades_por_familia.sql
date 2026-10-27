-- HU8 · Cruces producto–producto: completa los pares de V7 por familia de ingredientes.
--
-- V7 sembró pares sueltos y dejó huecos de la misma regla (había retinol + ácido láctico pero no
-- retinal + ácido láctico; el tinte solo cubría la PPD). Aquí cada familia se cruza completa con
-- la otra; los pares que ya existían conservan su motivo (ON CONFLICT DO NOTHING).
--
-- · Retinoide × exfoliante ácido (AHA/BHA/LHA): ambos aceleran el recambio y adelgazan la capa
--   córnea; usados a la vez suman irritación. Es práctica clínica y advertencia de etiqueta, sin
--   ensayo comparativo directo: por eso MEDIA (aviso), no ALTA.
-- · Alisado/permanente (tioglicolatos) × tinte oxidativo o decoloración (persulfatos): dos
--   servicios químicos el mismo día sobreprocesan la queratina; la práctica de cosmetología es
--   separarlos al menos 1–2 semanas. El peróxido de hidrógeno queda fuera: es el neutralizante
--   de la propia permanente.

CREATE TEMP TABLE familia (nombre VARCHAR(40), inci VARCHAR(200)) ON COMMIT DROP;
INSERT INTO familia (nombre, inci) VALUES
 ('retinoide', 'retinol'), ('retinoide', 'retinal'),
 ('exfoliante', 'glycolic acid'), ('exfoliante', 'lactic acid'), ('exfoliante', 'mandelic acid'),
 ('exfoliante', 'salicylic acid'), ('exfoliante', 'capryloyl salicylic acid'),
 ('tioglicolato', 'ammonium thioglycolate'), ('tioglicolato', 'glyceryl thioglycolate'),
 ('tioglicolato', 'thioglycolic acid'),
 ('oxidativo', 'p-phenylenediamine'), ('oxidativo', 'toluene-2,5-diamine'),
 ('oxidativo', 'toluene-2,5-diamine sulfate'), ('oxidativo', 'ammonium persulfate'),
 ('oxidativo', 'potassium persulfate'), ('oxidativo', 'sodium persulfate');

DO $$
DECLARE faltan TEXT;
BEGIN
    SELECT string_agg(inci, ', ') INTO faltan FROM familia WHERE inci NOT IN (SELECT nombre_inci FROM ingrediente);
    IF faltan IS NOT NULL THEN
        RAISE EXCEPTION 'Ingredientes de familias de incompatibilidad que no están en el catálogo: %', faltan;
    END IF;
END $$;

CREATE TEMP TABLE cruce_familia (a VARCHAR(40), b VARCHAR(40), severidad VARCHAR(10), motivo TEXT) ON COMMIT DROP;
INSERT INTO cruce_familia (a, b, severidad, motivo) VALUES
 ('retinoide', 'exfoliante', 'MEDIA',
  'Retinoide + exfoliante ácido (AHA/BHA) suman irritación, descamación y sequedad; úsalos en noches alternas.'),
 ('tioglicolato', 'oxidativo', 'MEDIA',
  'Alisado o permanente y tinte o decoloración el mismo día sobreprocesan el cabello; sepáralos al menos dos semanas.');

INSERT INTO regla_incompatibilidad (ingrediente_a, ingrediente_b, severidad, motivo)
SELECT ia.id, ib.id, c.severidad, c.motivo
FROM cruce_familia c
JOIN familia fa ON fa.nombre = c.a
JOIN familia fb ON fb.nombre = c.b
JOIN ingrediente ia ON ia.nombre_inci = fa.inci
JOIN ingrediente ib ON ib.nombre_inci = fb.inci
ON CONFLICT (ingrediente_a, ingrediente_b) DO NOTHING;
