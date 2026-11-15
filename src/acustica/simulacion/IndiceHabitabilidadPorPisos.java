package acustica.simulacion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import acustica.normativa.ResultadoHabitabilidad;

/**
 * Un IndiceHabitabilidad (AVL) por cada piso del edificio.
 *
 * Sirve para el reporte por pisos: en qué piso está el peor caso, cuántas
 * habitaciones tiene cada piso y qué habitaciones de un piso superan un
 * umbral de ruido.
 */
public class IndiceHabitabilidadPorPisos {

    private final Map<Integer, IndiceHabitabilidad> porPiso = new TreeMap<>();

    public IndiceHabitabilidadPorPisos(List<ResultadoHabitabilidad> resultados) {
        Map<Integer, List<ResultadoHabitabilidad>> grupos = new TreeMap<>();
        for (ResultadoHabitabilidad r : resultados) {
            grupos.computeIfAbsent(r.getHabitacion().getPiso(), k -> new ArrayList<>()).add(r);
        }
        for (Map.Entry<Integer, List<ResultadoHabitabilidad>> e : grupos.entrySet()) {
            porPiso.put(e.getKey(), new IndiceHabitabilidad(e.getValue()));
        }
    }

    /** Pisos que tienen habitaciones, de menor a mayor. */
    public Set<Integer> pisos() {
        return Collections.unmodifiableSet(porPiso.keySet());
    }

    /** Índice AVL de un piso, o null si ese piso no existe. */
    public IndiceHabitabilidad indiceDe(int piso) {
        return porPiso.get(piso);
    }

    /** Piso cuyo peor caso tiene el mayor exceso; null si no hay resultados. */
    public Integer pisoMasCritico() {
        Integer critico = null;
        double mayor = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Integer, IndiceHabitabilidad> e : porPiso.entrySet()) {
            double exceso = e.getValue().peorCaso().getExceso();
            if (critico == null || exceso > mayor) {
                critico = e.getKey();
                mayor = exceso;
            }
        }
        return critico;
    }
}