package com.glossup.perfil.domain.model;

/**
 * Alérgenos de contacto cosméticos más frecuentes, agrupados como en las series de parche
 * (NACDG 2019–2020, ESSCA 2021/2022, serie basal latinoamericana en Colombia 2016–2021).
 *
 * <p>Solo incluye alérgenos verificables contra la ficha INCI: metales como níquel o paladio
 * son frecuentes pero no se declaran en el INCI, y el motor no podría detectarlos. Cada código
 * se expande a sus nombres INCI en la tabla {@code alergeno_ingrediente}.</p>
 */
public enum Alergeno {
    FRAGANCIA("Fragancia / perfume", "Parfum, fragrance o aroma y los 26 alérgenos de fragancia regulados en la UE."),
    BALSAMO_DEL_PERU("Bálsamo del Perú", "Myroxylon pereirae: resina aromática en perfumes, cremas y labiales."),
    LIMONENO("Limoneno", "Componente cítrico de fragancias; al oxidarse forma hidroperóxidos alergénicos."),
    LINALOOL("Linalool", "Componente floral de fragancias; al oxidarse forma hidroperóxidos alergénicos."),
    METILISOTIAZOLINONA("Metilisotiazolinona (MI/MCI)", "Conservante en champús, jabones líquidos y toallitas húmedas."),
    BENZISOTIAZOLINONA("Benzisotiazolinona (BIT)", "Conservante de la familia de las isotiazolinonas."),
    FORMALDEHIDO("Formaldehído y liberadores", "Conservantes como quaternium-15, DMDM hydantoin o imidazolidinyl urea, y alisados con formol."),
    METILDIBROMO_GLUTARONITRILO("Metildibromo glutaronitrilo", "Conservante; de los alérgenos más frecuentes en la serie europea."),
    PARABENOS("Parabenos", "Conservantes: methylparaben, propylparaben, butylparaben, etc."),
    YODOPROPINIL_BUTILCARBAMATO("Yodopropinil butilcarbamato", "Conservante (IPBC) en cremas, champús y maquillaje."),
    PARAFENILENDIAMINA("Parafenilendiamina (PPD)", "Tintes de cabello permanentes oscuros y henna negra."),
    PERSULFATOS("Persulfatos", "Decolorantes del cabello (de amonio, potasio o sodio)."),
    TIOGLICOLATOS("Tioglicolatos", "Permanentes y alisados químicos."),
    ACRILATOS("Acrilatos", "Uñas acrílicas, en gel y esmalte semipermanente (HEMA y otros metacrilatos)."),
    COLOFONIA("Colofonia (resina)", "Resina en máscaras de pestañas, ceras depilatorias y adhesivos."),
    LANOLINA("Lanolina", "Cera de lana en bálsamos labiales, cremas hidratantes y ungüentos."),
    PROPOLEO("Propóleo", "Resina de abeja en productos naturales, labiales y cremas."),
    COCAMIDOPROPIL_BETAINA("Cocamidopropil betaína", "Tensioactivo suave en champús, geles y limpiadores faciales."),
    GLUCOSIDOS("Alquil glucósidos", "Tensioactivos 'naturales' (decyl, lauryl, coco-glucoside) en limpiadores y protectores solares."),
    PROPILENGLICOL("Propilenglicol", "Humectante y vehículo en cremas, desodorantes y geles."),
    FILTROS_UV("Filtros solares químicos", "Benzofenonas y octocrileno en protectores solares y cosméticos con FPS."),
    ACEITE_ARBOL_TE("Aceite de árbol de té", "Melaleuca alternifolia en productos para acné y champús anticaspa."),
    PLANTAS_COMPUESTAS("Plantas compuestas", "Manzanilla, caléndula, árnica y otras de la familia Asteraceae."),
    TIMEROSAL("Timerosal", "Conservante mercurial aún presente en algunos productos para ojos.");

    private final String etiqueta;
    private final String descripcion;

    Alergeno(String etiqueta, String descripcion) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getDescripcion() { return descripcion; }
}
