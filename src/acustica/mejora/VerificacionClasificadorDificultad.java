package acustica.mejora;

import acustica.dominio.Habitacion;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.ResultadoHabitabilidad;
import java.util.List;

/** Pruebas independientes del simulador para el ranking del Issue #42. */
public final class VerificacionClasificadorDificultad {

    private static int aprobadas;

    private VerificacionClasificadorDificultad() { }

    public static void main(String[] args) {
        System.out.println("=== PRUEBAS CLASIFICADOR DIFICULTAD ===");

        ClasificadorDificultad clasificador = new ClasificadorDificultad();

        // B: 300 iniciales + 2 mantenimientos de 100 = 500 total.
        PlanMejora b = plan("B", 5.0);
        b.agregar(solucion("B-1", "B", 6.0, 300, 4, 100, true));
        clasificador.registrar(resultado("B", 5.0, false), b);

        // C y A empatan en 200: A debe ir primero por ID.
        PlanMejora c = plan("C", 5.0);
        c.agregar(solucion("C-1", "C", 5.0, 200, 10, 0, true));
        clasificador.registrar(resultado("C", 5.0, false), c);

        PlanMejora a = plan("A", 5.0);
        a.agregar(solucion("A-1", "A", 7.0, 200, 10, 0, true));
        // Una estimación más barata NO debe desplazar a la verificada.
        a.agregar(solucion("A-E", "A", 9.0, 50, 10, 0, false));
        clasificador.registrar(resultado("A", 5.0, false), a);

        // D tiene candidata verificada, pero no cubre el exceso.
        PlanMejora d = plan("D", 5.0);
        d.agregar(solucion("D-1", "D", 3.0, 100, 10, 0, true));
        clasificador.registrar(resultado("D", 5.0, false), d);

        // E no tiene ninguna solución verificada.
        PlanMejora e = plan("E", 5.0);
        e.agregar(solucion("E-1", "E", 8.0, 80, 10, 0, false));
        clasificador.registrar(resultado("E", 5.0, false), e);

        List<ClasificadorDificultad.Entrada> ranking = clasificador.ranking();

        comprobar("Orden por costo total",
                ranking.size() == 5
                        && id(ranking, 0).equals("A")
                        && id(ranking, 1).equals("C")
                        && id(ranking, 2).equals("B")
                        && ranking.get(2).getCostoTotal() == 500.0);

        comprobar("Desempate por ID",
                ranking.get(0).getCostoTotal() == ranking.get(1).getCostoTotal()
                        && id(ranking, 0).equals("A")
                        && id(ranking, 1).equals("C"));

        comprobar("Solo soluciones suficientes verificadas",
                ranking.get(0).getMejorSolucion().getId().equals("A-1")
                        && ranking.get(3).getMejorSolucion() == null);

        comprobar("Habitaciones sin solución al final",
                id(ranking, 3).equals("D")
                        && id(ranking, 4).equals("E")
                        && ranking.get(3).requiereCombinar()
                        && ranking.get(3).tieneVerificadas()
                        && ranking.get(4).requiereCombinar()
                        && !ranking.get(4).tieneVerificadas());

        PlanMejora habitable = plan("F", 0.0);
        clasificador.registrar(resultado("F", 0.0, true), habitable);
        comprobar("Solo habitaciones NO_HABITABLE",
                clasificador.cantidad() == 5);

        boolean detectoDuplicado = false;
        try {
            clasificador.registrar(resultado("A", 5.0, false), a);
        } catch (IllegalArgumentException esperado) {
            detectoDuplicado = true;
        }
        comprobar("No duplicar habitaciones",
                detectoDuplicado && clasificador.cantidad() == 5);

        comprobar("Invariantes del rojo-negro",
                clasificador.arbolValido()
                        && clasificador.masFacil() != null
                        && clasificador.masFacil().getHabitacionId().equals("A"));

        System.out.println("\nRESULTADO: " + aprobadas + "/7 pruebas superadas");
    }

    private static String id(List<ClasificadorDificultad.Entrada> lista, int indice) {
        return lista.get(indice).getHabitacionId();
    }

    private static PlanMejora plan(String id, double exceso) {
        return new PlanMejora(id, exceso);
    }

    private static ResultadoHabitabilidad resultado(String id, double exceso, boolean habitable) {
        Habitacion habitacion = new Habitacion(id, "Habitación " + id,
                "Dormitorio", 4, 4, 3);
        return ResultadoHabitabilidad.propio(habitacion, 50 + exceso, 50,
                habitable ? EstadoHabitabilidad.HABITABLE
                        : EstadoHabitabilidad.NO_HABITABLE);
    }

    private static Solucion solucion(String id, String habitacionId, double reduccion,
            double costo, double anios, double mantenimiento, boolean verificada) {
        return new Solucion(id, Solucion.Tipo.MEJORAR_MATERIAL,
                habitacionId, "Solución de prueba", reduccion, costo,
                anios, mantenimiento, 10, verificada);
    }

    private static void comprobar(String nombre, boolean condicion) {
        if (!condicion) {
            throw new AssertionError("[ERROR] " + nombre);
        }
        aprobadas++;
        System.out.println("[OK] " + nombre);
    }
}
