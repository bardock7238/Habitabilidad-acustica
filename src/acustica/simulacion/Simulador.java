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


public class Simulador {

    private final Edificio edificio;
    private final Normativa normativa;
    private final List<ResultadoHabitabilidad> resultados;

    public Simulador(Edificio edificio, Normativa normativa) {
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
        resultados.clear();
        Grafo grafo = Grafo.desdeEdificio(edificio);

        for (Habitacion destino : edificio.getHabitaciones()) {
            double nivelTotal = calcularNivelTotal(destino, horario, grafo);
            destino.setNivelRuido(nivelTotal);

            try {
                boolean habitable = destino.evaluarHabitabilidad(normativa, horario);
                double limite = normativa.obtenerLimite(destino.getTipo(), horario);
                resultados.add(new ResultadoHabitabilidad(destino, nivelTotal, limite, habitable));
            } catch (IllegalArgumentException sinLimite) {
                // La normativa no define límite para este tipo/horario (p.ej. Sala-noche
                // no está en la OMS). No se puede evaluar; se deja constancia y se sigue
                // con las demás habitaciones en vez de detener toda la simulación.
                System.out.println("Aviso: sin limite normativo para " + destino.getTipo()
                        + "-" + horario + " (habitacion " + destino.getNombre() + ")");
            }
        }

        return resultados;
    }

    /**
     * Suma el aporte de todas las fuentes activas del edificio (de cualquier
     * habitación) que llegan a la habitación destino, usando el camino de
     * menor atenuación del grafo (Dijkstra).
     */
    private double calcularNivelTotal(Habitacion destino, String horario, Grafo grafo) {
        List<Double> aportes = new ArrayList<>();

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

    public double calcularPorcentajeHabitable() {
        if (resultados.isEmpty()) {
            return 0.0;
        }
        long habitables = resultados.stream().filter(ResultadoHabitabilidad::isHabitable).count();
        return 100.0 * habitables / resultados.size();
    }

    public List<ResultadoHabitabilidad> getResultados() {
        return resultados;
    }

    public Edificio getEdificio() {
        return edificio;
    }

    public Normativa getNormativa() {
        return normativa;
    }
}