package acustica.normativa;
import acustica.dominio.Habitacion;

/**
 * Guarda el resultado de evaluar una habitación: el nivel final de ruido
 * que llegó a ella, el límite normativo contra el que se comparó y si es
 * habitable o no. El Simulador crea uno de estos por cada Habitacion.
 * Es un objeto de datos inmutable: no calcula nada, solo almacena.
 */
public class ResultadoHabitabilidad {

    private final Habitacion habitacion;
    private final double nivelFinal;      // dB(A) que llegan a la habitación
    private final double limiteAplicado;  // dB contra el que se comparó
    private final boolean habitable;

    public ResultadoHabitabilidad(Habitacion habitacion, double nivelFinal, double limiteAplicado, boolean habitable) {
        this.habitacion = habitacion;
        this.nivelFinal = nivelFinal;
        this.limiteAplicado = limiteAplicado;
        this.habitable = habitable;
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
        return habitable;
    }

    /**
     * Cuántos dB se pasa (positivo) o le sobran (negativo) respecto al límite.
     * Útil para que el Simulador encuentre el "peor caso" del edificio.
     */
    public double getExceso() {
        return nivelFinal - limiteAplicado;
    }

    @Override
    public String toString() {
        return String.format("%s (%s): nivel=%.1f dB, limite=%.1f dB -> %s",
                habitacion.getNombre(),
                habitacion.getTipo(),
                nivelFinal,
                limiteAplicado,
                habitable ? "HABITABLE" : "NO HABITABLE");
    }
}