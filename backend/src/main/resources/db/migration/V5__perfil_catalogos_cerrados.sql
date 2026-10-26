-- Perfil dermatológico con catálogos cerrados (enums) en lugar de texto libre.
--
-- 1) Tablas `alergeno` y `condicion`: el reflejo en BD de los enums Alergeno, Afeccion y TipoPiel.
--    Todo lo que guarda códigos (perfil, reglas, mapeo de alérgenos) tiene FK a ellas, así un código
--    mal escrito en una semilla hace fallar la migración en vez de crear una regla que nunca dispara.
-- 2) `alergeno_ingrediente`: cada alérgeno (grupo) → sus nombres INCI, para que el motor detecte,
--    por ejemplo, methylparaben en alguien alérgico a PARABENOS.
-- 3) Migración de los datos de texto libre existentes. Lo que no se puede mapear se guarda en
--    tablas *_legacy: una alergia nunca se borra en silencio.
--
-- Fuentes: NACDG 2019–2020, ESSCA 2021/2022, serie basal latinoamericana en Colombia 2016–2021,
-- Reglamento (CE) 1223/2009 anexo III (26 alérgenos de fragancia), revisión de liberadores de
-- formaldehído (de Groot et al., Contact Dermatitis 2010).

-- ------------------------------------------------------------------ 1) catálogos cerrados
CREATE TABLE alergeno (codigo VARCHAR(60) PRIMARY KEY);
INSERT INTO alergeno (codigo) VALUES
 ('FRAGANCIA'), ('BALSAMO_DEL_PERU'), ('LIMONENO'), ('LINALOOL'), ('METILISOTIAZOLINONA'),
 ('BENZISOTIAZOLINONA'), ('FORMALDEHIDO'), ('METILDIBROMO_GLUTARONITRILO'), ('PARABENOS'),
 ('YODOPROPINIL_BUTILCARBAMATO'), ('PARAFENILENDIAMINA'), ('PERSULFATOS'), ('TIOGLICOLATOS'),
 ('ACRILATOS'), ('COLOFONIA'), ('LANOLINA'), ('PROPOLEO'), ('COCAMIDOPROPIL_BETAINA'),
 ('GLUCOSIDOS'), ('PROPILENGLICOL'), ('FILTROS_UV'), ('ACEITE_ARBOL_TE'), ('PLANTAS_COMPUESTAS'),
 ('TIMEROSAL');

-- Condiciones contra las que cruzan las reglas: tipos de piel + afecciones.
CREATE TABLE condicion (codigo VARCHAR(60) PRIMARY KEY);
INSERT INTO condicion (codigo) VALUES
 ('NORMAL'), ('SECA'), ('GRASA'), ('MIXTA'), ('SENSIBLE'),
 ('ACNE'), ('ROSACEA'), ('DERMATITIS_ATOPICA'), ('DERMATITIS_SEBORREICA'), ('DERMATITIS_CONTACTO'),
 ('DERMATITIS_PERIORAL'), ('PSORIASIS'), ('HIPERPIGMENTACION'), ('QUERATOSIS_PILAR'),
 ('FOTOENVEJECIMIENTO'), ('CASPA'), ('CUERO_CABELLUDO_SENSIBLE'), ('CUERO_CABELLUDO_GRASO'),
 ('CAIDA_DEL_CABELLO'), ('CABELLO_TENIDO'), ('CABELLO_DANADO'), ('EMBARAZO_LACTANCIA');

