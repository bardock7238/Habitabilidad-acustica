package acustica.simulacion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import acustica.estructuras.ArbolBMas;
import acustica.estructuras.NodoVisual;
import acustica.normativa.ResultadoHabitabilidad;

/**
 * Índice de los resultados de UNA simulación guardado en un árbol B+ cuya
 * clave es (piso, nivel recibido, habitación).
 *
 * QUÉ SE GUARDA: el {@link ResultadoHabitabilidad} completo, igual que en
 * {@link IndiceHabitabilidad} y {@link IndiceHabitabilidadPorPisos}. Así cada
 * nodo del índice permite leer el nombre, el nivel recibido, el límite y el
 * estado de la habitación sin volver a simular.
 *
 * POR QUÉ EL NIVEL ES {@code nivelRecibido}: el índice describe el ruido que
 * hay realmente en cada espacio. En una actividad de alto impacto,
 * {@code nivelFinal} puede ser el nivel del vecino más afectado y no el de la
 * habitación; usar ese valor haría que la habitación apareciera en el piso y
 * el nivel equivocados. El desempate es {@code habitacionId}, de modo que dos
 * habitaciones del mismo piso con el mismo nivel recibido se conservan como
 * claves distintas (el B+ no admite claves repetidas).
 *
 * CONSULTAS: el árbol queda ordenado por piso y, dentro del piso, por nivel
 * ascendente. Aprovechando las hojas enlazadas del B+:
 *
 * - {@link #habitacionesDePiso(int)}: todas las habitaciones de un piso.
 * - {@link #queSuperanNivel(int, double)}: las que están por encima de un
 *   nivel dentro de un piso (comparación estricta).
 * - {@link #enRangoDeNiveles(int, double, double)}: las que caen en un rango
 *   de niveles de un piso (ambos extremos incluidos).
 *
 * Cada consulta devuelve los resultados en el orden del índice: piso
 * ascendente y, dentro del piso, nivel ascendente; los empates se resuelven
 * por id de habitación.
 */
public class IndicePorPisoYNivel {

    /** Orden del B+. Con 4 caben hasta 3 claves por hoja. */
    private static final int ORDEN = 4;

    private final ArbolBMas<ClavePisoNivel, ResultadoHabitabilidad> arbol =
            new ArbolBMas<>(ORDEN);
    private final Set<Integer> pisos = new TreeSet<>();

