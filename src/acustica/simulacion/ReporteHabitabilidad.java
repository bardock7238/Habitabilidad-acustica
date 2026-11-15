
package acustica.simulacion;

import acustica.mejora.GeneradorSoluciones;
import acustica.mejora.PlanMejora;
import acustica.mejora.Solucion;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.ResultadoHabitabilidad;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reporte que usa los dos árboles:
 *
 * - Árbol AVL (IndiceHabitabilidad): cada nodo representa
 *   el resultado de una habitación, ordenado por exceso
 *   de ruido respecto a su límite acústico.
 *
 * - Árbol rojo-negro (PlanMejora): para cada habitación
 *   NO_HABITABLE, almacena soluciones ordenadas por costo.
 *
 * Las soluciones pueden ser:
 * - ESTIMADAS: no se han comprobado con el simulador.
 * - VERIFICADAS: se probaron mediante una simulación real.
 *
 * Una solución verificada no necesariamente cubre
 * todo el exceso acústico.
 */
public final class ReporteHabitabilidad {

    private static final int HORIZONTE_ANIOS = 10;
    private static final int SOLUCIONES_A_MOSTRAR = 3;

    private ReporteHabitabilidad() {
        // Utilidad estática: no instanciable.
    }

    /**
     * Versión anterior para mantener compatibilidad.
     *
     * Genera soluciones estimadas cuando no se
     * proporciona un simulador.
     */
    public static void imprimir(
            List<ResultadoHabitabilidad> resultados) {

        imprimir(resultados, null, null);
    }

    /**
     * Genera el reporte de habitabilidad.
     *
     * Cuando recibe simulador y horario,
     * verifica las mejoras de materiales
     * ejecutando nuevas simulaciones.
     */
    public static void imprimir(
            List<ResultadoHabitabilidad> resultados,
            Simulador simulador,
            String horario) {

        if ((simulador == null) != (horario == null)) {
            throw new IllegalArgumentException(
                    "Debes proporcionar simulador y horario juntos");
        }

        // ==========================================
        // 1. ÁRBOL AVL: HABITABILIDAD
        // ==========================================

        IndiceHabitabilidad indice =
                new IndiceHabitabilidad(resultados);

        // El AVL ordena de menor a mayor exceso.
        // Invertimos para mostrar el peor caso primero.
        List<ResultadoHabitabilidad> ordenados =
                new ArrayList<>(indice.ordenados());

        Collections.reverse(ordenados);

        System.out.println(
                "\n  [Arbol AVL] Edificio ordenado "
                        + "por exceso de ruido (peor caso primero)");

        System.out.println(
                "  Cada nodo es una habitacion: "
                        + "nivel recibido y exceso contra su limite.");

        System.out.println(
                "  En alto impacto el exceso es del vecino "
                        + "mas afectado; se muestra ese nivel.");

        System.out.println(
                "  'sin limite' y 'no evaluable' valen "
                        + "-infinito: no son el peor caso.");

        System.out.println(String.format(
                "  Habitaciones: %d | altura del arbol: %d "
                        + "| balanceado: %s",
                indice.tamano(),
                indice.altura(),
                indice.estaBalanceado() ? "si" : "NO"));

        int posicion = 1;

        for (ResultadoHabitabilidad r : ordenados) {

            String vecino = r.usaNivelDeVecino()
                    ? String.format(
                            " | vecino %5.1f dB",
                            r.getNivelFinal())
                    : "                  ";

            System.out.println(String.format(
                    "  %2d. %-28s recibido %5.1f dB%s "
                            + "| exceso %12s | %s",
                    posicion++,
                    r.getHabitacion().getNombre(),
                    r.getNivelRecibido(),
                    vecino,
                    r.etiquetaExceso(),
                    etiqueta(r.getEstado())));
        }

        // ==========================================
        // 2. ÁRBOL ROJO-NEGRO: SOLUCIONES
        // ==========================================

        System.out.println(
                "\n  [Arbol rojo-negro] Soluciones "
                        + "para las habitaciones NO habitables");

        boolean hayNoHabitables = false;

        for (ResultadoHabitabilidad r : ordenados) {

            PlanMejora plan;

            // Generar el plan según el contexto disponible.
            if (simulador != null) {

                // Verificar mejoras de materiales.
                plan = GeneradorSoluciones.generar(
                        r,
                        HORIZONTE_ANIOS,
                        simulador,
                        horario);

            } else {

                // Mantener compatibilidad:
                // propuestas todavía estimadas.
                plan = GeneradorSoluciones.generar(
                        r,
                        HORIZONTE_ANIOS);
            }

            // Las habitaciones que no necesitan
            // intervención no tienen plan de mejora.
            if (plan == null) {
                continue;
            }

            hayNoHabitables = true;

            // --------------------------------------
            // Información general del plan
            // --------------------------------------

            System.out.println(String.format(
                    "  %s: exceso de %.1f dB"
                            + " -> %d candidatas, %d verificadas"
                            + " (arbol %s)",
                    r.getHabitacion().getNombre(),
                    plan.getExcesoDb(),
                    plan.cantidad(),
                    plan.cantidadVerificadas(),
                    plan.arbolValido()
                            ? "valido"
                            : "INVALIDO"));

            // --------------------------------------
            // Mostrar soluciones por costo
            // --------------------------------------

            List<Solucion> todas = plan.todas();

            for (int i = 0;
                    i < Math.min(
                            SOLUCIONES_A_MOSTRAR,
                            todas.size());
                    i++) {

                Solucion solucion = todas.get(i);

                String estadoVerificacion =
                        solucion.isVerificada()
                                ? "[VERIFICADA]"
                                : "[ESTIMADA]";

                System.out.println(
                        "      " + estadoVerificacion
                                + " " + solucion);
            }

            // --------------------------------------
            // Buscar solución suficiente y verificada
            // --------------------------------------

            Solucion cubre =
                    plan.masBarataQueCubra();

            if (cubre != null) {

                System.out.println(
                        "      Solucion verificada "
                                + "que cubre el exceso: "
                                + cubre);

            } else {

                System.out.println(
                        "      Ninguna solucion verificada "
                                + "cubre el exceso por si sola.");
            }

            // --------------------------------------
            // Advertencia para modo estimado
            // --------------------------------------

            if (simulador == null) {

                System.out.println(
                        "      Nota: reporte en modo estimado; "
                                + "no se verificaron materiales.");
            }
        }

        // ==========================================
        // 3. RESULTADO GENERAL
        // ==========================================

        if (!hayNoHabitables) {

            System.out.println(
                    "  Todas las habitaciones son habitables: "
                            + "no hace falta buscar soluciones.");
        }
    }

    /**
     * Traduce el estado de habitabilidad
     * a una etiqueta legible para consola.
     */
    private static String etiqueta(
            EstadoHabitabilidad estado) {

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
