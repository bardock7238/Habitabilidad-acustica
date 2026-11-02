package acustica.simulacion.propagacion;

import acustica.dominio.Edificio;
import acustica.dominio.FuenteSonido;
import acustica.dominio.Habitacion;
import acustica.dominio.Material;
import acustica.dominio.Superficie;
import acustica.normativa.Normativa;

import java.util.ArrayList;
import java.util.List;

/**
 * Casos de prueba de la Tarea 4.3 (Ray tracing), actualizados al modelo v2
 * descrito en docs/modelo-matematico.md.
 *
 * Modelo que se verifica:
 *
 *   1. Ray tracing en la habitación de origen:
 *        - Se emiten N rayos en 360°; cada superficie recibe una fracción
 *          proporcional a su área.
 *        - Cada rayo pierde solo R_k (índice de reducción sonora del material),
 *          nunca S_k · R_k.
 *        - El nivel se PROMEDIA (no se suma):
 *            L = 10·log10( (1/N) · Σ 10^(L_i/10) )
 *
 *   2. Propagación a otra habitación (Grafo + Dijkstra):
 *        - Peso de la arista i -> j:
 *            w = max(0, R_k - 10·log10(S_k / A_j))
 *          donde A_j = 0.16·V_j/T60 es la absorción Sabine del recinto receptor.
 *        - Nivel en el destino: L_j = L_s - Σ w.
 *
 * Los valores esperados están calculados a mano en
 * docs/casos-de-prueba-raytracing.md.
 *
 * Se ejecuta con:
 *   javac -d out $(find src -name "*.java")
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
        Material concreto = new Material("Concreto", 30.0, 0.15);
        Material vidrio = new Material("Vidrio", 15.0, 0.01);
        Material ladrillo = new Material("Ladrillo hueco", 20.0, 0.12);
        Material madera = new Material("Madera", 25.0, 0.10);

        Habitacion sala = new Habitacion("S1", "Sala", "Sala", 4, 3, 3);
        Habitacion dormitorio = new Habitacion("D1", "Dormitorio", "Dormitorio", 3, 4, 3);

        // La sala tiene 4 superficies de 10 m² (total 40 m²) y R_k = 30, 15, 20 y 25 dB.
        Superficie medianera = new Superficie("M1", "pared interior", 10.0, concreto, sala, dormitorio);
        Superficie fachadaSala = new Superficie("F1", "fachada", 10.0, vidrio, sala);
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

        // Area total = 40 m² -> 40 rayos; cada superficie recibe 10.
        int n1 = 40;
        List<RayTracing.Rayo> rayos1 = RayTracing.emitirRayos(fuenteSala, sala, n1);
        double nivelSala = CalculoAcustico.calcularNivelHabitacion(rayos1);

        // Con el promedio por N, el resultado debe ser el mismo con 80 rayos.
        double nivelSala80 = CalculoAcustico.calcularNivelHabitacion(
                RayTracing.emitirRayos(fuenteSala, sala, 80));

        Grafo grafo1 = Grafo.desdeEdificio(edificio1);
        double nivelDorm1 = CalculoAcustico.calcularNivelEnDestino(fuenteSala, "S1", "D1", grafo1);

        System.out.println("=== Escenario 1: " + edificio1.getNombre() + " ===");
        System.out.println("Reparto de rayos esperado: 10 en cada superficie (N=40, cortes a 90/180/270 grados)");
        System.out.printf("Nivel Sala (ray tracing)    = %.2f dB  (esperado 60.59 dB)%n", nivelSala);
        System.out.printf("Nivel Dormitorio (Dijkstra) = %.2f dB  (esperado 49.39 dB)%n%n", nivelDorm1);

        total++; fallos += verificar("La medianera recibe 10 rayos a 50 dB",
                rayosConNivel(rayos1, "M1", 10, 50.0));
        total++; fallos += verificar("La fachada recibe 10 rayos a 65 dB",
                rayosConNivel(rayos1, "F1", 10, 65.0));
        total++; fallos += verificar("El piso recibe 10 rayos a 60 dB",
                rayosConNivel(rayos1, "P1", 10, 60.0));
        total++; fallos += verificar("El techo recibe 10 rayos a 55 dB",
                rayosConNivel(rayos1, "T1", 10, 55.0));
        total++; fallos += verificar("Nivel de la Sala = 60.59 dB (promedio energetico de los 40 rayos)",
                cercano(nivelSala, 60.59, TOLERANCIA_DB));
        total++; fallos += verificar("Nivel del Dormitorio = 49.39 dB (80 - 30.61 de la medianera)",
                cercano(nivelDorm1, 49.39, TOLERANCIA_DB));
        total++; fallos += verificar("El nivel por ray tracing no depende de N (N=40 vs N=80)",
                cercano(nivelSala, nivelSala80, TOLERANCIA_DB));

        // Caso borde: habitación sin superficies. El promedio debe devolver el
        // nivel de la fuente, no la suma de N copias (80 + 10·log10(N)).
        Habitacion vacia = new Habitacion("V1", "Vacia", "Sala", 4, 3, 3);
        double nivelVacia = CalculoAcustico.calcularNivelHabitacion(
                RayTracing.emitirRayos(fuenteSala, vacia, 40));
        total++; fallos += verificar("Habitacion sin superficies: el nivel es el de la fuente (80 dB)",
                cercano(nivelVacia, 80.0, TOLERANCIA_DB));

        // ============================================================
        // Escenario 2: edificio con pasillo
        // ============================================================
        Material muroPesado = new Material("Muro pesado", 36.0, 0.15);
        Material puerta = new Material("Puerta", 12.0, 0.05);

        Habitacion oficina1 = new Habitacion("H1", "Oficina 1", "Oficina", 4, 3, 3);
        Habitacion pasillo = new Habitacion("P1", "Pasillo", "Pasillo", 2, 6, 3);
        Habitacion oficina2 = new Habitacion("H2", "Oficina 2", "Oficina", 4, 3, 3);

        // La puerta (4 m²) tiene R_k = 12 dB; las paredes/piso/techo (12 m²) R_k = 36 dB.
        Superficie puerta1 = new Superficie("PU1", "puerta", 4.0, puerta, oficina1, pasillo);
        Superficie puerta2 = new Superficie("PU2", "puerta", 4.0, puerta, pasillo, oficina2);
        Superficie fachadaH1 = new Superficie("F1", "fachada", 12.0, muroPesado, oficina1);
        Superficie pisoH1 = new Superficie("P1", "piso", 12.0, muroPesado, oficina1);
        Superficie techoH1 = new Superficie("T1", "techo", 12.0, muroPesado, oficina1);

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

        // Area total de la Oficina 1 = 4 + 12 + 12 + 12 = 40 m² -> 40 rayos.
        int n2 = 40;
        List<RayTracing.Rayo> rayos2 = RayTracing.emitirRayos(fuenteO1, oficina1, n2);
        double nivelH1 = CalculoAcustico.calcularNivelHabitacion(rayos2);

        Grafo grafo2 = Grafo.desdeEdificio(edificio2);
        double nivelH2 = CalculoAcustico.calcularNivelEnDestino(fuenteO1, "H1", "H2", grafo2);
        Dijkstra.Resultado r2 = Dijkstra.calcular(grafo2, "H1");
        List<String> camino2 = r2.reconstruirCamino("H2");

        System.out.println("=== Escenario 2: " + edificio2.getNombre() + " ===");
        System.out.println("Reparto de rayos esperado: puerta 4 (68 dB), fachada/piso/techo 12 cada uno (44 dB)");
        System.out.printf("Nivel Oficina 1 (ray tracing) = %.2f dB  (esperado 58.15 dB)%n", nivelH1);
        System.out.printf("Nivel Oficina 2 (Dijkstra)    = %.2f dB  (esperado 46.81 dB)%n%n", nivelH2);

        total++; fallos += verificar("La puerta del pasillo recibe 4 rayos a 68 dB",
                rayosConNivel(rayos2, "PU1", 4, 68.0));
        total++; fallos += verificar("La fachada recibe 12 rayos a 44 dB",
                rayosConNivel(rayos2, "F1", 12, 44.0));
        total++; fallos += verificar("El piso recibe 12 rayos a 44 dB",
                rayosConNivel(rayos2, "P1", 12, 44.0));
        total++; fallos += verificar("El techo recibe 12 rayos a 44 dB",
                rayosConNivel(rayos2, "T1", 12, 44.0));
        total++; fallos += verificar("Nivel de la Oficina 1 = 58.15 dB (promedio de 4 rayos a 68 y 36 a 44)",
                cercano(nivelH1, 58.15, TOLERANCIA_DB));
        total++; fallos += verificar("El camino H1 -> H2 pasa por el pasillo",
                camino2.contains("P1"));
        total++; fallos += verificar("Nivel de la Oficina 2 = 46.81 dB (80 - 16.59 - 16.59)",
                cercano(nivelH2, 46.81, TOLERANCIA_DB));

        // ============================================================
        // Escenario 3: edificio con fachada a una via
        // ============================================================
        Material fachadaAislante = new Material("Fachada aislante", 30.0, 0.10);
        Material paredInterior = new Material("Pared interior", 30.0, 0.15);

        Habitacion salaVia = new Habitacion("S1", "Sala", "Sala", 5, 4, 3);
        Habitacion dormitorioInt = new Habitacion("D1", "Dormitorio", "Dormitorio", 3, 4, 3);

        Superficie fachadaVia = new Superficie("FV1", "fachada a la via", 20.0, fachadaAislante, salaVia);
        Superficie paredInt = new Superficie("PD1", "pared interior", 10.0, paredInterior, salaVia, dormitorioInt);

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
        System.out.printf("Nivel Sala (via la fachada)          = %.2f dB  (esperado 55.18 dB)%n", nivelSalaVia);
        System.out.printf("Nivel Dormitorio (via fachada+pared) = %.2f dB  (esperado 24.56 dB)%n%n", nivelDormInt);

        total++; fallos += verificar("La Sala esta conectada a EXTERIOR (fachada a la via)",
                estanConectados(grafo3, "S1", Grafo.EXTERIOR));
        total++; fallos += verificar("El Dormitorio NO esta conectado a EXTERIOR directamente",
                !estanConectados(grafo3, "D1", Grafo.EXTERIOR));
        total++; fallos += verificar("Nivel de la Sala = 55.18 dB (85 - 29.82 de la fachada)",
                cercano(nivelSalaVia, 55.18, TOLERANCIA_DB));
        total++; fallos += verificar("Nivel del Dormitorio = 24.56 dB (85 - 29.82 fachada - 30.61 pared)",
                cercano(nivelDormInt, 24.56, TOLERANCIA_DB));
        total++; fallos += verificar("Sala de dia NO cumple la normativa (55.18 > 35)",
                !salaCumpleDia);
        total++; fallos += verificar("Dormitorio de noche cumple la normativa (24.56 <= 30)",
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

    /**
     * Comprueba que una superficie recibe exactamente la cantidad de rayos
     * esperada y que todos llegan con el nivel esperado.
     */
    private static boolean rayosConNivel(List<RayTracing.Rayo> rayos, String idSuperficie,
            int cantidadEsperada, double nivelEsperado) {
        List<RayTracing.Rayo> filtrados = new ArrayList<>();
        for (RayTracing.Rayo rayo : rayos) {
            if (rayo.impacto() && rayo.getSuperficieImpactada().getId().equals(idSuperficie)) {
                filtrados.add(rayo);
            }
        }
        if (filtrados.size() != cantidadEsperada) {
            return false;
        }
        for (RayTracing.Rayo rayo : filtrados) {
            double nivel = rayo.getNivelInicial() - rayo.getSuperficieImpactada().calcularAtenuacion();
            if (Math.abs(nivel - nivelEsperado) > TOLERANCIA_DB) {
                return false;
            }
        }
        return true;
    }

    /** Comprueba si existe una arista directa (no alcanzabilidad transitiva). */
    private static boolean estanConectados(Grafo grafo, String desde, String hacia) {
        return grafo.getVecinos(desde).stream()
                .anyMatch(arista -> arista.getDestino().equals(hacia));
    }

    /** Compara dos niveles en dB con una tolerancia absoluta. */
    private static boolean cercano(double obtenido, double esperado, double tolerancia) {
        return Math.abs(obtenido - esperado) <= tolerancia;
    }

    private static int verificar(String descripcion, boolean condicion) {
        System.out.println((condicion ? "[OK]   " : "[FALLO]") + descripcion);
        return condicion ? 0 : 1;
    }
}
