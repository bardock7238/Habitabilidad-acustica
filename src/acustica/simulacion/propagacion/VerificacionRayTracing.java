package acustica.simulacion.propagacion;

import acustica.dominio.Edificio;
import acustica.dominio.FuenteSonido;
import acustica.dominio.Habitacion;
import acustica.dominio.Material;
import acustica.dominio.Superficie;
import acustica.normativa.Normativa;

import java.util.List;

/**
 * Casos de prueba de la Tarea 4.3 (Ray tracing).
 *
 * Diseña los tres escenarios pedidos y comprueba que los resultados del
 * modelo (RayTracing + CalculoAcustico, y Grafo + Dijkstra para los destinos)
 * coincidan con los valores esperados calculados a mano:
 *
 *   1. Edificio de dos habitaciones: fuente en la Sala, dormitorio adyacente.
 *   2. Edificio con pasillo: Oficina 1 -&gt; Pasillo -&gt; Oficina 2.
 *   3. Edificio con fachada a una vía: ruido exterior que entra por la fachada.
 *
 * Truco del cálculo a mano: si la habitación de origen tiene una superficie
 * total entera A (m2) y emitimos N = A rayos, cada superficie recibe un
 * número de rayos proporcional a su área (los 360° se reparten por área).
 * Se eligen áreas que den cortes exactos (múltiplos del paso angular) para
 * que el reparto no dependa de redondeos de coma flotante. Así los resultados
 * se verifican con lápiz y papel (ver docs/casos-de-prueba-raytracing.md).
 *
 * Se ejecuta con:
 *   javac -d out src/acustica/dominio/*.java src/acustica/normativa/*.java \
 *         src/acustica/simulacion/propagacion/*.java
 *   java -cp out acustica.simulacion.propagacion.VerificacionRayTracing
 */
public class VerificacionRayTracing {

    /** Tolerancia (en dB) para comparar niveles calculados vs. esperados. */
    private static final double TOLERANCIA_DB = 0.05;

