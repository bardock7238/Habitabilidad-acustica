
package acustica.simulacion;

import acustica.mejora.ClasificadorDificultad;
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

    /** Umbral y rango de ejemplo para mostrar las consultas del índice B+. */
    private static final double NIVEL_ALTO_DB = 55.0;
    private static final double RANGO_DESDE_DB = 30.0;
    private static final double RANGO_HASTA_DB = 55.0;

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
        // 1b. ÁRBOL AVL POR PISOS
        // ==========================================

        IndiceHabitabilidadPorPisos porPisos =
                new IndiceHabitabilidadPorPisos(resultados);

        System.out.println(
                "\n  [Arbol AVL por pisos] Peor caso de cada piso");

        for (int piso : porPisos.pisos()) {

            ResultadoHabitabilidad peor =
                    porPisos.indiceDe(piso).peorCaso();

            System.out.println(String.format(
                    "  Piso %d: habitaciones %d | peor caso %-20s "
                            + "recibido %5.1f dB | exceso %12s | %s",
                    piso,
                    porPisos.indiceDe(piso).tamano(),
                    peor.getHabitacion().getNombre(),
                    peor.getNivelRecibido(),
                    peor.etiquetaExceso(),
                    etiqueta(peor.getEstado())));
        }

        Integer pisoCritico = porPisos.pisoMasCritico();

        if (pisoCritico != null) {

            System.out.println(
                    "  Piso mas critico: piso " + pisoCritico);
        }

        // ==========================================
        // 1c. ÁRBOL B+ POR PISO Y NIVEL
        // ==========================================

        IndicePorPisoYNivel porPisoYNivel =
                new IndicePorPisoYNivel(resultados);

        System.out.println(
                "\n  [Arbol B+ por piso y nivel] "
                        + "Clave (piso, nivel recibido, habitacion)");

        System.out.println(
                "  El nivel indexado es el recibido por el espacio, "
                        + "no el del vecino de una actividad de alto impacto.");

        System.out.println(String.format(
                "  Habitaciones: %d | altura del arbol: %d "
                        + "| valido: %s",
                porPisoYNivel.tamano(),
                porPisoYNivel.altura(),
                porPisoYNivel.cumpleInvariantes() ? "si" : "NO"));

        System.out.println(String.format(
                "  Consultas de ejemplo: superan %.1f dB | "
                        + "rango [%.1f, %.1f] dB",
                NIVEL_ALTO_DB,
                RANGO_DESDE_DB,
                RANGO_HASTA_DB));

        for (int piso : porPisoYNivel.pisos()) {

            List<ResultadoHabitabilidad> delPiso =
                    porPisoYNivel.habitacionesDePiso(piso);

            System.out.println(String.format(
                    "  Piso %d (%d habitaciones):",
                    piso,
                    delPiso.size()));

            System.out.println(
                    "      Por nivel: "
                            + resumenNiveles(delPiso));

            System.out.println(String.format(
                    "      Superan %.1f dB: %s",
                    NIVEL_ALTO_DB,
                    resumenNiveles(porPisoYNivel.queSuperanNivel(
                            piso, NIVEL_ALTO_DB))));

            System.out.println(String.format(
                    "      Entre %.1f y %.1f dB: %s",
                    RANGO_DESDE_DB,
                    RANGO_HASTA_DB,
                    resumenNiveles(porPisoYNivel.enRangoDeNiveles(
                            piso, RANGO_DESDE_DB, RANGO_HASTA_DB))));
        }

        // ==========================================
        // 2. ÁRBOL ROJO-NEGRO: SOLUCIONES
        // ==========================================

        System.out.println(
                "\n  [Arbol rojo-negro] Soluciones "
                        + "para las habitaciones NO habitables");

        boolean hayNoHabitables = false;
        // Árbol rojo-negro global de facilidad económica.
        // Se construye únicamente para este horario.
        ClasificadorDificultad clasificador =
                new ClasificadorDificultad();

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

            // Registrar la habitación en el árbol global.
                // Usa la mejor solución verificada que cubra el exceso.
                clasificador.registrar(r, plan);

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
        // 3. ÁRBOL ROJO-NEGRO GLOBAL:
        //    RANKING DE FACILIDAD ECONÓMICA
        // ==========================================

        System.out.println(
                "\n  [Arbol rojo-negro global] Ranking de facilidad");

        System.out.println(
                "  Ordenado por costo total de la solucion "
                        + "verificada que cubre el exceso.");

        System.out.println(
                "  Empates por ID. Sin solucion suficiente "
                        + "verificada: al final.");

        System.out.println(
                "  Este ranking no determina el orden "
                        + "recomendado de intervencion.");

        int puesto = 1;

        for (ClasificadorDificultad.Entrada entrada
                : clasificador.ranking()) {

            if (entrada.requiereCombinar()) {

                String observacion = entrada.tieneVerificadas()
                        ? "requiere combinar: ninguna verificada "
                                + "cubre el exceso"
                        : "requiere combinar / pendiente de "
                                + "verificacion";

                System.out.println(String.format(
                        "  %2d. %-28s | exceso %5.1f dB | %s",
                        puesto++,
                        entrada.getNombreHabitacion(),
                        entrada.getExcesoDb(),
                        observacion));

            } else {

                System.out.println(String.format(
                        "  %2d. %-28s | exceso %5.1f dB "
                                + "| costo total %.2f "
                                + "| solucion %s",
                        puesto++,
                        entrada.getNombreHabitacion(),
                        entrada.getExcesoDb(),
                        entrada.getCostoTotal(),
                        entrada.getMejorSolucion().getId()));
            }
        }

        System.out.println(String.format(
                "  Habitaciones en el ranking: %d "
                        + "| arbol valido: %s",
                clasificador.cantidad(),
                clasificador.arbolValido() ? "si" : "NO"));


        // ==========================================
        // 4. RESULTADO GENERAL
        // ==========================================

        if (!hayNoHabitables) {

            System.out.println(
                    "  Todas las habitaciones son habitables: "
                            + "no hace falta buscar soluciones.");
        }
    }

    /**
     * Lista compacta de habitaciones con su nivel recibido, para las consultas
     * del índice B+ por piso y nivel.
     */
    private static String resumenNiveles(
            List<ResultadoHabitabilidad> resultados) {

        if (resultados.isEmpty()) {
            return "ninguna";
        }

        StringBuilder texto = new StringBuilder();

        for (int i = 0; i < resultados.size(); i++) {

            if (i > 0) {
                texto.append(", ");
            }

            ResultadoHabitabilidad r = resultados.get(i);

            texto.append(String.format("%s (%.1f dB)",
                    r.getHabitacion().getNombre(),
                    r.getNivelRecibido()));
        }

        return texto.toString();
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
