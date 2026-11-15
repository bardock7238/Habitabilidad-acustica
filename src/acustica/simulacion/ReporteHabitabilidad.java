package acustica.simulacion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import acustica.mejora.GeneradorSoluciones;
import acustica.mejora.PlanMejora;
import acustica.mejora.Solucion;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.ResultadoHabitabilidad;

/**
 * Reporte que usa los dos árboles:
 *
 *  - Árbol AVL (IndiceHabitabilidad): ordena TODAS las habitaciones del
 *    edificio por exceso de ruido y muestra si cada una es habitable o no.
 *  - Árbol rojo-negro (PlanMejora): para cada habitación NO habitable,
 *    guarda sus posibles soluciones ordenadas por costo.
 */
public final class ReporteHabitabilidad {

    private static final int HORIZONTE_ANIOS = 10;
    private static final int SOLUCIONES_A_MOSTRAR = 3;

    private ReporteHabitabilidad() {
        // Utilidad estática: no instanciable.
    }

    public static void imprimir(List<ResultadoHabitabilidad> resultados) {
        IndiceHabitabilidad indice = new IndiceHabitabilidad(resultados);

        // El AVL ordena de menor a mayor exceso; se muestra de más ruidosa a más silenciosa.
        List<ResultadoHabitabilidad> ordenados = new ArrayList<>(indice.ordenados());
        Collections.reverse(ordenados);

        System.out.println("\n  [Arbol AVL] Edificio ordenado por exceso de ruido (de mas a menos ruidosa)");
        System.out.println(String.format("  Habitaciones: %d | altura del arbol: %d | balanceado: %s",
                indice.tamano(), indice.altura(), indice.estaBalanceado() ? "si" : "NO"));

        int posicion = 1;
        for (ResultadoHabitabilidad r : ordenados) {
            String exceso = Double.isInfinite(r.getExceso())
                    ? "     -   "
                    : String.format("%+7.1f dB", r.getExceso());
            System.out.println(String.format("  %2d. %-28s nivel %5.1f dB | exceso %s | %s",
                    posicion++, r.getHabitacion().getNombre(), r.getNivelFinal(), exceso,
                    etiqueta(r.getEstado())));
        }

        // Rojo-negro: solo para las NO habitables
        System.out.println("\n  [Arbol rojo-negro] Soluciones para las habitaciones NO habitables");
        boolean hayNoHabitables = false;
        for (ResultadoHabitabilidad r : ordenados) {
            PlanMejora plan = GeneradorSoluciones.generar(r, HORIZONTE_ANIOS);
            if (plan == null) {
                continue; // habitable, tolerable o no evaluable: no hay nada que mejorar
            }
            hayNoHabitables = true;
            System.out.println(String.format("  %s: exceso de %.1f dB -> %d soluciones candidatas (arbol %s)",
                    r.getHabitacion().getNombre(), plan.getExcesoDb(), plan.cantidad(),
                    plan.arbolValido() ? "valido" : "INVALIDO"));

            List<Solucion> todas = plan.todas();
            for (int i = 0; i < Math.min(SOLUCIONES_A_MOSTRAR, todas.size()); i++) {
                System.out.println("      " + (i == 0 ? "Mas barata:   " : "Siguiente:    ") + todas.get(i));
            }
            Solucion cubre = plan.masBarataQueCubra();
            System.out.println("      Cumple el limite con una sola: "
                    + (cubre != null ? cubre : "ninguna (habria que combinar varias)"));
        }
        if (!hayNoHabitables) {
            System.out.println("  Todas las habitaciones son habitables: no hace falta buscar soluciones.");
        }
    }

    private static String etiqueta(EstadoHabitabilidad estado) {
        switch (estado) {
            case HABITABLE:
                return "[OK] HABITABLE";
            case TOLERABLE:
                return "[~]  TOLERABLE";
            case NO_HABITABLE:
                return "[X]  NO HABITABLE";
            default:
                return "[?]  NO EVALUABLE";
        }
    }
}
