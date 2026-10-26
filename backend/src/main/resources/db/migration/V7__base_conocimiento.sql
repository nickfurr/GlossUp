-- Base de conocimiento dermatológica del motor de compatibilidad.
--
--   regla_perfil            ingrediente → NO recomendado para una condición (HU6/HU7)
--   beneficio               ingrediente → recomendado para una condición (recomendaciones)
--   regla_incompatibilidad  ingrediente ↔ ingrediente que no conviene combinar (HU8)
--
-- Severidad: ALTA = contraindicación clara (el producto se marca como no compatible);
-- MEDIA = aviso relevante; BAJA = informativo. Fuentes principales:
--   · National Rosacea Society, encuestas de desencadenantes (1997, n=1.023; 2025, n=1.104)
--   · Consenso Delphi de dermatólogos cosméticos (JAAD 2025): activos por indicación
--   · Revisiones de seguridad en embarazo (retinoides, hidroquinona, ácido salicílico)
--   · Malassezia y ácido oleico (estudio split-scalp; revisión sobre aceites capilares)
--   · Estabilidad peróxido de benzoilo + retinoides (JCAD 2010 y literatura previa)
-- Deliberadamente excluido: "niacinamida + vitamina C" (mito sin sustento clínico).
-- Todas las tablas tienen FK a ingrediente/condicion: un nombre mal escrito hace fallar la migración.

-- El dominio hace Severidad.valueOf(...): un valor fuera del enum debe fallar aquí, no en producción.
ALTER TABLE regla_perfil ADD CONSTRAINT chk_regla_perfil_severidad CHECK (severidad IN ('BAJA', 'MEDIA', 'ALTA'));
ALTER TABLE regla_incompatibilidad ADD CONSTRAINT chk_regla_incompatibilidad_severidad CHECK (severidad IN ('BAJA', 'MEDIA', 'ALTA'));

