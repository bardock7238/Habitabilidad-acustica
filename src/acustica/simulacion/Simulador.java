package acustica.simulacion;

import java.util.ArrayList;
import java.util.List;


import acustica.dominio.Edificio;
import acustica.dominio.FuenteSonido;
import acustica.dominio.Habitacion;
import acustica.normativa.Normativa;
import acustica.normativa.ResultadoHabitabilidad;
import acustica.simulacion.propagacion.CalculoAcustico;
import acustica.simulacion.propagacion.Dijkstra;
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
                resultados.add(evaluarPorActividad(destino, actividad, nivelTotal, horario, grafo));
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

    /**
     * Árbol de decisión por actividad:
     * - EXIGE_SILENCIO (Nodo 2): compara el ruido que llega con sus límites.
     * - MODERADA (Nodo 3 = NO): habitable, no se compara contra ningún límite.
     * - ALTO_IMPACTO (Nodo 4): revisa si el aislamiento hacia cada vecino
     *   cubre {@code emiteDba - límite del vecino}.
     */
    private ResultadoHabitabilidad evaluarPorActividad(Habitacion habitacion, Actividad actividad,
            double nivelTotal, String horario, Grafo grafo) {
        EstadoHabitabilidad estado;
        double nivelReportado = nivelTotal;
        double limiteReportado;

        switch (actividad.categoria) {
            case EXIGE_SILENCIO:
                limiteReportado = actividad.limiteOptimo;
                estado = nivelTotal <= actividad.limiteOptimo
                        ? EstadoHabitabilidad.HABITABLE
                        : nivelTotal <= actividad.limiteMax
                                ? EstadoHabitabilidad.TOLERABLE
                                : EstadoHabitabilidad.NO_HABITABLE;
                break;
            case MODERADA:
                limiteReportado = Double.POSITIVE_INFINITY;
                estado = EstadoHabitabilidad.HABITABLE;
                break;
            default: // ALTO_IMPACTO
                limiteReportado = Double.POSITIVE_INFINITY;
                estado = EstadoHabitabilidad.HABITABLE;
                double peorExceso = Double.NEGATIVE_INFINITY;
                Dijkstra.Resultado camino = Dijkstra.calcular(grafo, habitacion.getId());
                for (Habitacion vecina : edificio.getHabitaciones()) {
                    if (vecina == habitacion) {
                        continue;
                    }
                    double limiteVecina = limiteQueExigeLaVecina(vecina, horario);
                    if (Double.isNaN(limiteVecina)) {
                        continue;
                    }
                    double nivelEnVecina = CalculoAcustico.calcularNivelConAtenuacion(
                            actividad.emiteDba, camino.getAtenuacionHasta(vecina.getId()));
                    double exceso = nivelEnVecina - limiteVecina;
                    if (exceso > peorExceso) {
                        peorExceso = exceso;
                        nivelReportado = nivelEnVecina;
                        limiteReportado = limiteVecina;
                    }
                }
                if (peorExceso > 0) {
                    estado = EstadoHabitabilidad.NO_HABITABLE;
                }
                break;
        }

        habitacion.setHabitable(esHabitable(estado));
        return new ResultadoHabitabilidad(habitacion, nivelReportado, limiteReportado, estado);
    }

    /**
     * Límite de ruido que protege a una habitación vecina frente a una
     * actividad de alto impacto. Devuelve NaN si esa vecina no exige nada
     * (tiene una actividad moderada o ruidosa, o no hay límite definido).
     */
    private double limiteQueExigeLaVecina(Habitacion vecina, String horario) {
        Actividad actividad = vecina.getActividadPrevista();
        if (actividad != null) {
            return actividad.exigeSilencio() ? actividad.limiteMax : Double.NaN;
        }
        try {
            return normativa.obtenerLimite(vecina.getTipo(), horario);
        } catch (IllegalArgumentException sinLimite) {
            return Double.NaN;
        }
    }

    private static boolean esHabitable(EstadoHabitabilidad estado) {
        return estado == EstadoHabitabilidad.HABITABLE || estado == EstadoHabitabilidad.TOLERABLE;
    }

    /**
     * Suma el aporte de todas las fuentes activas del edificio (de cualquier
     * habitación) y de las actividades de otras habitaciones que llegan a la
     * habitación destino, usando el camino de
     * menor atenuación del grafo (Dijkstra).
     */
    private double calcularNivelTotal(Habitacion destino, String horario, Grafo grafo) {
        List<Double> aportes = new ArrayList<>();
        aportes.add(destino.getNivelFondo());

        for (Habitacion origen : edificio.getHabitaciones()) {
            // Lo que hace la gente en otra habitación (fiesta, instrumento,
            // maquinaria...) también llega al destino: emiteDba - atenuación.
            Actividad actividadOrigen = origen.getActividadPrevista();
            if (origen != destino && actividadOrigen != null && actividadOrigen.emiteDba > 0) {
                double atenuacion = Dijkstra.calcular(grafo, origen.getId())
                        .getAtenuacionHasta(destino.getId());
                aportes.add(CalculoAcustico.calcularNivelConAtenuacion(
                        actividadOrigen.emiteDba, atenuacion));
            }
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

    public double calcularPorcentajeHabitable() {
        if (resultados.isEmpty()) {
            return 0.0;
        }
        long habitables = resultados.stream().filter(ResultadoHabitabilidad::isHabitable).count();
        return 100.0 * habitables / resultados.size();
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
