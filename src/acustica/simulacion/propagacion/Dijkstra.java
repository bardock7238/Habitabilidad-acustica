package acustica.simulacion.propagacion;

import java.util.ArrayList;
import java.util.Collections; //Utilidad para listas
import java.util.HashMap; // Para guardar pares clave. 
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue; // Una cola que saca primero el elemento "menor"

/** Implementación de Dijkstra sobre el Grafo del edificio para encontrar desde
 * un nodo de origen (la fuente del ruido), el camino que acumula la MENOR atenuación
 * en decibeles hacia cada habitación./**
  *  El peso de cada arista (Grafo.Arista.getPeso()) es la atenuación en dB
 * que aporta esa superficie; siempre es >= 0, así que Dijkstra clásico
 * aplica sin modificaciones.
  */
 public class Dijkstra {
    /** Resultado de correr Dijkstra dedde un nodo origen
     * para cada nodo, la atenuación acumulada mínima y el nodo anterior (para 
     * reconstruir ese camino)
     */

    public static class Resultado{
        private final Map<String, Double> atenuacionAcumulada;
        private final Map<String, String> predecesor;
        
        private Resultado(Map<String, Double> atenuacionAcumulada, Map <String, String> predecesor){
            this.atenuacionAcumulada = atenuacionAcumulada;
            this.predecesor = predecesor;
        }

        /** Atenuación acumulada mínima en dB desde el origen hasta el nodo dado. 
         * Devuelve Double.POSITIVE_INFINITY si el nodo no es alcanzable.
         */
        public double getAtenuacionHasta(String nodo){
            return atenuacionAcumulada.getOrDefault(nodo, Double.POSITIVE_INFINITY);
        }

        /** Reconstruye el camino (lista de ids de nodo, partiendo del origen) de menor 
         * atenuación hasta el nodo dado. Devuelve una lista vacia si NO es alcanzable
         */
                public List<String> reconstruirCamino(String destino) {
            if (!atenuacionAcumulada.containsKey(destino)) {
                return new ArrayList<>();
            }

            List<String> camino = new ArrayList<>();
            String actual = destino;
            while (actual !=  null){
                camino.add(actual);
                actual = predecesor.get(actual);
            }

                        Collections.reverse(camino);
            return camino;
        }
 
        public Map<String, Double> getAtenuacionAcumulada() {
            return atenuacionAcumulada;
        }
    }

    // Nodo + atenuación acumulada hasta él, para la cola de prioridad
    private static class NodoConAtenuacion implements Comparable<NodoConAtenuacion>{
        final String id;
        final double atenuacion;

        NodoConAtenuacion(String id, double atenuacion){
            this.id = id;
            this.atenuacion = atenuacion;
        }

        @Override 
        public int compareTo(NodoConAtenuacion otro){
            return Double.compare(this.atenuacion, otro.atenuacion);
        }
    }

    
    /**
     * Corre Dijkstra sobre el grafo desde el nodo origen.
     * @param grafo  grafo del edificio (habitaciones + EXTERIOR)
     * @param origen id del nodo donde está la fuente de ruido
     * @return resultado con la atenuación mínima acumulada hacia cada nodo alcanzable
     */

    public static Resultado calcular(Grafo grafo, String origen){
        if(!grafo.contieneNodo(origen)){
            throw new IllegalArgumentException("El nodo origen '" + origen + "no existe en el grafo");
        }

        Map<String, Double> atenuacionAcumulada = new HashMap<>();
        Map <String, String> predecesor = new HashMap<>();
        Map <String, Boolean> visitado = new HashMap<>();

        PriorityQueue<NodoConAtenuacion> pendientes = new PriorityQueue<>();

        atenuacionAcumulada.put(origen, 0.0);
        pendientes.add(new NodoConAtenuacion(origen, 0.0));

        while (!pendientes.isEmpty()) {
            NodoConAtenuacion actual = pendientes.poll();
 
            if (Boolean.TRUE.equals(visitado.get(actual.id))) {
                continue; // ya procesado con una atenuación menor o igual
            }
            visitado.put(actual.id, true);
 
            for (Grafo.Arista arista : grafo.getVecinos(actual.id)) {
                String vecino = arista.getDestino();
                double nuevaAtenuacion = actual.atenuacion + arista.getPeso();
                double atenuacionPrevia = atenuacionAcumulada.getOrDefault(vecino, Double.POSITIVE_INFINITY);
 
                if (nuevaAtenuacion < atenuacionPrevia) {
                    atenuacionAcumulada.put(vecino, nuevaAtenuacion);
                    predecesor.put(vecino, actual.id);
                    pendientes.add(new NodoConAtenuacion(vecino, nuevaAtenuacion));
                }
            }
        }
 
        return new Resultado(atenuacionAcumulada, predecesor);
    }



    }
 