-- ------------------------------------------------------------------ 2) alérgeno → INCI
CREATE TEMP TABLE semilla_alergeno (alergeno VARCHAR(60), inci VARCHAR(200)) ON COMMIT DROP;
INSERT INTO semilla_alergeno (alergeno, inci) VALUES
 -- Fragancia: nombres genéricos + los 26 alérgenos de fragancia del anexo III (UE)
 ('FRAGANCIA', 'parfum'), ('FRAGANCIA', 'fragrance'), ('FRAGANCIA', 'aroma'),
 ('FRAGANCIA', 'amyl cinnamal'), ('FRAGANCIA', 'amylcinnamyl alcohol'), ('FRAGANCIA', 'anise alcohol'),
 ('FRAGANCIA', 'benzyl alcohol'), ('FRAGANCIA', 'benzyl benzoate'), ('FRAGANCIA', 'benzyl cinnamate'),
 ('FRAGANCIA', 'benzyl salicylate'), ('FRAGANCIA', 'butylphenyl methylpropional'), ('FRAGANCIA', 'cinnamal'),
 ('FRAGANCIA', 'cinnamyl alcohol'), ('FRAGANCIA', 'citral'), ('FRAGANCIA', 'citronellol'),
 ('FRAGANCIA', 'coumarin'), ('FRAGANCIA', 'eugenol'), ('FRAGANCIA', 'evernia furfuracea extract'),
 ('FRAGANCIA', 'evernia prunastri extract'), ('FRAGANCIA', 'farnesol'), ('FRAGANCIA', 'geraniol'),
 ('FRAGANCIA', 'hexyl cinnamal'), ('FRAGANCIA', 'hydroxycitronellal'),
 ('FRAGANCIA', 'hydroxyisohexyl 3-cyclohexene carboxaldehyde'), ('FRAGANCIA', 'isoeugenol'),
 ('FRAGANCIA', 'alpha-isomethyl ionone'), ('FRAGANCIA', 'limonene'), ('FRAGANCIA', 'linalool'),
 ('FRAGANCIA', 'methyl 2-octynoate'),
 ('BALSAMO_DEL_PERU', 'myroxylon pereirae resin'), ('BALSAMO_DEL_PERU', 'myroxylon pereirae oil'),
 ('BALSAMO_DEL_PERU', 'myroxylon pereirae extract'), ('BALSAMO_DEL_PERU', 'benzyl benzoate'),
 ('BALSAMO_DEL_PERU', 'benzyl cinnamate'),
 ('LIMONENO', 'limonene'),
 ('LINALOOL', 'linalool'),
 ('METILISOTIAZOLINONA', 'methylisothiazolinone'), ('METILISOTIAZOLINONA', 'methylchloroisothiazolinone'),
 ('BENZISOTIAZOLINONA', 'benzisothiazolinone'),
 ('FORMALDEHIDO', 'formaldehyde'), ('FORMALDEHIDO', 'methylene glycol'), ('FORMALDEHIDO', 'quaternium-15'),
 ('FORMALDEHIDO', 'dmdm hydantoin'), ('FORMALDEHIDO', 'imidazolidinyl urea'), ('FORMALDEHIDO', 'diazolidinyl urea'),
 ('FORMALDEHIDO', '2-bromo-2-nitropropane-1,3-diol'), ('FORMALDEHIDO', 'sodium hydroxymethylglycinate'),
 ('FORMALDEHIDO', 'polyoxymethylene urea'), ('FORMALDEHIDO', 'glyoxal'),
 ('METILDIBROMO_GLUTARONITRILO', 'methyldibromo glutaronitrile'),
 ('PARABENOS', 'methylparaben'), ('PARABENOS', 'ethylparaben'), ('PARABENOS', 'propylparaben'),
 ('PARABENOS', 'butylparaben'), ('PARABENOS', 'isobutylparaben'), ('PARABENOS', 'isopropylparaben'),
 ('PARABENOS', 'benzylparaben'), ('PARABENOS', 'sodium methylparaben'), ('PARABENOS', 'sodium propylparaben'),
 ('YODOPROPINIL_BUTILCARBAMATO', 'iodopropynyl butylcarbamate'),
 ('PARAFENILENDIAMINA', 'p-phenylenediamine'), ('PARAFENILENDIAMINA', 'toluene-2,5-diamine'),
 ('PARAFENILENDIAMINA', 'toluene-2,5-diamine sulfate'), ('PARAFENILENDIAMINA', 'p-aminophenol'),
 ('PERSULFATOS', 'ammonium persulfate'), ('PERSULFATOS', 'potassium persulfate'), ('PERSULFATOS', 'sodium persulfate'),
 ('TIOGLICOLATOS', 'glyceryl thioglycolate'), ('TIOGLICOLATOS', 'ammonium thioglycolate'),
 ('TIOGLICOLATOS', 'thioglycolic acid'),
 ('ACRILATOS', 'hydroxyethyl methacrylate'), ('ACRILATOS', 'hydroxypropyl methacrylate'),
 ('ACRILATOS', 'methyl methacrylate'), ('ACRILATOS', 'ethyl methacrylate'),
 ('ACRILATOS', 'di-hema trimethylhexyl dicarbamate'),
 ('COLOFONIA', 'colophonium'), ('COLOFONIA', 'rosin'), ('COLOFONIA', 'hydrogenated rosin'),
 ('COLOFONIA', 'glyceryl rosinate'),
 ('LANOLINA', 'lanolin'), ('LANOLINA', 'lanolin alcohol'), ('LANOLINA', 'lanolin oil'),
 ('LANOLINA', 'hydrogenated lanolin'), ('LANOLINA', 'acetylated lanolin'),
 ('PROPOLEO', 'propolis extract'), ('PROPOLEO', 'propolis cera'),
 ('COCAMIDOPROPIL_BETAINA', 'cocamidopropyl betaine'), ('COCAMIDOPROPIL_BETAINA', 'cocamidopropyl dimethylamine'),
 ('GLUCOSIDOS', 'decyl glucoside'), ('GLUCOSIDOS', 'lauryl glucoside'), ('GLUCOSIDOS', 'coco-glucoside'),
 ('GLUCOSIDOS', 'caprylyl/capryl glucoside'), ('GLUCOSIDOS', 'cetearyl glucoside'), ('GLUCOSIDOS', 'arachidyl glucoside'),
 ('PROPILENGLICOL', 'propylene glycol'),
 ('FILTROS_UV', 'benzophenone-3'), ('FILTROS_UV', 'benzophenone-4'), ('FILTROS_UV', 'octocrylene'),
 ('FILTROS_UV', 'ethylhexyl methoxycinnamate'),
 ('ACEITE_ARBOL_TE', 'melaleuca alternifolia leaf oil'),
 ('PLANTAS_COMPUESTAS', 'chamomilla recutita flower extract'), ('PLANTAS_COMPUESTAS', 'anthemis nobilis flower extract'),
 ('PLANTAS_COMPUESTAS', 'calendula officinalis flower extract'), ('PLANTAS_COMPUESTAS', 'arnica montana flower extract'),
 ('PLANTAS_COMPUESTAS', 'achillea millefolium extract'), ('PLANTAS_COMPUESTAS', 'tanacetum parthenium extract'),
 ('TIMEROSAL', 'thimerosal');