-- ------------------------------------------------------------------ 1) Irritantes por condición
INSERT INTO regla_perfil (ingrediente_inci, condicion, severidad, motivo) VALUES
 -- Embarazo y lactancia
 ('retinol', 'EMBARAZO_LACTANCIA', 'ALTA', 'Los retinoides se desaconsejan en embarazo y lactancia por su potencial teratogénico; por precaución también los tópicos.'),
 ('retinal', 'EMBARAZO_LACTANCIA', 'ALTA', 'Los retinoides se desaconsejan en embarazo y lactancia por su potencial teratogénico; por precaución también los tópicos.'),
 ('retinyl palmitate', 'EMBARAZO_LACTANCIA', 'ALTA', 'Los retinoides se desaconsejan en embarazo y lactancia por su potencial teratogénico; por precaución también los tópicos.'),
 ('hydroxypinacolone retinoate', 'EMBARAZO_LACTANCIA', 'ALTA', 'Los retinoides se desaconsejan en embarazo y lactancia por su potencial teratogénico; por precaución también los tópicos.'),
 ('hydroquinone', 'EMBARAZO_LACTANCIA', 'ALTA', 'La hidroquinona tiene una absorción cutánea alta y datos de seguridad limitados en embarazo.'),
 ('minoxidil', 'EMBARAZO_LACTANCIA', 'ALTA', 'El minoxidil es un medicamento contraindicado en embarazo y lactancia.'),
 ('methylene glycol', 'EMBARAZO_LACTANCIA', 'ALTA', 'Los alisados con formol liberan formaldehído, irritante y carcinógeno; se evitan en embarazo.'),
 ('formaldehyde', 'EMBARAZO_LACTANCIA', 'ALTA', 'El formaldehído es irritante y carcinógeno; se evita en embarazo.'),
 ('salicylic acid', 'EMBARAZO_LACTANCIA', 'MEDIA', 'En baja concentración suele considerarse aceptable, pero se evitan peelings y concentraciones altas en embarazo.'),
 ('benzophenone-3', 'EMBARAZO_LACTANCIA', 'BAJA', 'La oxibenzona se absorbe a nivel sistémico; en embarazo suele preferirse un filtro mineral.'),
 ('p-phenylenediamine', 'EMBARAZO_LACTANCIA', 'BAJA', 'Por precaución se suele aconsejar posponer los tintes de oxidación durante el embarazo.'),
 -- Piel sensible
 ('fragrance', 'SENSIBLE', 'ALTA', 'Las fragancias son una causa frecuente de irritación en piel sensible.'),
 ('retinal', 'SENSIBLE', 'ALTA', 'El retinal puede causar enrojecimiento, descamación e irritación en piel sensible.'),
 ('hydroxypinacolone retinoate', 'SENSIBLE', 'MEDIA', 'Retinoide más suave, pero puede irritar la piel sensible al inicio.'),
 ('retinyl palmitate', 'SENSIBLE', 'BAJA', 'Retinoide suave; en piel sensible conviene introducirlo de forma progresiva.'),
 ('lactic acid', 'SENSIBLE', 'MEDIA', 'Los AHA pueden causar escozor y enrojecimiento en piel sensible.'),
 ('mandelic acid', 'SENSIBLE', 'BAJA', 'AHA suave, aunque puede escocer en piel muy sensible.'),
 ('camphor', 'SENSIBLE', 'MEDIA', 'El alcanfor puede causar escozor e irritación en piel sensible.'),
 ('methylisothiazolinone', 'SENSIBLE', 'MEDIA', 'Conservante muy sensibilizante; frecuente causa de dermatitis de contacto.'),
 ('methylchloroisothiazolinone', 'SENSIBLE', 'MEDIA', 'Conservante muy sensibilizante; frecuente causa de dermatitis de contacto.'),
 ('mentha piperita oil', 'SENSIBLE', 'MEDIA', 'Los aceites esenciales refrescantes pueden irritar la piel sensible.'),
 ('eucalyptus globulus leaf oil', 'SENSIBLE', 'MEDIA', 'Los aceites esenciales pueden irritar la piel sensible.'),
 ('lavandula angustifolia oil', 'SENSIBLE', 'MEDIA', 'El aceite de lavanda se oxida y puede sensibilizar la piel sensible.'),
 ('citrus limon peel oil', 'SENSIBLE', 'MEDIA', 'Los aceites cítricos pueden irritar y fotosensibilizar la piel sensible.'),
 ('citrus aurantium bergamia peel oil', 'SENSIBLE', 'MEDIA', 'Los aceites cítricos pueden irritar y fotosensibilizar la piel sensible.'),
 ('ammonium lauryl sulfate', 'SENSIBLE', 'MEDIA', 'Tensioactivo agresivo que altera la barrera de la piel sensible.'),
 ('sodium laureth sulfate', 'SENSIBLE', 'BAJA', 'Tensioactivo aniónico; menos irritante que el SLS pero puede resecar la piel sensible.'),
 ('isopropyl alcohol', 'SENSIBLE', 'MEDIA', 'El alcohol isopropílico irrita y deshidrata la piel sensible.'),
 ('hamamelis virginiana water', 'SENSIBLE', 'BAJA', 'El agua de hamamelis suele contener alcohol y puede irritar la piel sensible.'),
 ('ascorbic acid', 'SENSIBLE', 'BAJA', 'El ácido ascórbico se formula a pH muy bajo y puede escocer en piel sensible.'),
 ('propylene glycol', 'SENSIBLE', 'BAJA', 'Puede causar irritación o sensibilización en algunas personas.'),
 -- Piel seca
 ('ammonium lauryl sulfate', 'SECA', 'MEDIA', 'Los sulfatos fuertes eliminan los lípidos naturales y resecan la piel seca.'),
 ('isopropyl alcohol', 'SECA', 'MEDIA', 'El alcohol isopropílico reseca la piel seca.'),
 ('benzoyl peroxide', 'SECA', 'MEDIA', 'El peróxido de benzoilo reseca y descama la piel.'),
 ('kaolin', 'SECA', 'BAJA', 'Las arcillas absorben grasa y pueden resecar la piel seca.'),
 ('bentonite', 'SECA', 'BAJA', 'Las arcillas absorben grasa y pueden resecar la piel seca.'),
 ('sodium tallowate', 'SECA', 'BAJA', 'Los jabones alcalinos resecan la piel seca.'),
 ('sodium cocoate', 'SECA', 'BAJA', 'Los jabones alcalinos resecan la piel seca.'),
 -- Piel grasa y acné: ingredientes comedogénicos
 ('isopropyl myristate', 'GRASA', 'MEDIA', 'Emoliente comedogénico: puede obstruir los poros en piel grasa.'),
 ('isopropyl palmitate', 'GRASA', 'MEDIA', 'Emoliente comedogénico: puede obstruir los poros en piel grasa.'),
 ('myristyl myristate', 'GRASA', 'MEDIA', 'Emoliente comedogénico: puede obstruir los poros en piel grasa.'),
 ('cocos nucifera oil', 'GRASA', 'MEDIA', 'El aceite de coco es comedogénico en piel grasa.'),
 ('theobroma cacao seed butter', 'GRASA', 'MEDIA', 'La manteca de cacao es comedogénica en piel grasa.'),
 ('acetylated lanolin', 'GRASA', 'MEDIA', 'La lanolina acetilada es comedogénica.'),
 ('triticum vulgare germ oil', 'GRASA', 'MEDIA', 'El aceite de germen de trigo es comedogénico.'),
 ('algae extract', 'GRASA', 'BAJA', 'Algunos extractos de algas son comedogénicos.'),
 ('isopropyl myristate', 'ACNE', 'MEDIA', 'Emoliente comedogénico: puede favorecer comedones en piel con acné.'),
 ('isopropyl palmitate', 'ACNE', 'MEDIA', 'Emoliente comedogénico: puede favorecer comedones en piel con acné.'),
 ('myristyl myristate', 'ACNE', 'MEDIA', 'Emoliente comedogénico: puede favorecer comedones en piel con acné.'),
 ('cocos nucifera oil', 'ACNE', 'MEDIA', 'El aceite de coco es comedogénico y puede empeorar el acné.'),
 ('theobroma cacao seed butter', 'ACNE', 'MEDIA', 'La manteca de cacao es comedogénica y puede empeorar el acné.'),
 ('acetylated lanolin', 'ACNE', 'MEDIA', 'La lanolina acetilada es comedogénica y puede empeorar el acné.'),
 ('triticum vulgare germ oil', 'ACNE', 'MEDIA', 'El aceite de germen de trigo es comedogénico.'),
 ('algae extract', 'ACNE', 'MEDIA', 'Algunos extractos de algas son comedogénicos.'),
 ('oleic acid', 'ACNE', 'BAJA', 'El ácido oleico puede favorecer la comedogénesis.'),
 ('sodium lauryl sulfate', 'ACNE', 'BAJA', 'La irritación por SLS puede empeorar las lesiones de acné.'),
 -- Rosácea (encuestas de la National Rosacea Society)
 ('alcohol denat.', 'ROSACEA', 'ALTA', 'El alcohol es el irritante más reportado en rosácea (63-66 % de pacientes, National Rosacea Society).'),
 ('isopropyl alcohol', 'ROSACEA', 'ALTA', 'El alcohol es el irritante más reportado en rosácea (National Rosacea Society).'),
 ('parfum', 'ROSACEA', 'ALTA', 'Las fragancias empeoran la rosácea en el 57 % de pacientes (National Rosacea Society).'),
 ('fragrance', 'ROSACEA', 'ALTA', 'Las fragancias empeoran la rosácea en el 57 % de pacientes (National Rosacea Society).'),
 ('hamamelis virginiana water', 'ROSACEA', 'MEDIA', 'El hamamelis irrita al 31 % de pacientes con rosácea (National Rosacea Society).'),
 ('hamamelis virginiana leaf extract', 'ROSACEA', 'MEDIA', 'El hamamelis irrita al 31 % de pacientes con rosácea (National Rosacea Society).'),
 ('mentha piperita oil', 'ROSACEA', 'MEDIA', 'Menta y eucalipto agravan la rosácea en cerca de una cuarta parte de pacientes.'),
 ('eucalyptus globulus leaf oil', 'ROSACEA', 'MEDIA', 'Menta y eucalipto agravan la rosácea en cerca de una cuarta parte de pacientes.'),
 ('camphor', 'ROSACEA', 'MEDIA', 'El alcanfor es irritante y vasodilatador; puede empeorar el enrojecimiento.'),
 ('sodium lauryl sulfate', 'ROSACEA', 'MEDIA', 'Los sulfatos agravan la rosácea en cerca de una cuarta parte de pacientes.'),
 ('ammonium lauryl sulfate', 'ROSACEA', 'MEDIA', 'Los sulfatos agravan la rosácea en cerca de una cuarta parte de pacientes.'),
 ('retinal', 'ROSACEA', 'ALTA', 'Los retinoides suelen agravar los brotes y el enrojecimiento de la rosácea.'),
 ('lactic acid', 'ROSACEA', 'MEDIA', 'Los AHA pueden desencadenar brotes de rosácea.'),
 ('salicylic acid', 'ROSACEA', 'MEDIA', 'El ácido salicílico puede irritar la piel con rosácea.'),
 ('ascorbic acid', 'ROSACEA', 'BAJA', 'El ácido ascórbico a pH bajo puede escocer en piel con rosácea.'),
 -- Dermatitis atópica
 ('fragrance', 'DERMATITIS_ATOPICA', 'ALTA', 'Las fragancias suelen empeorar la dermatitis atópica.'),
 ('alcohol denat.', 'DERMATITIS_ATOPICA', 'MEDIA', 'El alcohol reseca e irrita la piel atópica.'),
 ('ammonium lauryl sulfate', 'DERMATITIS_ATOPICA', 'MEDIA', 'Los sulfatos fuertes dañan la barrera de la piel atópica.'),
 ('sodium laureth sulfate', 'DERMATITIS_ATOPICA', 'MEDIA', 'Los sulfatos dañan la barrera de la piel atópica.'),
 ('methylisothiazolinone', 'DERMATITIS_ATOPICA', 'MEDIA', 'Conservante muy sensibilizante sobre una barrera ya alterada.'),
 ('lavandula angustifolia oil', 'DERMATITIS_ATOPICA', 'MEDIA', 'Los aceites esenciales pueden sensibilizar la piel atópica.'),
 ('melaleuca alternifolia leaf oil', 'DERMATITIS_ATOPICA', 'MEDIA', 'Los aceites esenciales pueden sensibilizar la piel atópica.'),
 ('mentha piperita oil', 'DERMATITIS_ATOPICA', 'MEDIA', 'Los aceites esenciales pueden sensibilizar la piel atópica.'),
 ('citrus limon peel oil', 'DERMATITIS_ATOPICA', 'MEDIA', 'Los aceites esenciales pueden sensibilizar la piel atópica.'),
 ('glycolic acid', 'DERMATITIS_ATOPICA', 'MEDIA', 'Los exfoliantes ácidos irritan la piel atópica con barrera alterada.'),
 ('salicylic acid', 'DERMATITIS_ATOPICA', 'MEDIA', 'Los exfoliantes ácidos irritan la piel atópica con barrera alterada.'),
 ('benzoyl peroxide', 'DERMATITIS_ATOPICA', 'MEDIA', 'El peróxido de benzoilo irrita y reseca la piel atópica.'),
 ('lanolin', 'DERMATITIS_ATOPICA', 'BAJA', 'La lanolina sensibiliza con más frecuencia a personas con dermatitis atópica.'),
 ('urea', 'DERMATITIS_ATOPICA', 'BAJA', 'En concentraciones altas la urea puede escocer sobre piel inflamada.'),
 ('propylene glycol', 'DERMATITIS_ATOPICA', 'BAJA', 'Puede causar escozor o sensibilización en piel atópica.'),
 -- Antecedente de dermatitis de contacto: alérgenos con mayor riesgo de sensibilización
 ('fragrance', 'DERMATITIS_CONTACTO', 'ALTA', 'Las fragancias son la primera causa de dermatitis de contacto por cosméticos.'),
 ('methylisothiazolinone', 'DERMATITIS_CONTACTO', 'MEDIA', 'Uno de los alérgenos más frecuentes en las series de parche (NACDG 2019-2020: 13,8 %).'),
 ('methylchloroisothiazolinone', 'DERMATITIS_CONTACTO', 'MEDIA', 'Isotiazolinona con alto riesgo de sensibilización.'),
 ('formaldehyde', 'DERMATITIS_CONTACTO', 'MEDIA', 'Alérgeno frecuente en las series de parche.'),
 ('quaternium-15', 'DERMATITIS_CONTACTO', 'MEDIA', 'Liberador de formaldehído con riesgo de sensibilización.'),
 ('dmdm hydantoin', 'DERMATITIS_CONTACTO', 'MEDIA', 'Liberador de formaldehído con riesgo de sensibilización.'),
 ('p-phenylenediamine', 'DERMATITIS_CONTACTO', 'MEDIA', 'La PPD es de los alérgenos más frecuentes en la serie europea (6 %).'),
 ('myroxylon pereirae resin', 'DERMATITIS_CONTACTO', 'MEDIA', 'El bálsamo del Perú es de los alérgenos más frecuentes en la serie europea (8 %).'),
 ('propolis extract', 'DERMATITIS_CONTACTO', 'BAJA', 'El propóleo es un alérgeno en aumento en las series de parche.'),
 ('melaleuca alternifolia leaf oil', 'DERMATITIS_CONTACTO', 'BAJA', 'El aceite de árbol de té oxidado puede sensibilizar.'),
 -- Dermatitis seborreica y caspa (Malassezia)
 ('oleic acid', 'DERMATITIS_SEBORREICA', 'MEDIA', 'El ácido oleico, metabolito de Malassezia, induce descamación en personas susceptibles.'),
 ('olea europaea fruit oil', 'DERMATITIS_SEBORREICA', 'MEDIA', 'El aceite de oliva, rico en ácido oleico, favorece el crecimiento de Malassezia.'),
 ('alcohol denat.', 'DERMATITIS_SEBORREICA', 'BAJA', 'El alcohol irrita la piel con dermatitis seborreica.'),
 ('oleic acid', 'CASPA', 'MEDIA', 'El ácido oleico induce descamación tipo caspa en personas susceptibles (estudio split-scalp).'),
 ('olea europaea fruit oil', 'CASPA', 'MEDIA', 'El aceite de oliva, rico en ácido oleico, favorece el crecimiento de Malassezia.'),
 -- Dermatitis perioral
 ('petrolatum', 'DERMATITIS_PERIORAL', 'MEDIA', 'Las cremas oclusivas pesadas se asocian a la dermatitis perioral.'),
 ('paraffinum liquidum', 'DERMATITIS_PERIORAL', 'MEDIA', 'Las cremas oclusivas pesadas se asocian a la dermatitis perioral.'),
 ('isopropyl myristate', 'DERMATITIS_PERIORAL', 'MEDIA', 'Los emolientes oclusivos pueden empeorar la dermatitis perioral.'),
 ('sodium lauryl sulfate', 'DERMATITIS_PERIORAL', 'MEDIA', 'El SLS (también en pastas dentales) se asocia a la dermatitis perioral.'),
 ('cinnamal', 'DERMATITIS_PERIORAL', 'MEDIA', 'El cinamal (saborizante de canela) se asocia a dermatitis perioral y queilitis.'),
 -- Psoriasis
 ('alcohol denat.', 'PSORIASIS', 'MEDIA', 'El alcohol reseca e irrita las placas; la irritación puede desencadenar nuevas lesiones.'),
 ('sodium lauryl sulfate', 'PSORIASIS', 'MEDIA', 'El SLS irrita la piel; la irritación puede desencadenar nuevas lesiones.'),
 ('parfum', 'PSORIASIS', 'BAJA', 'Las fragancias pueden irritar la piel con psoriasis.'),
 -- Manchas
 ('citrus aurantium bergamia peel oil', 'HIPERPIGMENTACION', 'ALTA', 'Contiene bergapteno: con exposición solar causa manchas (fitofotodermatitis).'),
 ('citrus limon peel oil', 'HIPERPIGMENTACION', 'MEDIA', 'El aceite de limón es fotosensibilizante y puede manchar con el sol.'),
 -- Queratosis pilar
 ('sodium lauryl sulfate', 'QUERATOSIS_PILAR', 'BAJA', 'Los limpiadores agresivos resecan y empeoran la queratosis pilar.'),
 ('sodium tallowate', 'QUERATOSIS_PILAR', 'BAJA', 'Los jabones alcalinos resecan y empeoran la queratosis pilar.'),
 -- Cuero cabelludo sensible
 ('sodium lauryl sulfate', 'CUERO_CABELLUDO_SENSIBLE', 'MEDIA', 'Los sulfatos fuertes irritan el cuero cabelludo sensible.'),
 ('ammonium lauryl sulfate', 'CUERO_CABELLUDO_SENSIBLE', 'MEDIA', 'Los sulfatos fuertes irritan el cuero cabelludo sensible.'),
 ('parfum', 'CUERO_CABELLUDO_SENSIBLE', 'MEDIA', 'Las fragancias pueden causar picor en el cuero cabelludo sensible.'),
 ('fragrance', 'CUERO_CABELLUDO_SENSIBLE', 'MEDIA', 'Las fragancias pueden causar picor en el cuero cabelludo sensible.'),
 ('methylisothiazolinone', 'CUERO_CABELLUDO_SENSIBLE', 'MEDIA', 'Conservante sensibilizante frecuente en champús.'),
 ('menthol', 'CUERO_CABELLUDO_SENSIBLE', 'MEDIA', 'El mentol puede causar escozor en el cuero cabelludo sensible.'),
 ('mentha piperita oil', 'CUERO_CABELLUDO_SENSIBLE', 'MEDIA', 'El aceite de menta puede causar escozor en el cuero cabelludo sensible.'),
 ('p-phenylenediamine', 'CUERO_CABELLUDO_SENSIBLE', 'MEDIA', 'Los tintes de oxidación irritan el cuero cabelludo sensible.'),
 ('ammonium persulfate', 'CUERO_CABELLUDO_SENSIBLE', 'MEDIA', 'Los decolorantes irritan el cuero cabelludo sensible.'),
 ('ammonium thioglycolate', 'CUERO_CABELLUDO_SENSIBLE', 'MEDIA', 'Los alisados y permanentes irritan el cuero cabelludo sensible.'),
 -- Cuero cabelludo graso
 ('cocos nucifera oil', 'CUERO_CABELLUDO_GRASO', 'BAJA', 'Los aceites pesados aumentan la sensación de grasa en el cuero cabelludo.'),
 ('petrolatum', 'CUERO_CABELLUDO_GRASO', 'BAJA', 'Los oclusivos pesados se acumulan en el cuero cabelludo graso.'),
 ('paraffinum liquidum', 'CUERO_CABELLUDO_GRASO', 'BAJA', 'Los oclusivos pesados se acumulan en el cuero cabelludo graso.'),
 -- Caída del cabello
 ('ammonium thioglycolate', 'CAIDA_DEL_CABELLO', 'MEDIA', 'Los tioglicolatos debilitan la queratina y aumentan la rotura.'),
 ('methylene glycol', 'CAIDA_DEL_CABELLO', 'MEDIA', 'Los alisados con formol dañan la fibra y el cuero cabelludo.'),
 ('ammonium persulfate', 'CAIDA_DEL_CABELLO', 'MEDIA', 'La decoloración repetida aumenta la rotura del cabello.'),
 ('hydrogen peroxide', 'CAIDA_DEL_CABELLO', 'BAJA', 'Los oxidantes debilitan la fibra capilar.'),
 -- Cabello teñido
 ('sodium lauryl sulfate', 'CABELLO_TENIDO', 'MEDIA', 'Los sulfatos fuertes arrastran el color y aceleran su desvanecimiento.'),
 ('ammonium lauryl sulfate', 'CABELLO_TENIDO', 'MEDIA', 'Los sulfatos fuertes arrastran el color y aceleran su desvanecimiento.'),
 ('sodium laureth sulfate', 'CABELLO_TENIDO', 'BAJA', 'Los sulfatos aceleran el desvanecimiento del color.'),
 ('sodium c14-16 olefin sulfonate', 'CABELLO_TENIDO', 'BAJA', 'Tensioactivo fuerte que puede arrastrar el color.'),
 -- Cabello dañado
 ('sodium lauryl sulfate', 'CABELLO_DANADO', 'MEDIA', 'Los sulfatos fuertes resecan y levantan la cutícula del cabello dañado.'),
 ('ammonium lauryl sulfate', 'CABELLO_DANADO', 'MEDIA', 'Los sulfatos fuertes resecan y levantan la cutícula del cabello dañado.'),
 ('alcohol denat.', 'CABELLO_DANADO', 'MEDIA', 'Los alcoholes de cadena corta resecan la fibra capilar.'),
 ('isopropyl alcohol', 'CABELLO_DANADO', 'MEDIA', 'Los alcoholes de cadena corta resecan la fibra capilar.'),
 ('ammonium persulfate', 'CABELLO_DANADO', 'MEDIA', 'La decoloración causa daño oxidativo a la fibra.'),
 ('hydrogen peroxide', 'CABELLO_DANADO', 'MEDIA', 'Los oxidantes causan daño a la fibra capilar.'),
 ('ammonium thioglycolate', 'CABELLO_DANADO', 'MEDIA', 'Los tioglicolatos rompen enlaces de la queratina y debilitan el cabello.')
