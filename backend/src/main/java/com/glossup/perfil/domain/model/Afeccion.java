package com.glossup.perfil.domain.model;

/**
 * Afecciones de piel, cuero cabelludo y cabello más frecuentes (GBD 2021; series clínicas de
 * trastornos de cabello y cuero cabelludo), más condiciones que contraindican ingredientes.
 * El código también es la "condición" con la que el motor de compatibilidad cruza sus reglas.
 */
public enum Afeccion {
    ACNE("Acné", Zona.PIEL),
    ROSACEA("Rosácea", Zona.PIEL),
    DERMATITIS_ATOPICA("Dermatitis atópica / eccema", Zona.PIEL),
    DERMATITIS_SEBORREICA("Dermatitis seborreica (cara o cuero cabelludo)", Zona.PIEL),
    DERMATITIS_CONTACTO("Dermatitis de contacto (antecedente)", Zona.PIEL),
    DERMATITIS_PERIORAL("Dermatitis perioral", Zona.PIEL),
    PSORIASIS("Psoriasis", Zona.PIEL),
    HIPERPIGMENTACION("Manchas / melasma", Zona.PIEL),
    QUERATOSIS_PILAR("Queratosis pilar", Zona.PIEL),
    FOTOENVEJECIMIENTO("Arrugas y fotoenvejecimiento", Zona.PIEL),
    CASPA("Caspa", Zona.CABELLO),
    CUERO_CABELLUDO_SENSIBLE("Cuero cabelludo sensible o con picor", Zona.CABELLO),
    CUERO_CABELLUDO_GRASO("Cuero cabelludo graso", Zona.CABELLO),
    CAIDA_DEL_CABELLO("Caída del cabello", Zona.CABELLO),
    CABELLO_TENIDO("Cabello teñido o decolorado", Zona.CABELLO),
    CABELLO_DANADO("Cabello dañado o quebradizo", Zona.CABELLO),
    EMBARAZO_LACTANCIA("Embarazo o lactancia", Zona.GENERAL);

    public enum Zona { PIEL, CABELLO, GENERAL }

    private final String etiqueta;
    private final Zona zona;

    Afeccion(String etiqueta, Zona zona) {
        this.etiqueta = etiqueta;
        this.zona = zona;
    }

    public String getEtiqueta() { return etiqueta; }
    public Zona getZona() { return zona; }
}
