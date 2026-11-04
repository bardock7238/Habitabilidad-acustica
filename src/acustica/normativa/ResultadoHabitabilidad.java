package acustica.normativa;

import acustica.dominio.Habitacion;

/**
 * Resultado de evaluar una habitación: el nivel final de ruido que llegó a
 * ella, el límite normativo contra el que se comparó y el estado resultante.
 * El {@code Simulador} crea uno de estos por cada {@link Habitacion}.
 *
 * Es un objeto de datos: no calcula la propagación, solo almacena el resultado.
 */
public class ResultadoHabitabilidad {

    private final Habitacion habitacion;
    private final double nivelFinal;      // dB(A) que llegan a la habitación
    private final double limiteAplicado;  // dB contra el que se comparó
    private final EstadoHabitabilidad estado;

    public ResultadoHabitabilidad(Habitacion habitacion, double nivelFinal,
            double limiteAplicado, EstadoHabitabilidad estado) {
        this.habitacion = habitacion;
        this.nivelFinal = nivelFinal;
        this.limiteAplicado = limiteAplicado;
        this.estado = estado;
    }

    public Habitacion getHabitacion() {
        return habitacion;
    }

    public double getNivelFinal() {
        return nivelFinal;
    }

    public double getLimiteAplicado() {
        return limiteAplicado;
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
