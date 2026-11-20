package acustica.simulacion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import acustica.dominio.Habitacion;
import acustica.estructuras.NodoVisual;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.ResultadoHabitabilidad;

/**
 * Verificación del índice B+ por piso y nivel.
 *
 * Comprueba la construcción, el orden de las consultas, los límites
 * inclusivos/exclusivos, el desempate por id y que se indexe
 * {@code nivelRecibido} y no el nivel del vecino de una actividad de alto
 * impacto.
 *
 * Se ejecuta desde la raíz del proyecto con:
 *   javac -d out $(find src -name "*.java")
 *   java -cp out acustica.simulacion.VerificacionIndicePorPisoYNivel
 *
 * Termina con código de salida 1 si alguna verificación falla.
 */
public class VerificacionIndicePorPisoYNivel {

    public static void main(String[] args) {
        int total = 0;
        int fallos = 0;

        List<ResultadoHabitabilidad> base = base();

        total++; fallos += verificar("construcción: tamaño, altura e invariantes del B+",
                construccion(base));
        total++; fallos += verificar("pisos: devuelve 1, 2 y 3 en orden",
                new ArrayList<>(new IndicePorPisoYNivel(base).pisos())
                        .equals(Arrays.asList(1, 2, 3)));
        total++; fallos += verificar("piso 1: todas las habitaciones por nivel y desempate por id",
                ids(new IndicePorPisoYNivel(base).habitacionesDePiso(1))
                        .equals(Arrays.asList("A", "B", "C")));
        total++; fallos += verificar("piso 2: todas las habitaciones por nivel",
                ids(new IndicePorPisoYNivel(base).habitacionesDePiso(2))
                        .equals(Arrays.asList("D", "E", "F")));
        total++; fallos += verificar("piso 3: todas las habitaciones por nivel",
                ids(new IndicePorPisoYNivel(base).habitacionesDePiso(3))
                        .equals(Arrays.asList("G")));
        total++; fallos += verificar("piso inexistente: consulta vacía",
                new IndicePorPisoYNivel(base).habitacionesDePiso(99).isEmpty());

        IndicePorPisoYNivel indice = new IndicePorPisoYNivel(base);

        total++; fallos += verificar("superar un nivel es estricto: 50 dB no incluye a C (50 dB)",
                indice.queSuperanNivel(1, 50.0).isEmpty()
                        && ids(indice.queSuperanNivel(1, 49.9)).equals(Arrays.asList("C")));
        total++; fallos += verificar("superar un nivel estricto en piso 2",
                ids(indice.queSuperanNivel(2, 40.0)).equals(Arrays.asList("E", "F"))
                        && ids(indice.queSuperanNivel(2, 59.9)).equals(Arrays.asList("E", "F")));
        total++; fallos += verificar("rango de niveles inclusivo en ambos extremos",
                ids(indice.enRangoDeNiveles(1, 30.0, 50.0)).equals(Arrays.asList("A", "B", "C"))
                        && ids(indice.enRangoDeNiveles(1, 30.0, 49.9)).equals(Arrays.asList("A", "B"))
                        && ids(indice.enRangoDeNiveles(1, 50.0, 50.0)).equals(Arrays.asList("C")));
        total++; fallos += verificar("rango global: ordena por piso y luego por nivel",
                ids(indice.enRangoDeNiveles(35.0, 60.0))
                        .equals(Arrays.asList("C", "D", "E", "F", "G")));
        total++; fallos += verificar("superar un nivel en todo el edificio",
                ids(indice.queSuperanNivel(50.0)).equals(Arrays.asList("E", "F")));
        total++; fallos += verificar("rango invertido o con NaN devuelve vacío",
                indice.enRangoDeNiveles(1, 50.0, 30.0).isEmpty()
                        && indice.enRangoDeNiveles(1, Double.NaN, 50.0).isEmpty());

        total++; fallos += verificar("indexa nivelRecibido y no el nivel del vecino",
                usaNivelRecibido());

        total++; fallos += verificar("índice vacío: sin datos, sin pisos y sin dibujo",
                vacio());
        total++; fallos += verificar("el B+ se puede copiar para dibujar (nodo raíz con hijos)",
                dibujable(base));
        total++; fallos += verificar("habitación repetida lanza IllegalArgumentException",
                lanzaIllegalArgument(() -> {
                    List<ResultadoHabitabilidad> repetidos = new ArrayList<>();
                    ResultadoHabitabilidad r = crear("A", 1, 30.0);
                    repetidos.add(r);
                    repetidos.add(r);
                    new IndicePorPisoYNivel(repetidos);
                }));
        total++; fallos += verificar("lista null lanza IllegalArgumentException",
                lanzaIllegalArgument(() -> new IndicePorPisoYNivel(null)));

        System.out.println("\n=== Resumen ===");
        System.out.println("VerificacionIndicePorPisoYNivel: "
                + (total - fallos) + " / " + total + " verificaciones OK");
        if (fallos > 0) {
            System.out.println("HAY " + fallos + " VERIFICACION(ES) FALLIDA(S).");
        } else {
            System.out.println("Todas las verificaciones pasaron correctamente.");
        }
        System.exit(fallos == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------
    // Datos de prueba
    // ------------------------------------------------------------------

    private static List<ResultadoHabitabilidad> base() {
        return new ArrayList<>(Arrays.asList(
                crear("A", 1, 30.0),
                crear("B", 1, 30.0), // mismo piso y nivel que A: desempata el id
                crear("C", 1, 50.0),
                crear("D", 2, 40.0),
                crear("E", 2, 60.0),
                crear("F", 2, 60.0), // mismo piso y nivel que E
                crear("G", 3, 45.0)));
    }

    private static ResultadoHabitabilidad crear(String id, int piso, double nivel) {
        Habitacion habitacion = new Habitacion(id, "Habitacion " + id, "Dormitorio", 3, 3, 2.5);
        habitacion.setPiso(piso);
        return ResultadoHabitabilidad.propio(habitacion, nivel, 30.0,
                EstadoHabitabilidad.NO_HABITABLE);
    }

    private static boolean construccion(List<ResultadoHabitabilidad> base) {
        IndicePorPisoYNivel indice = new IndicePorPisoYNivel(base);
        return indice.tamano() == 7
                && !indice.estaVacio()
                && indice.altura() >= 2
                && indice.cumpleInvariantes();
    }

    private static boolean usaNivelRecibido() {
        Habitacion habitacion = new Habitacion("V", "Habitacion V", "Sala", 4, 4, 2.5);
        habitacion.setPiso(1);
        // nivelRecibido = 40, nivelFinal (vecino) = 80.
        ResultadoHabitabilidad conVecino = ResultadoHabitabilidad.conVecino(
                habitacion, 40.0, 80.0, 30.0, EstadoHabitabilidad.NO_HABITABLE);

        List<ResultadoHabitabilidad> resultados = new ArrayList<>();
        resultados.add(conVecino);
        IndicePorPisoYNivel indice = new IndicePorPisoYNivel(resultados);

        return ids(indice.enRangoDeNiveles(1, 39.0, 41.0)).equals(Arrays.asList("V"))
                && indice.queSuperanNivel(1, 70.0).isEmpty();
    }

    private static boolean vacio() {
        IndicePorPisoYNivel indice = new IndicePorPisoYNivel(new ArrayList<>());
        return indice.tamano() == 0
                && indice.altura() == 0
                && indice.estaVacio()
                && indice.cumpleInvariantes()
                && indice.pisos().isEmpty()
                && indice.habitacionesDePiso(1).isEmpty()
                && indice.queSuperanNivel(1, 0.0).isEmpty()
                && indice.enRangoDeNiveles(1, 0.0, 100.0).isEmpty()
                && indice.aNodoVisual() == null;
    }

    private static boolean dibujable(List<ResultadoHabitabilidad> base) {
        NodoVisual raiz = new IndicePorPisoYNivel(base).aNodoVisual();
        return raiz != null
                && raiz.getLinea1() != null
                && raiz.getLinea1().contains("P1")
                && raiz.getHijos().size() >= 2;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static List<String> ids(List<ResultadoHabitabilidad> resultados) {
        List<String> ids = new ArrayList<>();
        for (ResultadoHabitabilidad r : resultados) {
            ids.add(r.getHabitacion().getId());
        }
        return ids;
    }

    private static boolean lanzaIllegalArgument(Runnable accion) {
        try {
            accion.run();
            return false;
        } catch (IllegalArgumentException esperado) {
            return true;
        }
    }

    private static int verificar(String descripcion, boolean condicion) {
        System.out.println((condicion ? "[OK]   " : "[FALLO]") + " " + descripcion);
        return condicion ? 0 : 1;
    }
}
