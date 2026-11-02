package acustica.normativa;

/**
 * Resultado de evaluar una habitación contra un límite de ruido.
 *
 * - HABITABLE: el nivel no supera el límite óptimo.
 * - TOLERABLE: supera el óptimo pero no el máximo (solo con {@code Actividad}).
 * - NO_HABITABLE: supera el límite aplicable.
 * - NO_EVALUABLE: no hay límite definido para ese tipo de recinto y horario.
 */
public enum EstadoHabitabilidad {
    HABITABLE,
    TOLERABLE,
    NO_HABITABLE,
    NO_EVALUABLE
}