-- Los INCI del mapeo forman parte de la ontología de ingredientes.
INSERT INTO ingrediente (nombre_inci) SELECT DISTINCT inci FROM semilla_alergeno
ON CONFLICT (nombre_inci) DO NOTHING;

CREATE TABLE alergeno_ingrediente (
    alergeno         VARCHAR(60)  NOT NULL REFERENCES alergeno(codigo),
    ingrediente_inci VARCHAR(200) NOT NULL REFERENCES ingrediente(nombre_inci),
    PRIMARY KEY (alergeno, ingrediente_inci)
);
INSERT INTO alergeno_ingrediente (alergeno, ingrediente_inci) SELECT alergeno, inci FROM semilla_alergeno;

-- ------------------------------------------------------------------ 3) datos existentes del perfil
-- Texto libre conocido → código. Lo demás se respalda en *_legacy y sale de la tabla.
CREATE TEMP TABLE mapa_texto (tabla VARCHAR(20), texto VARCHAR(120), codigo VARCHAR(60)) ON COMMIT DROP;
INSERT INTO mapa_texto (tabla, texto, codigo) VALUES
 ('alergia', 'parfum', 'FRAGANCIA'), ('alergia', 'fragrance', 'FRAGANCIA'), ('alergia', 'fragancia', 'FRAGANCIA'),
 ('alergia', 'fragancias', 'FRAGANCIA'), ('alergia', 'perfume', 'FRAGANCIA'), ('alergia', 'perfumes', 'FRAGANCIA'),
 ('alergia', 'aroma', 'FRAGANCIA'), ('alergia', 'fragrance mix', 'FRAGANCIA'),
 ('alergia', 'balsamo del peru', 'BALSAMO_DEL_PERU'), ('alergia', 'bálsamo del perú', 'BALSAMO_DEL_PERU'),
 ('alergia', 'myroxylon pereirae', 'BALSAMO_DEL_PERU'), ('alergia', 'peru balsam', 'BALSAMO_DEL_PERU'),
 ('alergia', 'limonene', 'LIMONENO'), ('alergia', 'limoneno', 'LIMONENO'), ('alergia', 'linalool', 'LINALOOL'),
 ('alergia', 'methylisothiazolinone', 'METILISOTIAZOLINONA'), ('alergia', 'metilisotiazolinona', 'METILISOTIAZOLINONA'),
 ('alergia', 'methylchloroisothiazolinone', 'METILISOTIAZOLINONA'), ('alergia', 'mi', 'METILISOTIAZOLINONA'),
 ('alergia', 'mci', 'METILISOTIAZOLINONA'), ('alergia', 'mci/mi', 'METILISOTIAZOLINONA'),
 ('alergia', 'benzisothiazolinone', 'BENZISOTIAZOLINONA'), ('alergia', 'benzisotiazolinona', 'BENZISOTIAZOLINONA'),
 ('alergia', 'formaldehyde', 'FORMALDEHIDO'), ('alergia', 'formaldehido', 'FORMALDEHIDO'),
 ('alergia', 'formaldehído', 'FORMALDEHIDO'), ('alergia', 'formol', 'FORMALDEHIDO'),
 ('alergia', 'quaternium-15', 'FORMALDEHIDO'), ('alergia', 'dmdm hydantoin', 'FORMALDEHIDO'),
 ('alergia', 'imidazolidinyl urea', 'FORMALDEHIDO'), ('alergia', 'diazolidinyl urea', 'FORMALDEHIDO'),
 ('alergia', 'methyldibromo glutaronitrile', 'METILDIBROMO_GLUTARONITRILO'),
 ('alergia', 'parabenos', 'PARABENOS'), ('alergia', 'parabeno', 'PARABENOS'), ('alergia', 'parabens', 'PARABENOS'),
 ('alergia', 'paraben', 'PARABENOS'), ('alergia', 'methylparaben', 'PARABENOS'), ('alergia', 'propylparaben', 'PARABENOS'),
 ('alergia', 'butylparaben', 'PARABENOS'), ('alergia', 'ethylparaben', 'PARABENOS'),
 ('alergia', 'iodopropynyl butylcarbamate', 'YODOPROPINIL_BUTILCARBAMATO'),
 ('alergia', 'ppd', 'PARAFENILENDIAMINA'), ('alergia', 'p-phenylenediamine', 'PARAFENILENDIAMINA'),
 ('alergia', 'paraphenylenediamine', 'PARAFENILENDIAMINA'), ('alergia', 'parafenilendiamina', 'PARAFENILENDIAMINA'),
 ('alergia', 'tinte', 'PARAFENILENDIAMINA'), ('alergia', 'tintes', 'PARAFENILENDIAMINA'),
 ('alergia', 'persulfatos', 'PERSULFATOS'), ('alergia', 'ammonium persulfate', 'PERSULFATOS'),
 ('alergia', 'tioglicolato', 'TIOGLICOLATOS'), ('alergia', 'thioglycolate', 'TIOGLICOLATOS'),
 ('alergia', 'glyceryl thioglycolate', 'TIOGLICOLATOS'),
 ('alergia', 'acrilatos', 'ACRILATOS'), ('alergia', 'acrylates', 'ACRILATOS'), ('alergia', 'hema', 'ACRILATOS'),
 ('alergia', 'colofonia', 'COLOFONIA'), ('alergia', 'colophonium', 'COLOFONIA'), ('alergia', 'rosin', 'COLOFONIA'),
 ('alergia', 'lanolina', 'LANOLINA'), ('alergia', 'lanolin', 'LANOLINA'),
 ('alergia', 'propoleo', 'PROPOLEO'), ('alergia', 'propóleo', 'PROPOLEO'), ('alergia', 'propolis', 'PROPOLEO'),
 ('alergia', 'cocamidopropyl betaine', 'COCAMIDOPROPIL_BETAINA'), ('alergia', 'cocamidopropil betaina', 'COCAMIDOPROPIL_BETAINA'),
 ('alergia', 'cocamidopropil betaína', 'COCAMIDOPROPIL_BETAINA'),
 ('alergia', 'decyl glucoside', 'GLUCOSIDOS'), ('alergia', 'lauryl glucoside', 'GLUCOSIDOS'),
 ('alergia', 'coco-glucoside', 'GLUCOSIDOS'), ('alergia', 'glucosidos', 'GLUCOSIDOS'),
 ('alergia', 'propylene glycol', 'PROPILENGLICOL'), ('alergia', 'propilenglicol', 'PROPILENGLICOL'),
 ('alergia', 'oxybenzone', 'FILTROS_UV'), ('alergia', 'oxibenzona', 'FILTROS_UV'),
 ('alergia', 'benzophenone-3', 'FILTROS_UV'), ('alergia', 'octocrylene', 'FILTROS_UV'), ('alergia', 'octocrileno', 'FILTROS_UV'),
 ('alergia', 'tea tree', 'ACEITE_ARBOL_TE'), ('alergia', 'tea tree oil', 'ACEITE_ARBOL_TE'),
 ('alergia', 'arbol de te', 'ACEITE_ARBOL_TE'), ('alergia', 'árbol de té', 'ACEITE_ARBOL_TE'),
 ('alergia', 'aceite de arbol de te', 'ACEITE_ARBOL_TE'), ('alergia', 'aceite de árbol de té', 'ACEITE_ARBOL_TE'),
 ('alergia', 'melaleuca alternifolia leaf oil', 'ACEITE_ARBOL_TE'),
 ('alergia', 'manzanilla', 'PLANTAS_COMPUESTAS'), ('alergia', 'chamomile', 'PLANTAS_COMPUESTAS'),
 ('alergia', 'calendula', 'PLANTAS_COMPUESTAS'), ('alergia', 'caléndula', 'PLANTAS_COMPUESTAS'),
 ('alergia', 'arnica', 'PLANTAS_COMPUESTAS'), ('alergia', 'árnica', 'PLANTAS_COMPUESTAS'),
 ('alergia', 'timerosal', 'TIMEROSAL'), ('alergia', 'thimerosal', 'TIMEROSAL'), ('alergia', 'thiomersal', 'TIMEROSAL'),
 ('afeccion', 'acne', 'ACNE'), ('afeccion', 'acné', 'ACNE'),
 ('afeccion', 'rosacea', 'ROSACEA'), ('afeccion', 'rosácea', 'ROSACEA'),
 ('afeccion', 'dermatitis', 'DERMATITIS_ATOPICA'), ('afeccion', 'dermatitis atopica', 'DERMATITIS_ATOPICA'),
 ('afeccion', 'dermatitis atópica', 'DERMATITIS_ATOPICA'), ('afeccion', 'eczema', 'DERMATITIS_ATOPICA'),
 ('afeccion', 'eccema', 'DERMATITIS_ATOPICA'),
 ('afeccion', 'dermatitis seborreica', 'DERMATITIS_SEBORREICA'), ('afeccion', 'seborrea', 'DERMATITIS_SEBORREICA'),
 ('afeccion', 'dermatitis de contacto', 'DERMATITIS_CONTACTO'), ('afeccion', 'dermatitis por contacto', 'DERMATITIS_CONTACTO'),
 ('afeccion', 'dermatitis perioral', 'DERMATITIS_PERIORAL'), ('afeccion', 'psoriasis', 'PSORIASIS'),
 ('afeccion', 'melasma', 'HIPERPIGMENTACION'), ('afeccion', 'manchas', 'HIPERPIGMENTACION'),
 ('afeccion', 'hiperpigmentacion', 'HIPERPIGMENTACION'), ('afeccion', 'hiperpigmentación', 'HIPERPIGMENTACION'),
 ('afeccion', 'paño', 'HIPERPIGMENTACION'),
 ('afeccion', 'queratosis pilar', 'QUERATOSIS_PILAR'), ('afeccion', 'keratosis pilaris', 'QUERATOSIS_PILAR'),
 ('afeccion', 'arrugas', 'FOTOENVEJECIMIENTO'), ('afeccion', 'envejecimiento', 'FOTOENVEJECIMIENTO'),
 ('afeccion', 'fotoenvejecimiento', 'FOTOENVEJECIMIENTO'), ('afeccion', 'lineas de expresion', 'FOTOENVEJECIMIENTO'),
 ('afeccion', 'líneas de expresión', 'FOTOENVEJECIMIENTO'),
 ('afeccion', 'caspa', 'CASPA'), ('afeccion', 'dandruff', 'CASPA'),
 ('afeccion', 'cuero cabelludo sensible', 'CUERO_CABELLUDO_SENSIBLE'), ('afeccion', 'picor', 'CUERO_CABELLUDO_SENSIBLE'),
 ('afeccion', 'cuero cabelludo graso', 'CUERO_CABELLUDO_GRASO'),
 ('afeccion', 'caida del cabello', 'CAIDA_DEL_CABELLO'), ('afeccion', 'caída del cabello', 'CAIDA_DEL_CABELLO'),
 ('afeccion', 'caida de cabello', 'CAIDA_DEL_CABELLO'), ('afeccion', 'alopecia', 'CAIDA_DEL_CABELLO'),
 ('afeccion', 'cabello teñido', 'CABELLO_TENIDO'), ('afeccion', 'cabello tenido', 'CABELLO_TENIDO'),
 ('afeccion', 'decolorado', 'CABELLO_TENIDO'),
 ('afeccion', 'cabello dañado', 'CABELLO_DANADO'), ('afeccion', 'cabello danado', 'CABELLO_DANADO'),
 ('afeccion', 'cabello maltratado', 'CABELLO_DANADO'),
 ('afeccion', 'embarazo', 'EMBARAZO_LACTANCIA'), ('afeccion', 'embarazada', 'EMBARAZO_LACTANCIA'),
 ('afeccion', 'lactancia', 'EMBARAZO_LACTANCIA');

