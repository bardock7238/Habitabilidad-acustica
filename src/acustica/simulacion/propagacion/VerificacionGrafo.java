package acustica.simulacion.propagacion;

import acustica.dominio.Edificio;
import acustica.dominio.Habitacion;
import acustica.dominio.Material;
import acustica.dominio.Superficie;

import java.util.List;
/**
 * Verificación manual de las Tareas 3.1 (Grafo) y 3.2 (Dijkstra).
 *
 * Arma edificios de ejemplo (habitaciones conectadas por pared, con
 * pasillo, con fachada a una vía) y comprueba que:
 *   - Las habitaciones conectadas por una pared interior queden
 *     conectadas entre sí en el grafo (en ambos sentidos).
 *   - Las fachadas conecten con el nodo EXTERIOR.
 *   - Dijkstra encuentre el camino correcto y la atenuación acumulada
 *     coincida con lo calculado a mano.
 */
/**
 *  Se ejecuta con:
 * javac -d out src/acustica/dominio/*.java src/acustica/normativa/*.java src/acustica/simulacion/propagacion/*.java
 * java -cp out acustica.simulacion.propagacion.VerificacionGrafo
 */
public class VerificacionGrafo {

    public static void main(String[] args) {
        int total = 0;
        int fallos = 0;

        // Escenario 1: dos habitaciones unidas por una pared
        Material concreto = new Material("Concreto", 30.0, 0.15);

        Habitacion dormitorio = new Habitacion("D1", "Dormitorio", "Dormitorio", 3, 4, 2.5);
        Habitacion sala = new Habitacion("S1", "Sala", "Sala", 4, 5, 2.5);

        Superficie muro = new Superficie("M1", "muro", 8.0, concreto, dormitorio, sala);
        dormitorio.agregarSuperficie(muro);
        sala.agregarSuperficie(muro);

        Superficie fachadaSala = new Superficie("F1", "fachada", 12.0, concreto, sala);
        sala.agregarSuperficie(fachadaSala);

        Edificio edificio1 = new Edificio("Edificio 1: dos habitaciones + fachada");
        edificio1.agregarHabitacion(dormitorio);
        edificio1.agregarHabitacion(sala);

        Grafo grafo1 = Grafo.desdeEdificio(edificio1);

        System.out.println("=== Escenario 1: " + edificio1.getNombre() + " ===");
        total++; fallos += verificar("D1 y S1 conectadas (pared interior)",
                estanConectados(grafo1, "D1", "S1"));
        total++; fallos += verificar("S1 y D1 conectadas (sentido inverso)",
                estanConectados(grafo1, "S1", "D1"));
        total++; fallos += verificar("S1 conectada a EXTERIOR (fachada)",
                estanConectados(grafo1, "S1", Grafo.EXTERIOR));
        total++; fallos += verificar("D1 NO conectada a EXTERIOR",
                !estanConectados(grafo1, "D1", Grafo.EXTERIOR));

        // Dijkstra sobre el escenario 1: camino desde EXTERIOR hasta D1
        Dijkstra.Resultado r1 = Dijkstra.calcular(grafo1, Grafo.EXTERIOR);
        List<String> camino1 = r1.reconstruirCamino("D1");

        total++; fallos += verificar("Existe camino de EXTERIOR a D1",
                !camino1.isEmpty());
        total++; fallos += verificar("El camino empieza en EXTERIOR y termina en D1",
                !camino1.isEmpty() && camino1.get(0).equals(Grafo.EXTERIOR)
                        && camino1.get(camino1.size() - 1).equals("D1"));
        total++; fallos += verificar("El camino pasa por S1 (unica ruta posible)",
                camino1.contains("S1"));
        total++; fallos += verificar("La atenuacion hasta D1 es mayor que hasta S1 (D1 esta mas lejos)",
                r1.getAtenuacionHasta("D1") > r1.getAtenuacionHasta("S1"));

        // Escenario 2: edificio con pasillo
        Habitacion h1 = new Habitacion("H1", "Oficina 1", "Oficina", 3, 3, 2.5);
        Habitacion pasillo = new Habitacion("P1", "Pasillo", "Pasillo", 2, 6, 2.5);
        Habitacion h2 = new Habitacion("H2", "Oficina 2", "Oficina", 3, 3, 2.5);

        Superficie puerta1 = new Superficie("PU1", "puerta", 2.0, concreto, h1, pasillo);
        Superficie puerta2 = new Superficie("PU2", "puerta", 2.0, concreto, pasillo, h2);
        h1.agregarSuperficie(puerta1);
        pasillo.agregarSuperficie(puerta1);
        pasillo.agregarSuperficie(puerta2);
        h2.agregarSuperficie(puerta2);

        Edificio edificio2 = new Edificio("Edificio 2: con pasillo");
        edificio2.agregarHabitacion(h1);
        edificio2.agregarHabitacion(pasillo);
        edificio2.agregarHabitacion(h2);

        Grafo grafo2 = Grafo.desdeEdificio(edificio2);

        System.out.println("\n=== Escenario 2: " + edificio2.getNombre() + " ===");
        total++; fallos += verificar("H1 y Pasillo conectadas",
                estanConectados(grafo2, "H1", "P1"));
        total++; fallos += verificar("Pasillo y H2 conectadas",
                estanConectados(grafo2, "P1", "H2"));
        total++; fallos += verificar("H1 y H2 NO conectadas directamente",
                !estanConectados(grafo2, "H1", "H2"));

        // Dijkstra sobre el escenario 2: camino desde H1 hasta H2, pasando por el pasillo
        Dijkstra.Resultado r2 = Dijkstra.calcular(grafo2, "H1");
        List<String> camino2 = r2.reconstruirCamino("H2");

        total++; fallos += verificar("Existe camino de H1 a H2 (via el pasillo)",
                !camino2.isEmpty());
        total++; fallos += verificar("El camino de H1 a H2 pasa por el Pasillo",
                camino2.contains("P1"));
        total++; fallos += verificar("La atenuacion de H1 a H2 es la suma de las dos puertas",
                r2.getAtenuacionHasta("H2") == puerta1.calcularAtenuacion() + puerta2.calcularAtenuacion());

        // Escenario 3: edificio con fachada a una vía
        Habitacion recepcion = new Habitacion("R1", "Recepcion", "Oficina", 5, 4, 2.5);
        Superficie fachadaVia = new Superficie("FV1", "fachada", 15.0, concreto, recepcion);
        recepcion.agregarSuperficie(fachadaVia);

        Edificio edificio3 = new Edificio("Edificio 3: fachada a una via");
        edificio3.agregarHabitacion(recepcion);

        Grafo grafo3 = Grafo.desdeEdificio(edificio3);

        System.out.println("\n=== Escenario 3: " + edificio3.getNombre() + " ===");
        total++; fallos += verificar("Recepcion conectada a EXTERIOR",
                estanConectados(grafo3, "R1", Grafo.EXTERIOR));

        Dijkstra.Resultado r3 = Dijkstra.calcular(grafo3, Grafo.EXTERIOR);
        total++; fallos += verificar("Atenuacion EXTERIOR->Recepcion = atenuacion de la fachada",
                r3.getAtenuacionHasta("R1") == fachadaVia.calcularAtenuacion());

        // Resumen
        System.out.println("\n=== Resumen ===");
        System.out.println((total - fallos) + " / " + total + " verificaciones OK");
        if (fallos > 0) {
            System.out.println("HAY " + fallos + " VERIFICACION(ES) FALLIDA(S).");
        } else {
            System.out.println("Todas las verificaciones pasaron correctamente.");
        }
    }

    private static boolean estanConectados(Grafo grafo, String desde, String hacia) {
        return grafo.getVecinos(desde).stream()
                .anyMatch(arista -> arista.getDestino().equals(hacia));
    }

    private static int verificar(String descripcion, boolean condicion) {
        System.out.println((condicion ? "[OK]   " : "[FALLO]") + " " + descripcion);
        return condicion ? 0 : 1;
    }
}