    public static void main(String[] args) {
        int total = 0;
        int fallos = 0;

        // ============================================================
        // Escenario 1: edificio de dos habitaciones
        // ============================================================
        Material concreto = new Material("Concreto", 3.0, 0.15);
        Material aislante = new Material("Aislante acustico", 1.5, 0.10);
        Material ladrillo = new Material("Ladrillo hueco", 2.0, 0.12);
        Material madera = new Material("Madera", 2.5, 0.10);

        Habitacion sala = new Habitacion("S1", "Sala", "Sala", 4, 3, 3);
        Habitacion dormitorio = new Habitacion("D1", "Dormitorio", "Dormitorio", 3, 4, 3);

        // M1 attenuates 30 dB, F1 15 dB, P1 20 dB, T1 25 dB.
        Superficie medianera = new Superficie("M1", "pared interior", 10.0, concreto, sala, dormitorio);
        Superficie fachadaSala = new Superficie("F1", "fachada", 10.0, aislante, sala);
        Superficie pisoSala = new Superficie("P1", "piso", 10.0, ladrillo, sala);
        Superficie techoSala = new Superficie("T1", "techo", 10.0, madera, sala);

        sala.agregarSuperficie(medianera);
        sala.agregarSuperficie(fachadaSala);
        sala.agregarSuperficie(pisoSala);
        sala.agregarSuperficie(techoSala);
        dormitorio.agregarSuperficie(medianera);

        FuenteSonido fuenteSala = new FuenteSonido("F-S1", FuenteSonido.Tipo.FIJA, 80.0, 500, FuenteSonido.Horario.DIA);

        Edificio edificio1 = new Edificio("Edificio 1: dos habitaciones");
        edificio1.agregarHabitacion(sala);
        edificio1.agregarHabitacion(dormitorio);

        // Area total de la sala = 10 + 10 + 10 + 10 = 40 m2 -> 40 rayos.
        int n1 = 40;
        List<RayTracing.Rayo> rayos1 = RayTracing.emitirRayos(fuenteSala, sala, n1);
        double nivelSala = CalculoAcustico.calcularNivelHabitacion(rayos1);

        Grafo grafo1 = Grafo.desdeEdificio(edificio1);
        double nivelDorm1 = CalculoAcustico.calcularNivelEnDestino(fuenteSala, "S1", "D1", grafo1);

        System.out.println("=== Escenario 1: " + edificio1.getNombre() + " ===");
        System.out.println("Reparto de rayos esperado: 10 en cada superficie (N=40, cortes a 90/180/270 grados)");
        System.out.printf("Nivel Sala (ray tracing)    = %.2f dB  (esperado 76.61 dB)\n", nivelSala);
        System.out.printf("Nivel Dormitorio (Dijkstra) = %.2f dB  (esperado 50.00 dB)\n\n", nivelDorm1);

        total++; fallos += verificar("La medianera recibe 10 rayos (a 50 dB)",
                contarRayos(rayos1, "M1") == 10);
        total++; fallos += verificar("La fachada recibe 10 rayos (a 65 dB)",
                contarRayos(rayos1, "F1") == 10);
        total++; fallos += verificar("El piso recibe 10 rayos (a 60 dB)",
                contarRayos(rayos1, "P1") == 10);
        total++; fallos += verificar("El techo recibe 10 rayos (a 55 dB)",
                contarRayos(rayos1, "T1") == 10);
        total++; fallos += verificar("Nivel de la Sala = 76.61 dB (suma energetica de los 4 aportes)",
                cercano(nivelSala, 76.6072, TOLERANCIA_DB));
        total++; fallos += verificar("Nivel del Dormitorio = 50.00 dB (80 - 30 de la medianera)",
                cercano(nivelDorm1, 50.0, TOLERANCIA_DB));

        // ============================================================
        // Escenario 2: edificio con pasillo
        // ============================================================
        Habitacion oficina1 = new Habitacion("H1", "Oficina 1", "Oficina", 4, 3, 3);
        Habitacion pasillo = new Habitacion("P1", "Pasillo", "Pasillo", 2, 6, 3);
        Habitacion oficina2 = new Habitacion("H2", "Oficina 2", "Oficina", 4, 3, 3);

        Superficie puerta1 = new Superficie("PU1", "puerta", 4.0, concreto, oficina1, pasillo);
        Superficie puerta2 = new Superficie("PU2", "puerta", 4.0, concreto, pasillo, oficina2);
        Superficie fachadaH1 = new Superficie("F1", "fachada", 12.0, concreto, oficina1);
        Superficie pisoH1 = new Superficie("P1", "piso", 12.0, concreto, oficina1);
        Superficie techoH1 = new Superficie("T1", "techo", 12.0, concreto, oficina1);

        oficina1.agregarSuperficie(puerta1);
        oficina1.agregarSuperficie(fachadaH1);
        oficina1.agregarSuperficie(pisoH1);
        oficina1.agregarSuperficie(techoH1);
        pasillo.agregarSuperficie(puerta1);
        pasillo.agregarSuperficie(puerta2);
        oficina2.agregarSuperficie(puerta2);

        FuenteSonido fuenteO1 = new FuenteSonido("F-H1", FuenteSonido.Tipo.FIJA, 80.0, 500, FuenteSonido.Horario.DIA);

        Edificio edificio2 = new Edificio("Edificio 2: con pasillo");
        edificio2.agregarHabitacion(oficina1);
        edificio2.agregarHabitacion(pasillo);
        edificio2.agregarHabitacion(oficina2);

        // Area total de la Oficina 1 = 4 + 12 + 12 + 12 = 40 m2 -> 40 rayos.
        int n2 = 40;
        List<RayTracing.Rayo> rayos2 = RayTracing.emitirRayos(fuenteO1, oficina1, n2);
        double nivelH1 = CalculoAcustico.calcularNivelHabitacion(rayos2);

        Grafo grafo2 = Grafo.desdeEdificio(edificio2);
        double nivelH2 = CalculoAcustico.calcularNivelEnDestino(fuenteO1, "H1", "H2", grafo2);
        Dijkstra.Resultado r2 = Dijkstra.calcular(grafo2, "H1");
        List<String> camino2 = r2.reconstruirCamino("H2");

        System.out.println("=== Escenario 2: " + edificio2.getNombre() + " ===");
        System.out.println("Reparto de rayos esperado: puerta 4, fachada 12, piso 12, techo 12");
        System.out.printf("Nivel Oficina 1 (ray tracing) = %.2f dB  (esperado 74.17 dB)\n", nivelH1);
        System.out.printf("Nivel Oficina 2 (Dijkstra)    = %.2f dB  (esperado 56.00 dB)\n\n", nivelH2);

        total++; fallos += verificar("La puerta del pasillo recibe 4 rayos (a 68 dB)",
                contarRayos(rayos2, "PU1") == 4);
        total++; fallos += verificar("La fachada recibe 12 rayos (a 44 dB)",
                contarRayos(rayos2, "F1") == 12);
        total++; fallos += verificar("El piso recibe 12 rayos (a 44 dB)",
                contarRayos(rayos2, "P1") == 12);
        total++; fallos += verificar("El techo recibe 12 rayos (a 44 dB)",
                contarRayos(rayos2, "T1") == 12);
        total++; fallos += verificar("Nivel de la Oficina 1 = 74.17 dB (4 rayos a 68 dB, 36 a 44 dB)",
                cercano(nivelH1, 74.1735, TOLERANCIA_DB));
        total++; fallos += verificar("El camino H1 -> H2 pasa por el pasillo",
                camino2.contains("P1"));
        total++; fallos += verificar("Nivel de la Oficina 2 = 56.00 dB (80 - 12 - 12 de las dos puertas)",
                cercano(nivelH2, 56.0, TOLERANCIA_DB));

        // ============================================================
        // Escenario 3: edificio con fachada a una via
        // ============================================================
        Habitacion salaVia = new Habitacion("S1", "Sala", "Sala", 5, 4, 3);
        Habitacion dormitorioInt = new Habitacion("D1", "Dormitorio", "Dormitorio", 3, 4, 3);

        Superficie fachadaVia = new Superficie("FV1", "fachada a la via", 20.0, aislante, salaVia);
        Superficie paredInt = new Superficie("PD1", "pared interior", 10.0, concreto, salaVia, dormitorioInt);

        salaVia.agregarSuperficie(fachadaVia);
        salaVia.agregarSuperficie(paredInt);
        dormitorioInt.agregarSuperficie(paredInt);

        FuenteSonido via = new FuenteSonido("V1", FuenteSonido.Tipo.MOVIL, 85.0, 500, FuenteSonido.Horario.NOCHE);

        Edificio edificio3 = new Edificio("Edificio 3: fachada a una via");
        edificio3.agregarHabitacion(salaVia);
        edificio3.agregarHabitacion(dormitorioInt);

        Grafo grafo3 = Grafo.desdeEdificio(edificio3);
        double nivelSalaVia = CalculoAcustico.calcularNivelEnDestino(via, Grafo.EXTERIOR, "S1", grafo3);
        double nivelDormInt = CalculoAcustico.calcularNivelEnDestino(via, Grafo.EXTERIOR, "D1", grafo3);

        Normativa normativa = new Normativa();
        boolean salaCumpleDia = normativa.cumple(nivelSalaVia, "Sala", "dia");
        boolean dormCumpleNoche = normativa.cumple(nivelDormInt, "Dormitorio", "noche");

        System.out.println("=== Escenario 3: " + edificio3.getNombre() + " ===");
        System.out.printf("Nivel Sala (via la fachada)         = %.2f dB  (esperado 55.00 dB)\n", nivelSalaVia);
        System.out.printf("Nivel Dormitorio (via fachada+pared) = %.2f dB  (esperado 25.00 dB)\n\n", nivelDormInt);

        total++; fallos += verificar("La Sala esta conectada a EXTERIOR (fachada a la via)",
                estanConectados(grafo3, "S1", Grafo.EXTERIOR));
        total++; fallos += verificar("El Dormitorio NO esta conectado a EXTERIOR directamente",
                !estanConectados(grafo3, "D1", Grafo.EXTERIOR));
        total++; fallos += verificar("Nivel de la Sala = 55.00 dB (85 - 30 de la fachada)",
                cercano(nivelSalaVia, 55.0, TOLERANCIA_DB));
        total++; fallos += verificar("Nivel del Dormitorio = 25.00 dB (85 - 30 fachada - 30 pared)",
                cercano(nivelDormInt, 25.0, TOLERANCIA_DB));
        total++; fallos += verificar("Sala de dia NO cumple la normativa (55 > 35)",
                !salaCumpleDia);
        total++; fallos += verificar("Dormitorio de noche cumple la normativa (25 <= 30)",
                dormCumpleNoche);

        // ============================================================
        // Resumen
        // ============================================================
        System.out.println("=== Resumen ===");
        System.out.println((total - fallos) + " / " + total + " verificaciones OK");
        if (fallos > 0) {
            System.out.println("HAY " + fallos + " VERIFICACION(ES) FALLIDA(S).");
        } else {
            System.out.println("Todas las verificaciones pasaron correctamente.");
        }
    }

    /** Cuenta cuantos rayos de la lista chocaron con la superficie dada. */
    private static long contarRayos(List<RayTracing.Rayo> rayos, String idSuperficie) {
        return rayos.stream()
                .filter(r -> r.impacto() && r.getSuperficieImpactada().getId().equals(idSuperficie))
                .count();
    }

    private static boolean estanConectados(Grafo grafo, String desde, String hacia) {
        return grafo.getVecinos(desde).stream()
                .anyMatch(arista -> arista.getDestino().equals(hacia));
    }

    /** Compara dos niveles en dB con una tolerancia absoluta. */
    private static boolean cercano(double obtenido, double esperado, double tolerancia) {
        return Math.abs(obtenido - esperado) <= tolerancia;
    }

    private static int verificar(String descripcion, boolean condicion) {
        System.out.println((condicion ? "[OK]   " : "[FALLO]") + " " + descripcion);
        return condicion ? 0 : 1;
    }
}