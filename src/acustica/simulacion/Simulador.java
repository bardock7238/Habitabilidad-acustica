package acustica.simulacion;

import java.util.ArrayList;
import java.util.List;


import acustica.dominio.Edificio;
import acustica.dominio.FuenteSonido;
import acustica.dominio.Habitacion;
import acustica.normativa.Normativa;
import acustica.normativa.ResultadoHabitabilidad;
import acustica.simulacion.propagacion.CalculoAcustico;
import acustica.simulacion.propagacion.Grafo;
import acustica.dominio.Actividad;
import acustica.normativa.EstadoHabitabilidad;


public class Simulador {

    private final Edificio edificio;
    private final Normativa normativa;
    private List<ResultadoHabitabilidad> resultados;

    public Simulador(Edificio edificio, Normativa normativa) {
        if (edificio == null || normativa == null) {
            throw new IllegalArgumentException("El edificio y la normativa no pueden ser null");
        }
        this.edificio = edificio;
        this.normativa = normativa;
        this.resultados = new ArrayList<>();
    }

    /**
     * Corre la simulación completa para el horario dado ("dia" o "noche").
     *
     * @return la lista de resultados, uno por habitación
     */
    public List<ResultadoHabitabilidad> ejecutar(String horario) {
        if (horario == null) {
            throw new IllegalArgumentException("El horario no puede ser null");
        }
        // Lista local: cada ejecución devuelve una lista independiente. Si se
        // reutilizara el mismo objeto, los resultados de "dia" cambiarían al
        // ejecutar "noche".
        List<ResultadoHabitabilidad> resultados = new ArrayList<>();
        Grafo grafo = Grafo.desdeEdificio(edificio);

        for (Habitacion destino : edificio.getHabitaciones()) {
            double nivelTotal = calcularNivelTotal(destino, horario, grafo);
            destino.setNivelRuido(nivelTotal);

            Actividad actividad = destino.getActividadPrevista();
            if (actividad != null) {
                EstadoHabitabilidad estado = nivelTotal <= actividad.limiteOptimo
                        ? EstadoHabitabilidad.HABITABLE
                        : nivelTotal <= actividad.limiteMax
                                ? EstadoHabitabilidad.TOLERABLE
                                : EstadoHabitabilidad.NO_HABITABLE;
                destino.setHabitable(esHabitable(estado));
                resultados.add(new ResultadoHabitabilidad(destino, nivelTotal, actividad.limiteOptimo, estado));
                continue;
            }

            try {
                double limite = normativa.obtenerLimite(destino.getTipo(), horario);
                EstadoHabitabilidad estado = nivelTotal <= limite
                        ? EstadoHabitabilidad.HABITABLE
                        : EstadoHabitabilidad.NO_HABITABLE;
                destino.setHabitable(esHabitable(estado));
                resultados.add(new ResultadoHabitabilidad(destino, nivelTotal, limite, estado));
            } catch (IllegalArgumentException sinLimite) {
                destino.setHabitable(false);
                resultados.add(new ResultadoHabitabilidad(destino, nivelTotal, Double.NaN,
                        EstadoHabitabilidad.NO_EVALUABLE));
            }
        }

        this.resultados = resultados;
        return resultados;
    }

    private static boolean esHabitable(EstadoHabitabilidad estado) {
        return estado == EstadoHabitabilidad.HABITABLE || estado == EstadoHabitabilidad.TOLERABLE;
    }

    /**
     * Suma el aporte de todas las fuentes activas del edificio (de cualquier
     * habitación) que llegan a la habitación destino, usando el camino de
     * menor atenuación del grafo (Dijkstra).
     */
    private double calcularNivelTotal(Habitacion destino, String horario, Grafo grafo) {
        List<Double> aportes = new ArrayList<>();
        aportes.add(destino.getNivelFondo());

        for (Habitacion origen : edificio.getHabitaciones()) {
            for (FuenteSonido fuente : origen.getFuentes()) {
                if (!fuenteActivaEnHorario(fuente, horario)) {
                    continue;
                }
                double nivel = CalculoAcustico.calcularNivelEnDestino(
                        fuente, origen.getId(), destino.getId(), grafo);
                aportes.add(nivel);
            }
        }

        return CalculoAcustico.sumarNiveles(aportes.stream().mapToDouble(Double::doubleValue).toArray());
    }

    private boolean fuenteActivaEnHorario(FuenteSonido fuente, String horario) {
        return fuente.getHorario() != null
                && fuente.getHorario().name().equalsIgnoreCase(horario);
    }

    public List<ResultadoHabitabilidad> getResultados() {
        return List.copyOf(resultados);
    }

    public Edificio getEdificio() {
        return edificio;
    }

    public Normativa getNormativa() {
        return normativa;
    }
}
