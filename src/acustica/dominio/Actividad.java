package acustica.dominio;

/**
 * Actividad prevista en una habitación (árbol de decisión de habitabilidad).
 *
 * Cada actividad pertenece a una {@link Categoria}, que decide cómo se evalúa:
 *
 * - EXIGE_SILENCIO (Nodo 1 = SI): se mide el ruido que LLEGA de otros
 *   espacios. Usa {@code limiteOptimo} y {@code limiteMax}.
 * - MODERADA (Nodo 3 = NO): actividad de nivel moderado; es habitable.
 * - ALTO_IMPACTO (Nodo 3 = SI): se mide si el aislamiento hacia los vecinos
 *   alcanza (Nodo 4): aislamiento requerido = {@code emiteDba} - límite del
 *   vecino.
 *
 * {@code emiteDba} es el nivel típico que genera la actividad; solo lo usan
 * las actividades de alto impacto. Los límites solo aplican a las que exigen
 * silencio (en las demás valen {@link Double#NaN}).
 */
public enum Actividad {
    DORMIR(Categoria.EXIGE_SILENCIO, 0, 30, 40),
    ESTUDIAR(Categoria.EXIGE_SILENCIO, 0, 35, 40),
    LEER(Categoria.EXIGE_SILENCIO, 0, 35, 40),
    LLAMADAS(Categoria.MODERADA, 55),
    CONVERSAR(Categoria.MODERADA, 65),
    ENTRETENIMIENTO(Categoria.ALTO_IMPACTO, 75),
    INSTRUMENTO(Categoria.ALTO_IMPACTO, 85),
    EJERCICIO(Categoria.ALTO_IMPACTO, 90),
    JUGAR_NINOS(Categoria.ALTO_IMPACTO, 75),
    REUNION_SOCIAL(Categoria.ALTO_IMPACTO, 80),
    FIESTA(Categoria.ALTO_IMPACTO, 90),
    OBRAS(Categoria.ALTO_IMPACTO, 95),
    VEHICULOS(Categoria.ALTO_IMPACTO, 75),
    MAQUINARIA(Categoria.ALTO_IMPACTO, 85);

    /** Cómo se evalúa la actividad en el árbol de decisión. */
    public enum Categoria {
        EXIGE_SILENCIO,
        MODERADA,
        ALTO_IMPACTO
    }

    public final Categoria categoria;

    /** Nivel típico emitido por la actividad, en dBA. */
    public final double emiteDba;

    /** Ruido máximo recibido para considerarla habitable (solo EXIGE_SILENCIO), en dBA. */
    public final double limiteOptimo;

    /** Ruido máximo tolerado antes de declararla no habitable (solo EXIGE_SILENCIO), en dBA. */
    public final double limiteMax;

    Actividad(Categoria categoria, double emiteDba, double limiteOptimo, double limiteMax) {
        this.categoria = categoria;
        this.emiteDba = emiteDba;
        this.limiteOptimo = limiteOptimo;
        this.limiteMax = limiteMax;
    }

    Actividad(Categoria categoria, double emiteDba) {
        this(categoria, emiteDba, Double.NaN, Double.NaN);
    }

    public boolean exigeSilencio() {
        return categoria == Categoria.EXIGE_SILENCIO;
    }

    public boolean esAltoImpacto() {
        return categoria == Categoria.ALTO_IMPACTO;
    }
}
