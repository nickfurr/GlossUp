-- HU7 · Compatibilidad producto–perfil por tipo de piel, comedogenia y pH.
--
-- 1) ingrediente.comedogenicidad: grado 0–5 de la Tabla I de Fulton JE Jr., "Comedogenicity and
--    irritancy of commonly used ingredients in skin care products", J Soc Cosmet Chem 40:321-333
--    (1989). Cuando la tabla da un rango ("0-2", depende de la materia prima) se guarda el valor
--    alto: el motor prefiere un aviso de más a un falso negativo (RNF-01).
-- 2) regla_rango: alerta cuando un valor del producto cae en un rango, según la condición del
--    perfil. Dos atributos:
--    · COMEDOGENICIDAD de cada ingrediente. Nunca ALTA: el grado se midió en oreja de conejo con
--      el ingrediente puro, y en producto terminado suele no reproducirse (Draelos & DiNardo,
--      J Am Acad Dermatol 54:507-512, 2006). Es un aviso, no una contraindicación.
--    · PH del producto. La superficie de la piel sana está por debajo de 5 (Lambers et al., Int J
--      Cosmet Sci 28:359-370, 2006); los productos alcalinos (jabones, pH 9–10) elevan el pH y
--      dañan la barrera, sobre todo en piel seca, sensible o atópica (Ali & Yosipovitch, Acta Derm
--      Venereol 93:261-267, 2013). Por debajo de 3.5 los exfoliantes ácidos irritan: el CIR limita
--      los alfahidroxiácidos de uso doméstico a pH ≥ 3.5 (CIR, Int J Toxicol 17 Suppl 1, 1998).
--      En acné, un jabón alcalino dio más lesiones inflamatorias que un syndet ácido (Korting et
--      al., Infection 15:104-108, 1987).
-- 3) Las reglas comedogénicas sueltas de V7 que ahora cubre el grado se retiran para no duplicar
--    alertas. "acetylated lanolin" se retira porque Fulton le da grado 0: la comedogénica es el
--    alcohol de lanolina acetilada (grado 4), por su aditivo acetato de cetilo.

-- ------------------------------------------------------------------ 1) Grado comedogénico
ALTER TABLE ingrediente ADD COLUMN comedogenicidad SMALLINT
    CONSTRAINT chk_ingrediente_comedogenicidad CHECK (comedogenicidad BETWEEN 0 AND 5);

CREATE TEMP TABLE fulton (inci VARCHAR(200) PRIMARY KEY, grado SMALLINT NOT NULL) ON COMMIT DROP;
INSERT INTO fulton (inci, grado) VALUES
 -- I. Lanolinas
 ('acetylated lanolin', 0), ('lanolin', 1), ('lanolin alcohol', 2), ('lanolin oil', 1),
 -- II. Ácidos grasos y ésteres
 ('myristic acid', 3), ('palmitic acid', 2), ('stearic acid', 3), ('ascorbyl palmitate', 2),
 ('ethylhexyl palmitate', 4), ('isopropyl myristate', 5), ('isopropyl palmitate', 4), ('myristyl myristate', 5),
 -- III. Alcoholes, glicoles, azúcares y derivados
 ('alcohol', 0), ('alcohol denat.', 0), ('isopropyl alcohol', 0), ('myristyl alcohol', 2), ('cetyl alcohol', 2),
 ('cetearyl alcohol', 2), ('stearyl alcohol', 2), ('ceteareth-20', 2), ('propylene glycol', 0),
 ('butylene glycol', 1), ('hexylene glycol', 2), ('sorbitan oleate', 3), ('polysorbate 20', 0),
 ('polysorbate 80', 0), ('glycerin', 0), ('sorbitol', 0), ('glyceryl stearate', 1), ('glyceryl stearate se', 3),
 ('caprylic/capric triglyceride', 1), ('polyglyceryl-3 diisostearate', 4), ('peg-100 stearate', 0), ('steareth-2', 2),
 -- IV. Ceras
 ('cera alba', 2), ('copernicia cerifera cera', 1), ('euphorbia cerifera cera', 1), ('simmondsia chinensis seed oil', 2),
 -- V. Espesantes
 ('cellulose gum', 0), ('magnesium aluminum silicate', 0), ('carbomer', 1), ('bentonite', 0), ('kaolin', 0), ('talc', 1),
 -- VI. Aceites ("coconut butter" de la tabla = aceite de coco)
 ('theobroma cacao seed butter', 4), ('cocos nucifera oil', 4), ('sesamum indicum seed oil', 3), ('zea mays germ oil', 3),
 ('persea gratissima oil', 3), ('oenothera biennis oil', 3), ('glycine soja oil', 3), ('olea europaea fruit oil', 2),
 ('prunus amygdalus dulcis oil', 2), ('hydrogenated polyisobutene', 1), ('ricinus communis seed oil', 1),
 ('hydrogenated castor oil', 1), ('squalane', 1), ('helianthus annuus seed oil', 0), ('paraffinum liquidum', 2),
 -- X. Vitaminas y extractos
 ('tocopherol', 3), ('tocopheryl acetate', 0), ('retinyl palmitate', 3), ('panthenol', 0),
 ('chamomilla recutita flower extract', 0),
 -- XI. Conservantes y aditivos
 ('methylparaben', 0), ('propylparaben', 0), ('allantoin', 0), ('sodium hyaluronate', 0), ('sodium pca', 0),
 ('sulfur', 0), ('magnesium stearate', 1), ('zinc oxide', 1), ('triethanolamine', 2),
 -- XII. Filtros UV
 ('benzophenone-3', 0), ('ethylhexyl methoxycinnamate', 0), ('ethylhexyl salicylate', 0);