ON CONFLICT (ingrediente_inci, condicion) DO NOTHING;

-- ------------------------------------------------------------------ 2) Beneficios por condición
CREATE TABLE beneficio (
    id               BIGSERIAL PRIMARY KEY,
    ingrediente_inci VARCHAR(200) NOT NULL REFERENCES ingrediente(nombre_inci),
    condicion        VARCHAR(60)  NOT NULL REFERENCES condicion(codigo),
    motivo           TEXT         NOT NULL,
    CONSTRAINT uq_beneficio UNIQUE (ingrediente_inci, condicion)
);

INSERT INTO beneficio (ingrediente_inci, condicion, motivo) VALUES
 -- Acné (consenso Delphi JAAD 2025 / guías de acné)
 ('salicylic acid', 'ACNE', 'Exfolia dentro del poro y reduce comedones.'),
 ('benzoyl peroxide', 'ACNE', 'Antibacteriano frente a C. acnes; consenso para acné y piel grasa.'),
 ('azelaic acid', 'ACNE', 'Antiinflamatorio y antibacteriano; consenso para acné y manchas.'),
 ('niacinamide', 'ACNE', 'Antiinflamatorio y seborregulador.'),
 ('retinol', 'ACNE', 'Normaliza la queratinización del folículo; consenso para acné.'),
 ('retinal', 'ACNE', 'Retinoide con acción antiacné.'),
 ('glycolic acid', 'ACNE', 'Exfoliante; consenso para acné.'),
 ('sulfur', 'ACNE', 'Antibacteriano y queratolítico clásico.'),
 ('zinc pca', 'ACNE', 'Seborregulador.'),
 ('capryloyl salicylic acid', 'ACNE', 'Derivado lipofílico del ácido salicílico.'),
 ('melaleuca alternifolia leaf oil', 'ACNE', 'Al 5 % mostró mejoría frente a placebo (evidencia moderada).'),
 -- Rosácea
 ('azelaic acid', 'ROSACEA', 'Tratamiento de primera línea en rosácea papulopustular.'),
 ('niacinamide', 'ROSACEA', 'Reduce rojeces y refuerza la barrera; consenso para enrojecimiento.'),
 ('zinc oxide', 'ROSACEA', 'Filtro mineral bien tolerado; consenso para enrojecimiento.'),
 ('titanium dioxide', 'ROSACEA', 'Filtro mineral bien tolerado.'),
 ('sulfur', 'ROSACEA', 'Usado clásicamente en rosácea.'),
 ('centella asiatica extract', 'ROSACEA', 'Calmante y reparador.'),
 ('glycyrrhiza glabra root extract', 'ROSACEA', 'Calmante antirrojeces.'),
 ('panthenol', 'ROSACEA', 'Calmante y reparador de barrera.'),
 ('allantoin', 'ROSACEA', 'Calmante.'),
 -- Dermatitis atópica
 ('ceramide np', 'DERMATITIS_ATOPICA', 'Repone los lípidos de barrera deficientes en piel atópica.'),
 ('ceramide ap', 'DERMATITIS_ATOPICA', 'Repone los lípidos de barrera deficientes en piel atópica.'),
 ('ceramide eop', 'DERMATITIS_ATOPICA', 'Repone los lípidos de barrera deficientes en piel atópica.'),
 ('cholesterol', 'DERMATITIS_ATOPICA', 'Lípido de barrera que acompaña a las ceramidas.'),
 ('avena sativa kernel flour', 'DERMATITIS_ATOPICA', 'Avena coloidal: protector cutáneo reconocido para eccema.'),
 ('petrolatum', 'DERMATITIS_ATOPICA', 'Oclusivo de referencia para reducir la pérdida de agua.'),
 ('glycerin', 'DERMATITIS_ATOPICA', 'Humectante bien tolerado.'),
 ('butyrospermum parkii butter', 'DERMATITIS_ATOPICA', 'Emoliente que suaviza la piel seca atópica.'),
 ('squalane', 'DERMATITIS_ATOPICA', 'Emoliente bien tolerado.'),
 ('panthenol', 'DERMATITIS_ATOPICA', 'Calmante y reparador de barrera.'),
 ('niacinamide', 'DERMATITIS_ATOPICA', 'Estimula la síntesis de ceramidas.'),
 ('urea', 'DERMATITIS_ATOPICA', 'En baja concentración hidrata la piel atópica.'),
 -- Dermatitis de contacto
 ('petrolatum', 'DERMATITIS_CONTACTO', 'Oclusivo con bajísimo potencial alergénico.'),
 ('ceramide np', 'DERMATITIS_CONTACTO', 'Repara la barrera tras la dermatitis.'),
 ('avena sativa kernel flour', 'DERMATITIS_CONTACTO', 'Protector cutáneo calmante.'),
 ('panthenol', 'DERMATITIS_CONTACTO', 'Calmante y reparador.'),
 ('allantoin', 'DERMATITIS_CONTACTO', 'Calmante.'),
 -- Dermatitis seborreica y caspa
 ('zinc pyrithione', 'DERMATITIS_SEBORREICA', 'Antifúngico frente a Malassezia.'),
 ('piroctone olamine', 'DERMATITIS_SEBORREICA', 'Antifúngico frente a Malassezia.'),
 ('ketoconazole', 'DERMATITIS_SEBORREICA', 'Antifúngico de referencia en dermatitis seborreica.'),
 ('selenium disulfide', 'DERMATITIS_SEBORREICA', 'Antifúngico y citostático.'),
 ('climbazole', 'DERMATITIS_SEBORREICA', 'Antifúngico.'),
 ('salicylic acid', 'DERMATITIS_SEBORREICA', 'Queratolítico que retira escamas.'),
 ('sulfur', 'DERMATITIS_SEBORREICA', 'Queratolítico y antifúngico.'),
 ('zinc pyrithione', 'CASPA', 'Antifúngico frente a Malassezia.'),
 ('piroctone olamine', 'CASPA', 'Antifúngico frente a Malassezia.'),
 ('ketoconazole', 'CASPA', 'Antifúngico de referencia.'),
 ('selenium disulfide', 'CASPA', 'Antifúngico y citostático.'),
 ('climbazole', 'CASPA', 'Antifúngico.'),
 ('salicylic acid', 'CASPA', 'Queratolítico que retira escamas.'),
 ('melaleuca alternifolia leaf oil', 'CASPA', 'Champú al 5 % mejoró la caspa frente a placebo.'),
 -- Dermatitis perioral
 ('azelaic acid', 'DERMATITIS_PERIORAL', 'Antiinflamatorio usado en dermatitis perioral.'),
 ('niacinamide', 'DERMATITIS_PERIORAL', 'Antiinflamatorio y reparador de barrera.'),
 ('zinc oxide', 'DERMATITIS_PERIORAL', 'Calmante y protector.'),
 -- Psoriasis
 ('salicylic acid', 'PSORIASIS', 'Queratolítico que ablanda y retira las placas.'),
 ('urea', 'PSORIASIS', 'Queratolítico e hidratante.'),
 ('lactic acid', 'PSORIASIS', 'Queratolítico e hidratante.'),
 ('petrolatum', 'PSORIASIS', 'Oclusivo que reduce la descamación.'),
 ('ceramide np', 'PSORIASIS', 'Repara la barrera.'),
 -- Manchas / melasma
 ('niacinamide', 'HIPERPIGMENTACION', 'Inhibe la transferencia de melanosomas; consenso para manchas.'),
 ('azelaic acid', 'HIPERPIGMENTACION', 'Inhibe la tirosinasa; consenso para manchas.'),
 ('tranexamic acid', 'HIPERPIGMENTACION', 'Antimanchas con buena evidencia en melasma.'),
 ('alpha-arbutin', 'HIPERPIGMENTACION', 'Inhibe la tirosinasa.'),
 ('arbutin', 'HIPERPIGMENTACION', 'Inhibe la tirosinasa.'),
 ('kojic acid', 'HIPERPIGMENTACION', 'Inhibe la tirosinasa.'),
 ('ascorbic acid', 'HIPERPIGMENTACION', 'Antioxidante despigmentante.'),
 ('3-o-ethyl ascorbic acid', 'HIPERPIGMENTACION', 'Derivado estable de vitamina C despigmentante.'),
 ('sodium ascorbyl phosphate', 'HIPERPIGMENTACION', 'Derivado estable de vitamina C.'),
 ('glycolic acid', 'HIPERPIGMENTACION', 'Exfoliante; consenso para manchas.'),
 ('retinol', 'HIPERPIGMENTACION', 'Acelera la renovación celular; consenso para manchas.'),
 ('isobutylamido thiazolyl resorcinol', 'HIPERPIGMENTACION', 'Tiamidol: inhibidor potente de la tirosinasa humana.'),
 ('butylresorcinol', 'HIPERPIGMENTACION', 'Inhibe la tirosinasa.'),
 ('glycyrrhiza glabra root extract', 'HIPERPIGMENTACION', 'Despigmentante y calmante.'),
 ('zinc oxide', 'HIPERPIGMENTACION', 'La fotoprotección diaria es imprescindible para tratar manchas.'),
 ('titanium dioxide', 'HIPERPIGMENTACION', 'La fotoprotección diaria es imprescindible para tratar manchas.'),
 ('bis-ethylhexyloxyphenol methoxyphenyl triazine', 'HIPERPIGMENTACION', 'Filtro de amplio espectro UVA/UVB.'),
 ('methylene bis-benzotriazolyl tetramethylbutylphenol', 'HIPERPIGMENTACION', 'Filtro de amplio espectro UVA/UVB.'),
 -- Queratosis pilar
 ('urea', 'QUERATOSIS_PILAR', 'Queratolítico que suaviza los tapones foliculares.'),
 ('lactic acid', 'QUERATOSIS_PILAR', 'Exfoliante e hidratante.'),
 ('salicylic acid', 'QUERATOSIS_PILAR', 'Exfoliante que desobstruye el folículo.'),
 ('glycolic acid', 'QUERATOSIS_PILAR', 'Exfoliante.'),
 -- Fotoenvejecimiento
 ('retinol', 'FOTOENVEJECIMIENTO', 'Estimula colágeno; consenso para líneas finas y arrugas.'),
 ('retinal', 'FOTOENVEJECIMIENTO', 'Retinoide más potente que el retinol.'),
 ('hydroxypinacolone retinoate', 'FOTOENVEJECIMIENTO', 'Retinoide de mejor tolerancia.'),
 ('retinyl palmitate', 'FOTOENVEJECIMIENTO', 'Retinoide suave.'),
 ('bakuchiol', 'FOTOENVEJECIMIENTO', 'Alternativa al retinol con mejor tolerancia.'),
 ('ascorbic acid', 'FOTOENVEJECIMIENTO', 'Antioxidante que estimula colágeno.'),
 ('tetrahexyldecyl ascorbate', 'FOTOENVEJECIMIENTO', 'Vitamina C liposoluble estable.'),
 ('ferulic acid', 'FOTOENVEJECIMIENTO', 'Antioxidante que potencia a las vitaminas C y E.'),
 ('tocopherol', 'FOTOENVEJECIMIENTO', 'Antioxidante.'),
 ('niacinamide', 'FOTOENVEJECIMIENTO', 'Mejora textura y tono.'),
 ('glycolic acid', 'FOTOENVEJECIMIENTO', 'Exfoliante; consenso para líneas finas.'),
 ('palmitoyl pentapeptide-4', 'FOTOENVEJECIMIENTO', 'Péptido señal que estimula colágeno.'),
 ('acetyl hexapeptide-8', 'FOTOENVEJECIMIENTO', 'Péptido que suaviza líneas de expresión.'),
 ('copper tripeptide-1', 'FOTOENVEJECIMIENTO', 'Péptido reparador.'),
 ('adenosine', 'FOTOENVEJECIMIENTO', 'Antiarrugas.'),
 ('zinc oxide', 'FOTOENVEJECIMIENTO', 'La fotoprotección previene el fotoenvejecimiento; consenso.'),
 ('bis-ethylhexyloxyphenol methoxyphenyl triazine', 'FOTOENVEJECIMIENTO', 'Filtro de amplio espectro.'),
 ('methylene bis-benzotriazolyl tetramethylbutylphenol', 'FOTOENVEJECIMIENTO', 'Filtro de amplio espectro.'),
 -- Tipos de piel
 ('glycerin', 'SECA', 'Humectante.'),
 ('sodium hyaluronate', 'SECA', 'Humectante.'),
 ('hyaluronic acid', 'SECA', 'Humectante.'),
 ('ceramide np', 'SECA', 'Repara la barrera.'),
 ('cholesterol', 'SECA', 'Lípido de barrera.'),
 ('squalane', 'SECA', 'Emoliente.'),
 ('butyrospermum parkii butter', 'SECA', 'Emoliente.'),
 ('urea', 'SECA', 'Humectante.'),
 ('petrolatum', 'SECA', 'Oclusivo.'),
 ('panthenol', 'SECA', 'Humectante.'),
 ('niacinamide', 'GRASA', 'Seborregulador; consenso para poros y piel grasa.'),
 ('salicylic acid', 'GRASA', 'Desobstruye poros.'),
 ('zinc pca', 'GRASA', 'Seborregulador.'),
 ('kaolin', 'GRASA', 'Absorbe el exceso de grasa.'),
 ('retinol', 'GRASA', 'Consenso para piel grasa y poros.'),
 ('niacinamide', 'MIXTA', 'Equilibra zonas grasas y secas.'),
 ('sodium hyaluronate', 'MIXTA', 'Hidratación ligera.'),
 ('centella asiatica extract', 'SENSIBLE', 'Calmante y reparadora.'),
 ('madecassoside', 'SENSIBLE', 'Calmante.'),
 ('panthenol', 'SENSIBLE', 'Calmante.'),
 ('allantoin', 'SENSIBLE', 'Calmante.'),
 ('bisabolol', 'SENSIBLE', 'Calmante.'),
 ('avena sativa kernel flour', 'SENSIBLE', 'Calmante.'),
 ('ceramide np', 'SENSIBLE', 'Refuerza la barrera.'),
 ('ectoin', 'SENSIBLE', 'Calmante y protectora.'),
 ('beta-glucan', 'SENSIBLE', 'Calmante.'),
 -- Cuero cabelludo y cabello
 ('panthenol', 'CUERO_CABELLUDO_SENSIBLE', 'Calmante.'),
 ('bisabolol', 'CUERO_CABELLUDO_SENSIBLE', 'Calmante.'),
 ('allantoin', 'CUERO_CABELLUDO_SENSIBLE', 'Calmante.'),
 ('aloe barbadensis leaf juice', 'CUERO_CABELLUDO_SENSIBLE', 'Calmante.'),
 ('salicylic acid', 'CUERO_CABELLUDO_GRASO', 'Limpia el exceso de sebo.'),
 ('zinc pca', 'CUERO_CABELLUDO_GRASO', 'Seborregulador.'),
 ('minoxidil', 'CAIDA_DEL_CABELLO', 'Tratamiento tópico con mayor evidencia para la alopecia androgenética.'),
 ('rosmarinus officinalis leaf oil', 'CAIDA_DEL_CABELLO', 'Un ensayo lo comparó con minoxidil 2 % a 6 meses con resultados similares.'),
 ('caffeine', 'CAIDA_DEL_CABELLO', 'Estimula el folículo in vitro; evidencia clínica limitada.'),
 ('ketoconazole', 'CAIDA_DEL_CABELLO', 'Coadyuvante en alopecia androgenética.'),
 ('serenoa serrulata fruit extract', 'CAIDA_DEL_CABELLO', 'Evidencia limitada.'),
 ('biotin', 'CAIDA_DEL_CABELLO', 'Solo útil si hay déficit de biotina.'),
 ('sodium cocoyl isethionate', 'CABELLO_TENIDO', 'Limpieza suave sin sulfatos que preserva el color.'),
 ('sodium lauroyl methyl isethionate', 'CABELLO_TENIDO', 'Limpieza suave sin sulfatos que preserva el color.'),
 ('decyl glucoside', 'CABELLO_TENIDO', 'Tensioactivo suave que preserva el color.'),
 ('amodimethicone', 'CABELLO_TENIDO', 'Protege la cutícula y el color.'),
 ('hydrolyzed keratin', 'CABELLO_TENIDO', 'Repara la fibra tras la coloración.'),
 ('hydrolyzed keratin', 'CABELLO_DANADO', 'Rellena zonas dañadas de la fibra.'),
 ('hydrolyzed wheat protein', 'CABELLO_DANADO', 'Fortalece la fibra.'),
 ('amodimethicone', 'CABELLO_DANADO', 'Se deposita en zonas dañadas y reduce la rotura.'),
 ('dimethicone', 'CABELLO_DANADO', 'Reduce la fricción y la rotura.'),
 ('argania spinosa kernel oil', 'CABELLO_DANADO', 'Nutre y aporta brillo.'),
 ('behentrimonium chloride', 'CABELLO_DANADO', 'Desenreda y reduce la rotura.'),
 ('panthenol', 'CABELLO_DANADO', 'Humecta la fibra.'),
 ('cocos nucifera oil', 'CABELLO_DANADO', 'Penetra la fibra y reduce la pérdida de proteína al lavar.'),
 -- Embarazo y lactancia: alternativas consideradas seguras
 ('azelaic acid', 'EMBARAZO_LACTANCIA', 'Alternativa antiacné y antimanchas considerada segura en embarazo.'),
 ('niacinamide', 'EMBARAZO_LACTANCIA', 'Considerada segura en embarazo.'),
 ('zinc oxide', 'EMBARAZO_LACTANCIA', 'Filtro mineral sin absorción sistémica relevante.'),
 ('titanium dioxide', 'EMBARAZO_LACTANCIA', 'Filtro mineral sin absorción sistémica relevante.'),
 ('sodium hyaluronate', 'EMBARAZO_LACTANCIA', 'Hidratante considerado seguro en embarazo.'),
 ('glycolic acid', 'EMBARAZO_LACTANCIA', 'En baja concentración se considera aceptable en embarazo.');

