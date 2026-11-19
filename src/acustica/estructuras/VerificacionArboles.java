package acustica.estructuras;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Verificación de los tres árboles del proyecto: {@link ArbolAVL},
 * {@link ArbolRojinegro} y {@link ArbolBMas}.
 *
 * Cada árbol se somete a inserciones y eliminaciones aleatorias (semilla fija,
 * así la prueba se repite igual) contra {@link TreeSet}/{@link TreeMap} de
 * Java, que hacen de oráculo. En cada paso se comprueban tamaño, orden,
 * mínimo, máximo, búsqueda e invariantes; al final, rango y duplicados.
 *
 * Se ejecuta desde la raíz del proyecto con:
 *   javac -d out $(find src -name "*.java")
 *   java -cp out acustica.estructuras.VerificacionArboles
 *
 * Termina con código de salida 1 si alguna verificación falla.
 */
public class VerificacionArboles {

    private static final long SEMILLA = 46_2026L;
    private static final int OPERACIONES = 2_000;

    public static void main(String[] args) {
        int total = 0;
        int fallos = 0;

        System.out.println("=== AVL ===");
        total++; fallos += verificar("2000 inserciones/eliminaciones aleatorias coinciden con TreeSet",
                pruebaAleatoriaAvl());
        total++; fallos += verificar("árbol vacío: tamaño 0, mínimo/máximo null, enOrden vacío",
                vacioAvl());
        total++; fallos += verificar("no admite duplicados (insertar devuelve false)",
                duplicadosAvl());
        total++; fallos += verificar("un comparador null lanza IllegalArgumentException",
                lanzaIllegalArgument(() -> new ArbolAVL<Integer>(null)));

        System.out.println("\n=== Rojo-negro ===");
        total++; fallos += verificar("2000 inserciones/eliminaciones aleatorias coinciden con TreeSet",
                pruebaAleatoriaRojinegro());
        total++; fallos += verificar("árbol vacío: tamaño 0, mínimo/máximo null, enOrden vacío",
                vacioRojinegro());
        total++; fallos += verificar("no admite duplicados (insertar devuelve false)",
                duplicadosRojinegro());
        total++; fallos += verificar("un comparador null lanza IllegalArgumentException",
                lanzaIllegalArgument(() -> new ArbolRojinegro<Integer>(null)));

        System.out.println("\n=== B+ ===");
        for (int orden : new int[] {3, 4, 5}) {
            final int ordenPrueba = orden;
            total++; fallos += verificar("orden " + orden + ": 2000 inserciones/eliminaciones aleatorias coinciden con TreeMap",
                    pruebaAleatoriaBMas(ordenPrueba));
        }
        total++; fallos += verificar("árbol vacío: tamaño 0, mínimo/máximo null, rango vacío",
                vacioBMas());
        total++; fallos += verificar("clave duplicada: insertar devuelve false y conserva el valor anterior",
                duplicadosBMas());
        total++; fallos += verificar("un orden menor que 3 lanza IllegalArgumentException",
                lanzaIllegalArgument(() -> new ArbolBMas<Integer, Integer>(2)));
        total++; fallos += verificar("clave o valor null lanzan IllegalArgumentException",
                lanzaIllegalArgument(() -> new ArbolBMas<Integer, Integer>(4).insertar(null, 1))
                        && lanzaIllegalArgument(() -> new ArbolBMas<Integer, Integer>(4).insertar(1, null)));
        total++; fallos += verificar("rango con extremos null lanza y rango invertido devuelve vacío",
                lanzaIllegalArgument(() -> new ArbolBMas<Integer, Integer>(4).rango(null, 1))
                        && new ArbolBMas<Integer, Integer>(4).rango(5, 1).isEmpty());

        total++; fallos += verificar("insertar null lanza IllegalArgumentException en AVL y rojo-negro",
                lanzaIllegalArgument(() -> new ArbolAVL<Integer>(Integer::compareTo).insertar(null))
                        && lanzaIllegalArgument(() -> new ArbolRojinegro<Integer>(Integer::compareTo).insertar(null)));

        System.out.println("\n=== Resumen ===");
        System.out.println((total - fallos) + " / " + total + " verificaciones OK");
        if (fallos > 0) {
            System.out.println("HAY " + fallos + " VERIFICACION(ES) FALLIDA(S).");
        } else {
            System.out.println("Todas las verificaciones pasaron correctamente.");
        }
        System.exit(fallos == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------
    // AVL
    // ------------------------------------------------------------------

    private static boolean pruebaAleatoriaAvl() {
        ArbolAVL<Integer> arbol = new ArbolAVL<>(Integer::compareTo);
        TreeSet<Integer> esperado = new TreeSet<>();
        Random rnd = new Random(SEMILLA);
        List<Integer> ordenados = new ArrayList<>();

        for (int paso = 0; paso < OPERACIONES; paso++) {
            int dato = rnd.nextInt(400);
            if (esperado.isEmpty() || rnd.nextInt(100) < 55) {
                if (arbol.insertar(dato) != esperado.add(dato)) {
                    return false;
                }
            } else if (arbol.eliminar(dato) != esperado.remove(dato)) {
                return false;
            }
            if (!estadoAvl(arbol, esperado)) {
                return false;
            }
        }

        for (int limite = -10; limite < 410; limite += 7) {
            ordenados.clear();
            ordenados.addAll(esperado.tailSet(limite, false));
            if (arbol.contiene(limite) != esperado.contains(limite)
                    || !arbol.mayoresQue(limite).equals(ordenados)) {
                return false;
            }
        }
        return true;
    }

    private static boolean estadoAvl(ArbolAVL<Integer> arbol, TreeSet<Integer> esperado) {
        return arbol.tamano() == esperado.size()
                && arbol.estaVacio() == esperado.isEmpty()
                && arbol.estaBalanceado()
                && Objects.equals(arbol.minimo(), esperado.isEmpty() ? null : esperado.first())
                && Objects.equals(arbol.maximo(), esperado.isEmpty() ? null : esperado.last())
                && arbol.enOrden().equals(new ArrayList<>(esperado));
    }

    private static boolean vacioAvl() {
        ArbolAVL<Integer> arbol = new ArbolAVL<>(Integer::compareTo);
        return arbol.estaVacio()
                && arbol.tamano() == 0
                && arbol.altura() == 0
                && arbol.minimo() == null
                && arbol.maximo() == null
                && arbol.enOrden().isEmpty()
                && arbol.mayoresQue(0).isEmpty()
                && !arbol.contiene(1)
                && !arbol.eliminar(1)
                && arbol.estaBalanceado();
    }

    private static boolean duplicadosAvl() {
        ArbolAVL<Integer> arbol = new ArbolAVL<>(Integer::compareTo);
        arbol.insertar(5);
        return !arbol.insertar(5) && arbol.tamano() == 1 && arbol.contiene(5);
    }

    // ------------------------------------------------------------------
    // Rojo-negro
    // ------------------------------------------------------------------

    private static boolean pruebaAleatoriaRojinegro() {
        ArbolRojinegro<Integer> arbol = new ArbolRojinegro<>(Integer::compareTo);
        TreeSet<Integer> esperado = new TreeSet<>();
        Random rnd = new Random(SEMILLA + 1);

        for (int paso = 0; paso < OPERACIONES; paso++) {
            int dato = rnd.nextInt(400);
            if (esperado.isEmpty() || rnd.nextInt(100) < 55) {
                if (arbol.insertar(dato) != esperado.add(dato)) {
                    return false;
                }
            } else if (arbol.eliminar(dato) != esperado.remove(dato)) {
                return false;
            }
            if (!estadoRojinegro(arbol, esperado)) {
                return false;
            }
        }

        for (int limite = -10; limite < 410; limite += 7) {
            final int corte = limite;
            Integer primero = esperado.ceiling(corte);
            if (!Objects.equals(arbol.primeroQue(v -> v >= corte), primero)
                    || !arbol.tomarMientras(v -> v < corte)
                            .equals(new ArrayList<>(esperado.headSet(corte)))) {
                return false;
            }
        }
        return true;
    }

    private static boolean estadoRojinegro(ArbolRojinegro<Integer> arbol, TreeSet<Integer> esperado) {
        return arbol.tamano() == esperado.size()
                && arbol.estaVacio() == esperado.isEmpty()
                && arbol.cumpleInvariantes()
                && Objects.equals(arbol.minimo(), esperado.isEmpty() ? null : esperado.first())
                && Objects.equals(arbol.maximo(), esperado.isEmpty() ? null : esperado.last())
                && arbol.enOrden().equals(new ArrayList<>(esperado));
    }

    private static boolean vacioRojinegro() {
        ArbolRojinegro<Integer> arbol = new ArbolRojinegro<>(Integer::compareTo);
        return arbol.estaVacio()
                && arbol.tamano() == 0
                && arbol.altura() == 0
                && arbol.minimo() == null
                && arbol.maximo() == null
                && arbol.enOrden().isEmpty()
                && arbol.primeroQue(v -> true) == null
                && arbol.tomarMientras(v -> true).isEmpty()
                && !arbol.contiene(1)
                && !arbol.eliminar(1)
                && arbol.cumpleInvariantes();
    }

    private static boolean duplicadosRojinegro() {
        ArbolRojinegro<Integer> arbol = new ArbolRojinegro<>(Integer::compareTo);
        arbol.insertar(5);
        return !arbol.insertar(5) && arbol.tamano() == 1 && arbol.contiene(5);
    }

    // ------------------------------------------------------------------
    // B+
    // ------------------------------------------------------------------

    private static boolean pruebaAleatoriaBMas(int orden) {
        ArbolBMas<Integer, Integer> arbol = new ArbolBMas<>(orden);
        TreeMap<Integer, Integer> esperado = new TreeMap<>();
        Random rnd = new Random(SEMILLA + orden);
        List<Integer> valores;

        for (int paso = 0; paso < OPERACIONES; paso++) {
            int clave = rnd.nextInt(400);
            if (esperado.isEmpty() || rnd.nextInt(100) < 55) {
                boolean nueva = !esperado.containsKey(clave);
                if (nueva) {
                    esperado.put(clave, clave * 10);
                }
                if (arbol.insertar(clave, clave * 10) != nueva) {
                    return false;
                }
            } else if (arbol.eliminar(clave) != (esperado.remove(clave) != null)) {
                return false;
            }
            if (arbol.tamano() != esperado.size()
                    || arbol.estaVacio() != esperado.isEmpty()
                    || !arbol.cumpleInvariantes()
                    || !Objects.equals(arbol.minimo(), esperado.isEmpty() ? null : esperado.firstKey())
                    || !Objects.equals(arbol.maximo(), esperado.isEmpty() ? null : esperado.lastKey())) {
                return false;
            }
        }

        for (int desde = -20; desde < 420; desde += 37) {
            for (int hasta = desde - 5; hasta < 430; hasta += 53) {
                valores = desde > hasta
                        ? new ArrayList<>()
                        : new ArrayList<>(esperado.subMap(desde, true, hasta, true).values());
                if (!arbol.rango(desde, hasta).equals(valores)) {
                    return false;
                }
            }
        }
        for (int clave = -1; clave < 410; clave += 13) {
            if (!Objects.equals(arbol.buscar(clave), esperado.get(clave))) {
                return false;
            }
        }
        return true;
    }

    private static boolean vacioBMas() {
        ArbolBMas<Integer, String> arbol = new ArbolBMas<>(4);
        return arbol.estaVacio()
                && arbol.tamano() == 0
                && arbol.altura() == 0
                && arbol.minimo() == null
                && arbol.maximo() == null
                && arbol.buscar(1) == null
                && !arbol.eliminar(1)
                && arbol.rango(0, 10).isEmpty()
                && arbol.aNodoVisual(String::valueOf) == null
                && arbol.cumpleInvariantes();
    }

    private static boolean duplicadosBMas() {
        ArbolBMas<Integer, String> arbol = new ArbolBMas<>(4);
        arbol.insertar(7, "siete");
        return !arbol.insertar(7, "otra cosa")
                && arbol.tamano() == 1
                && "siete".equals(arbol.buscar(7));
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

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
