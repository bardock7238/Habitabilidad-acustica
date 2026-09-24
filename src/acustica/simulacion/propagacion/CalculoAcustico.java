package acustica.simulacion.propagacion;

import java.util.List;

import acustica.dominio.FuenteSonido;
import acustica.dominio.Habitacion;

/**
 * Cálculo acústico del rayo (Tarea 4.2).
 *
 * Calcula cuánta energía pierde cada rayo al atravesar cada superficie
 * y suma los aportes para obtener el nivel de ruido de cada habitación.
 *
 * Depende de:
 * - Tarea 4.1 (RayTracing): origen de los rayos y su superficie impactada.
 * - Tarea 1.1 (Material): coeficiente de atenuación del material.
 * - Tarea 1.3 (Superficie): Superficie.calcularAtenuacion() en dB.
 *
 * Modelo:
 * - Cada rayo pierde la atenuación de la superficie que atraviesa:
 *   nivelAtenuado = nivelInicial - atenuación.
 * - Si el rayo no impactó ninguna superficie, no pierde energía.
 * - Los aportes se suman de forma energética (incoherente):
 *   L_total = 10 * log10( suma( 10^(Li/10) ) ).
 */
public class CalculoAcustico {

    private CalculoAcustico() {
        // Utilidad estática: no instanciable.
    }

    /**
     * Calcula el nivel con el que un rayo llega tras atravesar su superficie.
     *
     * @param rayo rayo ya emitido por RayTracing
     * @return nivelInicial menos la atenuación de la superficie impactada;
     *         si no hubo impacto, devuelve el nivel inicial sin cambios
     */
    public static double calcularNivelRayo(RayTracing.Rayo rayo) {
        if (rayo == null) {
            throw new IllegalArgumentException("El rayo no puede ser null");
        }
        if (!rayo.impacto()) {
            return rayo.getNivelInicial();
        }
        double atenuacion = Math.max(0.0, rayo.getSuperficieImpactada().calcularAtenuacion());
        return rayo.getNivelInicial() - atenuacion;
    }

    /**
     * Suma energética de varios niveles en dB.
     *
     * @param niveles niveles individuales en dB
     * @return nivel combinado; Double.NEGATIVE_INFINITY si no hay aportes
     */
    public static double sumarNiveles(double... niveles) {
        if (niveles == null || niveles.length == 0) {
            return Double.NEGATIVE_INFINITY;
        }
        double sumaEnergia = 0.0;
        for (double nivel : niveles) {
            if (Double.isNaN(nivel) || nivel == Double.NEGATIVE_INFINITY) {
                continue;
            }
            sumaEnergia += Math.pow(10.0, nivel / 10.0);
        }
        if (sumaEnergia <= 0.0) {
            return Double.NEGATIVE_INFINITY;
        }
        return 10.0 * Math.log10(sumaEnergia);
    }

    /**
     * Calcula el nivel de ruido que aportan varios rayos a una habitación.
     * Cada rayo se atenúa con su superficie y luego se suman los aportes.
     *
     * @param rayos rayos que llegan a la habitación (ya emitidos)
     * @return nivel total en dB; Double.NEGATIVE_INFINITY si la lista está vacía
     */
    public static double calcularNivelHabitacion(List<RayTracing.Rayo> rayos) {
        if (rayos == null || rayos.isEmpty()) {
            return Double.NEGATIVE_INFINITY;
        }
        double[] niveles = new double[rayos.size()];
        for (int i = 0; i < rayos.size(); i++) {
            niveles[i] = calcularNivelRayo(rayos.get(i));
        }
        return sumarNiveles(niveles);
    }

    /**
     * Propaga una fuente dentro de su habitación: emite los rayos,
     * calcula la pérdida de cada uno y suma el nivel total recibido.
     *
     * @param fuente fuente de sonido que emite
     * @param habitacion habitación donde está la fuente
     * @param cantidadRayos cuántos rayos lanzar (ver RayTracing.emitirRayos)
     * @return nivel total en dB dentro de la habitación de origen
     */
    public static double propagarEnHabitacion(FuenteSonido fuente, Habitacion habitacion, int cantidadRayos) {
        List<RayTracing.Rayo> rayos = RayTracing.emitirRayos(fuente, habitacion, cantidadRayos);
        return calcularNivelHabitacion(rayos);
    }

    /**
     * Calcula el nivel que llega a un destino lejano restando la atenuación
     * acumulada del camino de menor atenuación (Dijkstra).
     *
     * @param nivelEmitido nivel en dB al salir de la fuente
     * @param atenuacionAcumulada atenuación total del camino en dB (>= 0)
     * @return nivel recibido; Double.NEGATIVE_INFINITY si el destino es inalcanzable
     */
    public static double calcularNivelConAtenuacion(double nivelEmitido, double atenuacionAcumulada) {
        if (Double.isInfinite(atenuacionAcumulada) || Double.isNaN(atenuacionAcumulada)) {
            return Double.NEGATIVE_INFINITY;
        }
        return nivelEmitido - Math.max(0.0, atenuacionAcumulada);
    }

    /**
     * Nivel que llega desde una fuente hasta otra habitación usando el grafo.
     *
     * @param fuente fuente de sonido
     * @param habitacionOrigenId id del nodo donde está la fuente
     * @param habitacionDestinoId id de la habitación a evaluar
     * @param grafo grafo del edificio (Tarea 3.1)
     * @return nivel en dB en el destino; Double.NEGATIVE_INFINITY si no hay camino
     */
    public static double calcularNivelEnDestino(FuenteSonido fuente, String habitacionOrigenId,
            String habitacionDestinoId, Grafo grafo) {
        if (fuente == null || habitacionOrigenId == null || habitacionDestinoId == null || grafo == null) {
            throw new IllegalArgumentException("Ningún argumento puede ser null");
        }
        Dijkstra.Resultado resultado = Dijkstra.calcular(grafo, habitacionOrigenId);
        double atenuacion = resultado.getAtenuacionHasta(habitacionDestinoId);
        return calcularNivelConAtenuacion(fuente.obtenerNivelEmitido(), atenuacion);
    }
}