-- ------------------------------------------------------------------ 3) Incompatibilidades entre ingredientes (HU8)
-- regla_incompatibilidad (V1) referencia ingrediente(id); se resuelve por nombre y se verifica que
-- ningún par se pierda por un nombre que no exista en el catálogo.
CREATE TEMP TABLE semilla_incompatibilidad (a VARCHAR(200), b VARCHAR(200), severidad VARCHAR(20), motivo TEXT) ON COMMIT DROP;
INSERT INTO semilla_incompatibilidad (a, b, severidad, motivo) VALUES
 ('retinol', 'benzoyl peroxide', 'MEDIA', 'El peróxido de benzoilo puede oxidar y degradar los retinoides; aplicarlos en momentos distintos salvo fórmulas diseñadas para combinarse.'),
 ('retinal', 'benzoyl peroxide', 'MEDIA', 'El peróxido de benzoilo puede oxidar y degradar los retinoides; aplicarlos en momentos distintos.'),
 ('retinol', 'glycolic acid', 'MEDIA', 'Retinoide + AHA aumenta la irritación y la descamación.'),
 ('retinol', 'lactic acid', 'MEDIA', 'Retinoide + AHA aumenta la irritación y la descamación.'),
 ('retinol', 'salicylic acid', 'MEDIA', 'Retinoide + BHA aumenta la irritación y la sequedad.'),
 ('retinal', 'glycolic acid', 'MEDIA', 'Retinoide + AHA aumenta la irritación y la descamación.'),
 ('retinal', 'salicylic acid', 'MEDIA', 'Retinoide + BHA aumenta la irritación y la sequedad.'),
 ('benzoyl peroxide', 'glycolic acid', 'MEDIA', 'Doble efecto irritante y secante.'),
 ('benzoyl peroxide', 'hydroquinone', 'MEDIA', 'Usados juntos pueden manchar temporalmente la piel.'),
 ('benzoyl peroxide', 'ascorbic acid', 'BAJA', 'El peróxido de benzoilo es oxidante y puede inactivar la vitamina C.'),
 ('glycolic acid', 'salicylic acid', 'BAJA', 'Exfoliación excesiva si se usan juntos a diario.'),
 ('copper tripeptide-1', 'ascorbic acid', 'BAJA', 'La vitamina C puede desestabilizar los péptidos de cobre.'),
 ('copper tripeptide-1', 'glycolic acid', 'BAJA', 'El pH ácido degrada los péptidos de cobre.'),
 ('ammonium thioglycolate', 'p-phenylenediamine', 'MEDIA', 'Alisado o permanente y tinte el mismo día dañan la fibra; se recomienda separarlos unas dos semanas.');

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM semilla_incompatibilidad s
               WHERE NOT EXISTS (SELECT 1 FROM ingrediente i WHERE i.nombre_inci = s.a)
                  OR NOT EXISTS (SELECT 1 FROM ingrediente i WHERE i.nombre_inci = s.b)) THEN
        RAISE EXCEPTION 'Incompatibilidad con un ingrediente que no está en el catálogo';
    END IF;
END $$;

INSERT INTO regla_incompatibilidad (ingrediente_a, ingrediente_b, severidad, motivo)
SELECT ia.id, ib.id, s.severidad, s.motivo
FROM semilla_incompatibilidad s
JOIN ingrediente ia ON ia.nombre_inci = s.a
JOIN ingrediente ib ON ib.nombre_inci = s.b
ON CONFLICT (ingrediente_a, ingrediente_b) DO NOTHING;
