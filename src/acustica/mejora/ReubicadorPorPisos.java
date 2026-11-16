package acustica.mejora;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import acustica.dominio.Actividad;
import acustica.dominio.Edificio;
import acustica.dominio.Habitacion;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.ResultadoHabitabilidad;
import acustica.simulacion.Simulador;

/**
 * Solución "cambiar de piso": para una habitación NO_HABITABLE, prueba
 * intercambiar su actividad con la de una habitación de OTRO piso y vuelve a
 * correr el Simulador para ver qué pasa de verdad.
 *
 * A diferencia de las soluciones de GeneradorSoluciones (estimaciones por
 * fórmula), estas quedan VERIFICADAS: solo se agrega la propuesta si, tras el
 * intercambio, la habitación ya no es NO_HABITABLE y el edificio en total
 * tiene menos habitaciones NO_HABITABLE que antes (o sea, no se movió el
 * problema a otro lado).
 *
 * La reducción de cada propuesta es la REAL: cuántos dB bajó el exceso de la
 * habitación entre la simulación original y la del intercambio.
 *
 * El edificio queda como estaba: cada intercambio de prueba se deshace y al
 * final se corre el simulador otra vez para restaurar niveles y estados.
 */
public final class ReubicadorPorPisos {

    /** Costo de ejemplo de reorganizar el uso de dos espacios (ajustar). */
    private static final double COSTO_REUBICAR = 80.0;

    private ReubicadorPorPisos() {
        // Utilidad estática: no instanciable.
    }

    /**
     * Agrega al plan las propuestas de cambio de piso que funcionan.
     *
     * @return cuántas propuestas se agregaron
     */
    public static int agregarPropuestas(PlanMejora plan, ResultadoHabitabilidad problema,
            Edificio edificio, Simulador simulador, String horario, int horizonteAnios) {
        Habitacion origen = problema.getHabitacion();
        Actividad actOrigen = origen.getActividadPrevista();
        if (problema.getEstado() != EstadoHabitabilidad.NO_HABITABLE || actOrigen == null) {
            return 0;
        }

        Set<String> antes = idsNoHabitables(simulador.ejecutar(horario));
        int agregadas = 0;

        for (Habitacion destino : edificio.getHabitaciones()) {
            Actividad actDestino = destino.getActividadPrevista();
            if (destino == origen || destino.getPiso() == origen.getPiso() || actDestino == null) {
                continue;
            }
            origen.setActividadPrevista(actDestino);
            destino.setActividadPrevista(actOrigen);
            try {
                List<ResultadoHabitabilidad> resultadosDespues = simulador.ejecutar(horario);
                Set<String> despues = idsNoHabitables(resultadosDespues);
                if (!despues.contains(origen.getId()) && despues.size() < antes.size()) {
                    double reduccionReal = medirReduccionReal(problema, resultadosDespues);
                    if (reduccionReal <= 0.0) {
                        continue; // sin reducción medible: no se propone
                    }
                    boolean nueva = plan.agregar(new Solucion(
                            origen.getId() + "-PISO-" + destino.getId(),
                            Solucion.Tipo.CAMBIAR_DE_PISO,
                            origen.getId(),
                            String.format("Intercambiar %s de %s (piso %d) con %s de %s (piso %d)",
                                    actOrigen, origen.getNombre(), origen.getPiso(),
                                    actDestino, destino.getNombre(), destino.getPiso()),
                            reduccionReal,
                            COSTO_REUBICAR,
                            Double.POSITIVE_INFINITY, // no se desgasta: no requiere mantenimiento
                            0.0,
                            horizonteAnios,
                            true));
                    if (nueva) {
                        agregadas++;
                    }
                }
            } finally {
                origen.setActividadPrevista(actOrigen);
                destino.setActividadPrevista(actDestino);
            }
        }

        simulador.ejecutar(horario); // deja niveles y estados como estaban
        return agregadas;
    }

    /**
     * Cuántos dB bajó el exceso de la habitación al re-simular con el
     * intercambio. Devuelve 0 si no se puede medir (exceso infinito) o si no
     * bajó.
     */
    private static double medirReduccionReal(ResultadoHabitabilidad original,
            List<ResultadoHabitabilidad> simulados) {
        for (ResultadoHabitabilidad r : simulados) {
            if (r.getHabitacion().getId().equals(original.getHabitacion().getId())) {
                double excesoAntes = original.getExceso();
                double excesoDespues = r.getExceso();
                if (!Double.isFinite(excesoAntes) || !Double.isFinite(excesoDespues)) {
                    return 0.0;
                }
                double reduccion = excesoAntes - excesoDespues;
                return reduccion > 0.000001 ? reduccion : 0.0;
            }
        }
        return 0.0;
    }

    private static Set<String> idsNoHabitables(List<ResultadoHabitabilidad> resultados) {
        Set<String> ids = new HashSet<>();
        for (ResultadoHabitabilidad r : resultados) {
            if (r.getEstado() == EstadoHabitabilidad.NO_HABITABLE) {
                ids.add(r.getHabitacion().getId());
            }
        }
        return ids;
    }
}