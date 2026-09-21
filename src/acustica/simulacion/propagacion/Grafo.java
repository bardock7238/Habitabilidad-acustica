package acustica.simulacion.propagacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import acustica.dominio.Edificio;
import acustica.dominio.Habitacion;
import acustica.dominio.Superficie;

/**
 * Grafo de adyacencia del edificio (Tarea 3.1).
 *
 * Recorre el edificio y construye el grafo que usará la propagación:
 * - Cada habitación es un nodo (identificado por su id).
 * - Cada superficie interior es una conexión entre sus dos habitaciones.
 * - Las fachadas (superficies con una sola habitación) conectan con el
 *   nodo especial EXTERIOR.
 * - El peso de cada conexión es la atenuación que aporta la superficie
 *   (Superficie.calcularAtenuacion()).
 *
 * El grafo es no dirigido: el sonido puede propagarse en ambos sentidos.
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
        Grafo grafo = new Grafo();
        grafo.agregarNodo(EXTERIOR);

        Set<String> superficiesVistas = new LinkedHashSet<>();
        for (Habitacion habitacion : edificio.getHabitaciones()) {
            grafo.agregarNodo(habitacion.getId());

            for (Superficie superficie : habitacion.getSuperficies()) {
                if (!superficiesVistas.add(superficie.getId())) {
                    continue; // superficie interior ya procesada desde la otra habitación
                }

                String origen = superficie.getHabitacion1().getId();
                String destino = superficie.esParedInterior()
                        ? superficie.getHabitacion2().getId()
                        : EXTERIOR;

                grafo.agregarAristaNoDirigida(origen, destino, superficie.calcularAtenuacion());
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

    /** Agrega una conexión en ambos sentidos entre dos nodos. */
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
        return vecinos == null ? new ArrayList<>() : vecinos;
    }

    /** Devuelve todos los ids de los nodos del grafo. */
    public Set<String> getNodos() {
        return nodos.keySet();
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