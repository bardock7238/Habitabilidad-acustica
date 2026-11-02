package acustica.simulacion.propagacion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import acustica.dominio.Edificio;
import acustica.dominio.Habitacion;
import acustica.dominio.Superficie;

/**
 * Grafo de adyacencia dirigido del edificio (Tarea 3.1).
 *
 * Recorre el edificio y construye el grafo que usa la propagación:
 * - Cada habitación es un nodo (identificado por su id).
 * - Cada superficie interior genera DOS aristas dirigidas (i → j y j → i),
 *   porque la atenuación depende de la absorción del recinto receptor.
 * - Las fachadas (superficies con una sola habitación) conectan la habitación
 *   con el nodo especial {@link #EXTERIOR} en ambos sentidos.
 * - El peso de cada arista es la atenuación en dB que aporta la superficie:
 *
 *       w_{i→j} = max(0, R_k − 10·log10(S_k / A_j))
 *
 *   donde {@code A_j} es la absorción equivalente (Sabine) del recinto que
 *   recibe el sonido. Hacia {@link #EXTERIOR} se usa {@code w = R_k}.
 */
public class Grafo {

    /** Nodo especial que representa el exterior del edificio. */
    public static final String EXTERIOR = "EXTERIOR";

    private final Map<String, List<Arista>> nodos;

    public Grafo() {
        this.nodos = new LinkedHashMap<>();
    }

    /**
     * Construye el grafo de un edificio recorriendo sus habitaciones y superficies.
     *
     * @param edificio edificio a modelar
     * @return grafo con los nodos y conexiones del edificio
     */
    public static Grafo desdeEdificio(Edificio edificio) {
        if (edificio == null) {
            throw new IllegalArgumentException("El edificio no puede ser null");
        }
        Grafo grafo = new Grafo();
        grafo.agregarNodo(EXTERIOR);

        Set<String> superficiesVistas = new LinkedHashSet<>();
        for (Habitacion habitacion : edificio.getHabitaciones()) {
            grafo.agregarNodo(habitacion.getId());

            for (Superficie superficie : habitacion.getSuperficies()) {
                if (!superficiesVistas.add(superficie.getId())) {
                    continue; // superficie interior ya procesada desde la otra habitación
                }

                Habitacion h1 = superficie.getHabitacion1();
                if (superficie.esParedInterior()) {
                    Habitacion h2 = superficie.getHabitacion2();
                    grafo.agregarArista(h1.getId(), h2.getId(), superficie.calcularAtenuacionHacia(h2));
                    grafo.agregarArista(h2.getId(), h1.getId(), superficie.calcularAtenuacionHacia(h1));
                } else {
                    grafo.agregarArista(h1.getId(), EXTERIOR, superficie.calcularAtenuacion());
                    grafo.agregarArista(EXTERIOR, h1.getId(), superficie.calcularAtenuacionHacia(h1));
                }
            }
        }

        return grafo;
    }

    /** Agrega un nodo al grafo (lo crea vacío si no existe). */
    public void agregarNodo(String id) {
        nodos.putIfAbsent(id, new ArrayList<>());
    }

    /** Agrega una conexión dirigida desde el nodo origen hacia el destino. */
    public void agregarArista(String origen, String destino, double peso) {
        agregarNodo(origen);
        agregarNodo(destino);
        nodos.get(origen).add(new Arista(destino, peso));
    }

    /** Agrega una conexión en ambos sentidos entre dos nodos, con el mismo peso. */
    public void agregarAristaNoDirigida(String a, String b, double peso) {
        agregarArista(a, b, peso);
        agregarArista(b, a, peso);
    }

    /** Indica si el nodo existe en el grafo. */
    public boolean contieneNodo(String id) {
        return nodos.containsKey(id);
    }

    /** Devuelve las conexiones de salida de un nodo (vacía si no existe). */
    public List<Arista> getVecinos(String nodo) {
        List<Arista> vecinos = nodos.get(nodo);
        return vecinos == null ? List.of() : List.copyOf(vecinos);
    }

    /** Devuelve todos los ids de los nodos del grafo (vista de solo lectura). */
    public Set<String> getNodos() {
        return Collections.unmodifiableSet(nodos.keySet());
    }

    /** Conexión entre un nodo y otro, con la atenuación como peso. */
    public static class Arista {
        private final String destino;
        private final double peso;

        public Arista(String destino, double peso) {
            this.destino = destino;
            this.peso = peso;
        }

        public String getDestino() {
            return destino;
        }

        public double getPeso() {
            return peso;
        }

        @Override
        public String toString() {
            return "-> " + destino + " (" + peso + " dB)";
        }
    }
}
