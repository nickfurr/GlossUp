-- Tipo de cabello en el motor de compatibilidad (auditoría NC-T09 / R-07, RNF-02). Los doce tipos de Andre Walker
-- se agrupan en tres familias, porque las recomendaciones de la literatura van por familia y no por subtipo:
--   CABELLO_LISO (1A–1C), CABELLO_ONDULADO (2A–2C), CABELLO_RIZADO (3A–4C).
-- Fuentes: Draelos ZD. Essentials of hair care often neglected: hair cleansing. Int J Trichology. 2010;2(1):24-29.
--          Gavazzoni Dias MFR. Hair cosmetics: an overview. Int J Trichology. 2015;7(1):2-15.
-- Severidad BAJA o MEDIA: son avisos de cuidado, no de salud. Pendiente de validar con el asesor experto (0.0 §3.1).

INSERT INTO condicion (codigo) VALUES ('CABELLO_LISO'), ('CABELLO_ONDULADO'), ('CABELLO_RIZADO');

-- Zona en la que aplica la regla: un sulfato en un limpiador facial no le importa al tipo de cabello.
-- NULL = cualquier zona (todas las reglas anteriores).
ALTER TABLE regla_perfil ADD COLUMN zona VARCHAR(10) CHECK (zona IN ('ROSTRO', 'CUERPO', 'CABELLO'));
ALTER TABLE beneficio    ADD COLUMN zona VARCHAR(10) CHECK (zona IN ('ROSTRO', 'CUERPO', 'CABELLO'));

INSERT INTO regla_perfil (ingrediente_inci, condicion, severidad, motivo, zona) VALUES
 ('sodium lauryl sulfate', 'CABELLO_RIZADO', 'MEDIA', 'El rizo es más seco porque el sebo baja con dificultad por la fibra curva; los sulfatos fuertes le quitan la grasa que le queda y lo encrespan (Draelos, 2010).', 'CABELLO'),
 ('ammonium lauryl sulfate', 'CABELLO_RIZADO', 'MEDIA', 'El rizo es más seco porque el sebo baja con dificultad por la fibra curva; los sulfatos fuertes le quitan la grasa que le queda y lo encrespan (Draelos, 2010).', 'CABELLO'),
 ('sodium laureth sulfate', 'CABELLO_RIZADO', 'BAJA', 'Limpia más suave que el lauril sulfato, pero en rizos secos conviene alternarlo con un limpiador sin sulfatos (Draelos, 2010).', 'CABELLO'),
 ('alcohol denat.', 'CABELLO_RIZADO', 'MEDIA', 'Los alcoholes de cadena corta se evaporan llevándose el agua de la fibra: resecan y encrespan el rizo (Gavazzoni Dias, 2015).', 'CABELLO'),
 ('isopropyl alcohol', 'CABELLO_RIZADO', 'MEDIA', 'Los alcoholes de cadena corta se evaporan llevándose el agua de la fibra: resecan y encrespan el rizo (Gavazzoni Dias, 2015).', 'CABELLO'),
 ('sodium lauryl sulfate', 'CABELLO_ONDULADO', 'BAJA', 'Las ondas pierden definición cuando el lavado las reseca; un limpiador más suave las conserva mejor (Draelos, 2010).', 'CABELLO'),
 ('ammonium lauryl sulfate', 'CABELLO_ONDULADO', 'BAJA', 'Las ondas pierden definición cuando el lavado las reseca; un limpiador más suave las conserva mejor (Draelos, 2010).', 'CABELLO'),
 ('butyrospermum parkii butter', 'CABELLO_LISO', 'BAJA', 'Las mantecas densas apelmazan el cabello liso, sobre todo si es fino: lo dejan pesado y con aspecto graso (Gavazzoni Dias, 2015).', 'CABELLO'),
 ('cocos nucifera oil', 'CABELLO_LISO', 'BAJA', 'Los aceites pesados apelmazan el cabello liso, sobre todo si es fino: úsalo solo en medios y puntas (Gavazzoni Dias, 2015).', 'CABELLO'),
 ('petrolatum', 'CABELLO_LISO', 'BAJA', 'La vaselina no se enjuaga con facilidad y deja el cabello liso pesado y opaco (Gavazzoni Dias, 2015).', 'CABELLO');

INSERT INTO beneficio (ingrediente_inci, condicion, motivo, zona) VALUES
 ('glycerin', 'CABELLO_RIZADO', 'humectante: atrae agua a la fibra y ayuda a definir el rizo sin resecarlo.', 'CABELLO'),
 ('butyrospermum parkii butter', 'CABELLO_RIZADO', 'emoliente denso que sella la hidratación del rizo seco.', 'CABELLO'),
 ('panthenol', 'CABELLO_RIZADO', 'humectante que penetra la fibra y le da elasticidad.', 'CABELLO'),
 ('behentrimonium chloride', 'CABELLO_RIZADO', 'acondicionador catiónico que desenreda y reduce el frizz.', 'CABELLO'),
 ('glycerin', 'CABELLO_ONDULADO', 'humectante liviano que define la onda sin apelmazarla.', 'CABELLO'),
 ('aloe barbadensis leaf juice', 'CABELLO_ONDULADO', 'hidrata y da definición sin peso.', 'CABELLO'),
 ('panthenol', 'CABELLO_ONDULADO', 'humectante que da elasticidad sin peso.', 'CABELLO'),
 ('panthenol', 'CABELLO_LISO', 'hidrata y da cuerpo sin apelmazar.', 'CABELLO'),
 ('hydrolyzed keratin', 'CABELLO_LISO', 'proteína que refuerza la fibra fina y le da cuerpo.', 'CABELLO')
ON CONFLICT (ingrediente_inci, condicion) DO NOTHING;