-- Alergias
UPDATE perfil_alergia a SET nombre = m.codigo
FROM mapa_texto m WHERE m.tabla = 'alergia' AND lower(trim(a.nombre)) = m.texto;
UPDATE perfil_alergia SET nombre = upper(trim(nombre))
WHERE upper(trim(nombre)) IN (SELECT codigo FROM alergeno);
CREATE TABLE perfil_alergia_legacy AS SELECT * FROM perfil_alergia WHERE nombre NOT IN (SELECT codigo FROM alergeno);
DELETE FROM perfil_alergia WHERE nombre NOT IN (SELECT codigo FROM alergeno);
DELETE FROM perfil_alergia a USING perfil_alergia b
WHERE a.perfil_id = b.perfil_id AND a.nombre = b.nombre AND a.id > b.id;
ALTER TABLE perfil_alergia ADD CONSTRAINT fk_perfil_alergia_alergeno FOREIGN KEY (nombre) REFERENCES alergeno(codigo);

-- Afecciones (solo códigos de Afeccion, no tipos de piel)
UPDATE perfil_afeccion a SET nombre = m.codigo
FROM mapa_texto m WHERE m.tabla = 'afeccion' AND lower(trim(a.nombre)) = m.texto;
UPDATE perfil_afeccion SET nombre = upper(trim(nombre))
WHERE upper(trim(nombre)) IN (SELECT codigo FROM condicion);
CREATE TABLE perfil_afeccion_legacy AS SELECT * FROM perfil_afeccion
WHERE nombre NOT IN (SELECT codigo FROM condicion) OR nombre IN ('NORMAL', 'SECA', 'GRASA', 'MIXTA', 'SENSIBLE');
DELETE FROM perfil_afeccion
WHERE nombre NOT IN (SELECT codigo FROM condicion) OR nombre IN ('NORMAL', 'SECA', 'GRASA', 'MIXTA', 'SENSIBLE');
DELETE FROM perfil_afeccion a USING perfil_afeccion b
WHERE a.perfil_id = b.perfil_id AND a.nombre = b.nombre AND a.id > b.id;
ALTER TABLE perfil_afeccion ADD CONSTRAINT fk_perfil_afeccion_condicion FOREIGN KEY (nombre) REFERENCES condicion(codigo);

