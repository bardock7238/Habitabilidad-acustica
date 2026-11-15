package acustica.normativa;

import acustica.dominio.Habitacion;

/**
 * Resultado de evaluar una habitación: los niveles de ruido involucrados, el
 * límite normativo contra el que se comparó y el estado resultante.
 * El {@code Simulador} crea uno de estos por cada {@link Habitacion}.
 *
 * Hay dos niveles y NO significan lo mismo:
 *
 * - {@code nivelRecibido}: dB que llegan a ESTA habitación. Es siempre su
 *   propio nivel, el que el Simulador le calculó.
 * - {@code nivelFinal}: nivel contra el que se calculó el exceso. Cuando el
 *   límite es del propio recinto (Normativa, EXIGE_SILENCIO, MODERADA)
 *   coincide con {@code nivelRecibido}; en una actividad ALTO_IMPACTO es el
 *   nivel que llega al vecino más afectado, porque ahí se evalúa el
 *   aislamiento hacia los vecinos. Por eso {@link #usaNivelDeVecino()} dice
 *   cuál de los dos casos aplica sin comparar doubles.
 *
 * Es un objeto de datos: no calcula la propagación, solo almacena el resultado.
 */
public class ResultadoHabitabilidad {

    private final Habitacion habitacion;
    private final double nivelRecibido;   // dB(A) que llegan a esta habitación
    private final double nivelFinal;      // dB contra el que se midió el exceso
    private final double limiteAplicado;  // dB contra el que se comparó
    private final EstadoHabitabilidad estado;
    private final boolean nivelDeVecino;

    private ResultadoHabitabilidad(Habitacion habitacion, double nivelRecibido, double nivelFinal,
            double limiteAplicado, EstadoHabitabilidad estado, boolean nivelDeVecino) {
        this.habitacion = habitacion;
        this.nivelRecibido = nivelRecibido;
        this.nivelFinal = nivelFinal;
        this.limiteAplicado = limiteAplicado;
        this.estado = estado;
        this.nivelDeVecino = nivelDeVecino;
    }

    /** Resultado cuyo límite es del propio recinto: nivelFinal = nivelRecibido. */
    public static ResultadoHabitabilidad propio(Habitacion habitacion, double nivelRecibido,
            double limiteAplicado, EstadoHabitabilidad estado) {
        return new ResultadoHabitabilidad(habitacion, nivelRecibido, nivelRecibido,
                limiteAplicado, estado, false);
    }

    /**
     * Resultado de una actividad de alto impacto: el exceso se mide en el
     * vecino más afectado, que puede estar al mismo nivel que el recibido.
     *
     * @param nivelRecibido dB que llegan a esta habitación
     * @param nivelVecino   dB que llegan al vecino más afectado
     * @param limiteVecino  límite que exige ese vecino
     */
    public static ResultadoHabitabilidad conVecino(Habitacion habitacion, double nivelRecibido,
            double nivelVecino, double limiteVecino, EstadoHabitabilidad estado) {
        return new ResultadoHabitabilidad(habitacion, nivelRecibido, nivelVecino,
                limiteVecino, estado, true);
    }

    public Habitacion getHabitacion() {
        return habitacion;
    }

    public double getNivelRecibido() {
        return nivelRecibido;
    }

    public double getNivelFinal() {
        return nivelFinal;
    }

    public double getLimiteAplicado() {
        return limiteAplicado;
    }

    /** True si nivelFinal es el nivel de un vecino (actividad de alto impacto). */
    public boolean usaNivelDeVecino() {
        return nivelDeVecino;
    }

    public boolean isHabitable() {
        return estado == EstadoHabitabilidad.HABITABLE || estado == EstadoHabitabilidad.TOLERABLE;
    }

    public EstadoHabitabilidad getEstado() {
        return estado;
    }

    /**
     * Cuántos dB se pasa (positivo) o le sobran (negativo) respecto al límite.
     * Para resultados NO_EVALUABLE devuelve {@link Double#NEGATIVE_INFINITY},
     * de modo que no compitan por el "peor caso" del edificio.
     */
    public double getExceso() {
        return estado == EstadoHabitabilidad.NO_EVALUABLE
                ? Double.NEGATIVE_INFINITY
                : nivelFinal - limiteAplicado;
    }

    @Override
    public String toString() {
        String limiteTexto = Double.isInfinite(limiteAplicado)
                ? "sin limite"
                : String.format("%.1f dB", limiteAplicado);
        return String.format("%s (%s): nivel=%.1f dB, limite=%s -> %s",
                habitacion.getNombre(),
                habitacion.getTipo(),
                nivelFinal,
                limiteTexto,
                estado);
    }
}