    public IndicePorPisoYNivel(List<ResultadoHabitabilidad> resultados) {
        if (resultados == null) {
            throw new IllegalArgumentException("La lista de resultados no puede ser null");
        }
        for (ResultadoHabitabilidad resultado : resultados) {
            ClavePisoNivel clave = ClavePisoNivel.de(resultado);
            if (!arbol.insertar(clave, resultado)) {
                throw new IllegalArgumentException(
                        "Habitación repetida en el índice: " + clave.getHabitacionId());
            }
            pisos.add(clave.getPiso());
        }
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /** Todas las habitaciones de un piso, de menor a mayor nivel recibido. */
    public List<ResultadoHabitabilidad> habitacionesDePiso(int piso) {
        return arbol.rango(ClavePisoNivel.primeraDePiso(piso), ClavePisoNivel.ultimaDePiso(piso));
    }

    /**
     * Habitaciones de un piso cuyo nivel recibido es ESTRICTAMENTE mayor que
     * {@code nivel}, de menor a mayor nivel.
     */
    public List<ResultadoHabitabilidad> queSuperanNivel(int piso, double nivel) {
        if (Double.isNaN(nivel) || nivel == Double.POSITIVE_INFINITY) {
            return new ArrayList<>();
        }
        // nextUp deja fuera las habitaciones que están exactamente en el nivel.
        double siguiente = Math.nextUp(nivel);
        return arbol.rango(new ClavePisoNivel(piso, siguiente, ""),
                ClavePisoNivel.ultimaDePiso(piso));
    }

    /** Habitaciones de todo el edificio que superan un nivel, por piso y nivel. */
    public List<ResultadoHabitabilidad> queSuperanNivel(double nivel) {
        List<ResultadoHabitabilidad> resultado = new ArrayList<>();
        for (int piso : pisos) {
            resultado.addAll(queSuperanNivel(piso, nivel));
        }
        return resultado;
    }

    /**
     * Habitaciones de un piso con nivel recibido dentro de [desde, hasta],
     * ambos extremos incluidos. Si el rango está invertido o algún extremo es
     * NaN, devuelve una lista vacía.
     */
    public List<ResultadoHabitabilidad> enRangoDeNiveles(int piso, double desde, double hasta) {
        if (Double.isNaN(desde) || Double.isNaN(hasta) || desde > hasta) {
            return new ArrayList<>();
        }
        return arbol.rango(new ClavePisoNivel(piso, desde, ""),
                new ClavePisoNivel(piso, hasta, ClavePisoNivel.ID_MAXIMO));
    }

    /**
     * Habitaciones de todo el edificio con nivel recibido dentro de
     * [desde, hasta], ordenadas por piso y luego por nivel.
     */
    public List<ResultadoHabitabilidad> enRangoDeNiveles(double desde, double hasta) {
        List<ResultadoHabitabilidad> resultado = new ArrayList<>();
        for (int piso : pisos) {
            resultado.addAll(enRangoDeNiveles(piso, desde, hasta));
        }
        return resultado;
    }

    /** Pisos que tienen habitaciones, de menor a mayor. */
    public Set<Integer> pisos() {
        return Collections.unmodifiableSet(pisos);
    }

    /** Todos los resultados en el orden del índice (piso y nivel ascendentes). */
    public List<ResultadoHabitabilidad> ordenados() {
        List<ResultadoHabitabilidad> resultado = new ArrayList<>();
        for (int piso : pisos) {
            resultado.addAll(habitacionesDePiso(piso));
        }
        return resultado;
    }

    // ------------------------------------------------------------------
    // Estado del árbol
    // ------------------------------------------------------------------

    public int tamano() {
        return arbol.tamano();
    }

    public boolean estaVacio() {
        return arbol.estaVacio();
    }

    public int altura() {
        return arbol.altura();
    }

    public boolean cumpleInvariantes() {
        return arbol.cumpleInvariantes();
    }

    // ------------------------------------------------------------------
    // Dibujo
    // ------------------------------------------------------------------

    /**
     * Copia del B+ lista para dibujar. Cada clave muestra piso, nivel recibido
     * e id de la habitación (el id es el desempate y por eso también se ve).
     */
    public NodoVisual aNodoVisual() {
        return arbol.aNodoVisual(clave -> String.format("P%d %.1f %s",
                clave.getPiso(), clave.getNivel(), clave.getHabitacionId()));
    }

    // ------------------------------------------------------------------
    // Clave compuesta
    // ------------------------------------------------------------------

    /**
     * Clave (piso, nivel recibido, habitación). El id es obligatorio: sin él,
     * dos habitaciones del mismo piso con el mismo nivel tendrían la misma
     * clave y el B+ descartaría una.
     */
    public static final class ClavePisoNivel implements Comparable<ClavePisoNivel> {

        /** Texto que ordena después de cualquier id real (extremo superior). */
        static final String ID_MAXIMO = "\uffff";

        private final int piso;
        private final double nivel;
        private final String habitacionId;

        public ClavePisoNivel(int piso, double nivel, String habitacionId) {
            if (habitacionId == null) {
                throw new IllegalArgumentException("El id de la habitación no puede ser null");
            }
            this.piso = piso;
            this.nivel = nivel;
            this.habitacionId = habitacionId;
        }

        static ClavePisoNivel de(ResultadoHabitabilidad resultado) {
            if (resultado == null) {
                throw new IllegalArgumentException("El resultado no puede ser null");
            }
            return new ClavePisoNivel(
                    resultado.getHabitacion().getPiso(),
                    resultado.getNivelRecibido(),
                    resultado.getHabitacion().getId());
        }

        static ClavePisoNivel primeraDePiso(int piso) {
            return new ClavePisoNivel(piso, Double.NEGATIVE_INFINITY, "");
        }

        static ClavePisoNivel ultimaDePiso(int piso) {
            return new ClavePisoNivel(piso, Double.POSITIVE_INFINITY, ID_MAXIMO);
        }

        public int getPiso() {
            return piso;
        }

        public double getNivel() {
            return nivel;
        }

        public String getHabitacionId() {
            return habitacionId;
        }

        @Override
        public int compareTo(ClavePisoNivel otra) {
            int porPiso = Integer.compare(piso, otra.piso);
            if (porPiso != 0) {
                return porPiso;
            }
            int porNivel = Double.compare(nivel, otra.nivel);
            if (porNivel != 0) {
                return porNivel;
            }
            return habitacionId.compareTo(otra.habitacionId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ClavePisoNivel)) {
                return false;
            }
            ClavePisoNivel otra = (ClavePisoNivel) obj;
            return piso == otra.piso
                    && Double.compare(nivel, otra.nivel) == 0
                    && habitacionId.equals(otra.habitacionId);
        }

        @Override
        public int hashCode() {
            int hash = Integer.hashCode(piso);
            hash = 31 * hash + Double.hashCode(nivel);
            return 31 * hash + habitacionId.hashCode();
        }

        @Override
        public String toString() {
            return String.format("(piso %d, %.1f dB, %s)", piso, nivel, habitacionId);
        }
    }
}