-- Tipo de cabello: ya llegaba como código desde el front; lo que no sea código válido se limpia.
UPDATE perfil_dermatologico SET tipo_cabello = NULL
WHERE tipo_cabello IS NOT NULL AND tipo_cabello NOT IN (
 'LISO_1A', 'LISO_1B', 'LISO_1C', 'ONDULADO_2A', 'ONDULADO_2B', 'ONDULADO_2C',
 'RIZADO_3A', 'RIZADO_3B', 'RIZADO_3C', 'MUY_RIZADO_4A', 'MUY_RIZADO_4B', 'MUY_RIZADO_4C');

-- ------------------------------------------------------------------ 4) reglas de V4 → códigos + FKs
UPDATE regla_perfil SET ingrediente_inci = 'alcohol denat.' WHERE ingrediente_inci = 'alcohol denat';
UPDATE regla_perfil SET condicion = 'DERMATITIS_CONTACTO' WHERE condicion = 'dermatitis' AND ingrediente_inci = 'parfum';
UPDATE regla_perfil SET condicion = 'DERMATITIS_ATOPICA' WHERE condicion IN ('dermatitis', 'eczema');
UPDATE regla_perfil SET condicion = upper(condicion);
INSERT INTO ingrediente (nombre_inci) SELECT DISTINCT ingrediente_inci FROM regla_perfil
ON CONFLICT (nombre_inci) DO NOTHING;
ALTER TABLE regla_perfil ADD CONSTRAINT fk_regla_perfil_condicion FOREIGN KEY (condicion) REFERENCES condicion(codigo);
ALTER TABLE regla_perfil ADD CONSTRAINT fk_regla_perfil_ingrediente FOREIGN KEY (ingrediente_inci) REFERENCES ingrediente(nombre_inci);
ALTER TABLE regla_perfil ADD CONSTRAINT uq_regla_perfil UNIQUE (ingrediente_inci, condicion);