DO $$
DECLARE faltan TEXT;
BEGIN
    SELECT string_agg(inci, ', ') INTO faltan FROM fulton WHERE inci NOT IN (SELECT nombre_inci FROM ingrediente);
    IF faltan IS NOT NULL THEN
        RAISE EXCEPTION 'Ingredientes de la tabla de Fulton que no están en el catálogo: %', faltan;
    END IF;
END $$;

UPDATE ingrediente i SET comedogenicidad = f.grado FROM fulton f WHERE i.nombre_inci = f.inci;

-- ------------------------------------------------------------------ 2) Reglas por rango
CREATE TABLE regla_rango (
    id         BIGSERIAL PRIMARY KEY,
    atributo   VARCHAR(20)  NOT NULL CHECK (atributo IN ('COMEDOGENICIDAD', 'PH')),
    condicion  VARCHAR(60)  NOT NULL REFERENCES condicion(codigo),
    desde      NUMERIC(3,1) NOT NULL,
    hasta      NUMERIC(3,1) NOT NULL,
    severidad  VARCHAR(10)  NOT NULL CHECK (severidad IN ('BAJA', 'MEDIA')),
    motivo     TEXT         NOT NULL,
    CHECK (desde <= hasta)
);

INSERT INTO regla_rango (atributo, condicion, desde, hasta, severidad, motivo) VALUES
 -- Comedogenia (grado 0–5 del ingrediente)
 ('COMEDOGENICIDAD', 'ACNE',   4, 5, 'MEDIA', 'Grado comedogénico alto (4–5 de 5, Fulton 1989): favorece comedones en piel con acné.'),
 ('COMEDOGENICIDAD', 'ACNE',   3, 3, 'BAJA',  'Grado comedogénico moderado (3 de 5, Fulton 1989): vigila si aparecen comedones.'),
 ('COMEDOGENICIDAD', 'GRASA',  4, 5, 'MEDIA', 'Grado comedogénico alto (4–5 de 5, Fulton 1989): puede obstruir los poros en piel grasa.'),
 ('COMEDOGENICIDAD', 'MIXTA',  4, 5, 'BAJA',  'Grado comedogénico alto (4–5 de 5, Fulton 1989): evita aplicarlo en la zona T, donde tu piel es grasa.'),
 -- pH del producto: ácido (exfoliantes, pH < 3.5)
 ('PH', 'SENSIBLE',            0.0, 3.4, 'MEDIA', 'Producto muy ácido (pH < 3.5): los exfoliantes a ese pH irritan la piel sensible.'),
 ('PH', 'ROSACEA',             0.0, 3.4, 'MEDIA', 'Producto muy ácido (pH < 3.5): puede provocar ardor y enrojecimiento en la rosácea.'),
 ('PH', 'DERMATITIS_ATOPICA',  0.0, 3.4, 'MEDIA', 'Producto muy ácido (pH < 3.5): irrita una barrera cutánea ya alterada.'),
 ('PH', 'DERMATITIS_PERIORAL', 0.0, 3.4, 'MEDIA', 'Producto muy ácido (pH < 3.5): los irritantes tópicos empeoran la dermatitis perioral.'),
 -- pH del producto: alcalino (jabones, pH ≥ 7.5)
 ('PH', 'SENSIBLE',            7.5, 14.0, 'MEDIA', 'Producto alcalino (pH ≥ 7.5): eleva el pH de la piel y debilita su barrera.'),
 ('PH', 'SECA',                7.5, 14.0, 'MEDIA', 'Producto alcalino (pH ≥ 7.5): arrastra los lípidos de la piel y aumenta la resequedad.'),
 ('PH', 'DERMATITIS_ATOPICA',  7.5, 14.0, 'MEDIA', 'Producto alcalino (pH ≥ 7.5): eleva el pH y activa proteasas que empeoran la dermatitis atópica.'),
 ('PH', 'ROSACEA',             7.5, 14.0, 'MEDIA', 'Producto alcalino (pH ≥ 7.5): los limpiadores alcalinos irritan la piel con rosácea.'),
 ('PH', 'ACNE',                7.5, 14.0, 'BAJA',  'Producto alcalino (pH ≥ 7.5): el jabón alcalino se asoció a más lesiones inflamatorias de acné que un limpiador ácido.');

-- ------------------------------------------------------------------ 3) Reglas sueltas que ahora cubre el grado
DELETE FROM regla_perfil
WHERE condicion IN ('ACNE', 'GRASA')
  AND ingrediente_inci IN ('isopropyl myristate', 'isopropyl palmitate', 'myristyl myristate',
                           'cocos nucifera oil', 'theobroma cacao seed butter', 'acetylated lanolin');
