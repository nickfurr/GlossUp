gitpackage com.glossup.perfil.domain.model;

/** Tipo de cabello según la clasificación de Andre Walker (1A–4C). */
public enum TipoCabello {
    LISO_1A("Liso 1A · fino y completamente liso"),
    LISO_1B("Liso 1B · liso con más cuerpo"),
    LISO_1C("Liso 1C · liso, grueso o resistente"),
    ONDULADO_2A("Ondulado 2A · ondas suaves"),
    ONDULADO_2B("Ondulado 2B · ondas marcadas"),
    ONDULADO_2C("Ondulado 2C · ondas fuertes"),
    RIZADO_3A("Rizado 3A · rizos amplios"),
    RIZADO_3B("Rizado 3B · rizos definidos"),
    RIZADO_3C("Rizado 3C · rizos cerrados"),
    MUY_RIZADO_4A("Muy rizado 4A · espirales pequeñas"),
    MUY_RIZADO_4B("Muy rizado 4B · patrón angular"),
    MUY_RIZADO_4C("Muy rizado 4C · patrón muy cerrado");

    private final String etiqueta;

    TipoCabello(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() { return etiqueta; }
}
