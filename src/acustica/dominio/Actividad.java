package acustica.dominio;

/**
 * Actividad prevista en una habitación, con sus umbrales de ruido.
 *
 * {@code limiteOptimo} y {@code limiteMax} permiten clasificar el nivel como
 * HABITABLE, TOLERABLE o NO_HABITABLE. {@code emiteDba} es un nivel típico de
 * la actividad, útil como dato descriptivo; el simulador usa el nivel real de
 * cada {@link FuenteSonido}, no este valor.
 */
public enum Actividad {
    DORMIR(0, 30, 40),
    ESTUDIAR(0, 35, 45),
    LLAMADAS(55, 45, 55),
    CONVERSAR(65, 55, 65),
    ENTRETENIMIENTO(75, 65, 75),
    INSTRUMENTO(85, 75, 85),
    EJERCICIO(90, 80, 90);

    /** Nivel típico emitido por la actividad, en dBA (dato descriptivo). */
    public final double emiteDba;

    /** Nivel máximo para considerar la habitación habitable, en dBA. */
    public final double limiteOptimo;

    /** Nivel máximo tolerado antes de declararla no habitable, en dBA. */
    public final double limiteMax;

    Actividad(double emiteDba, double limiteOptimo, double limiteMax) {
        this.emiteDba = emiteDba;
        this.limiteOptimo = limiteOptimo;
        this.limiteMax = limiteMax;
    }
}
