package acustica.mejora;

import acustica.dominio.Habitacion;
import acustica.dominio.Superficie;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.ResultadoHabitabilidad;

/**
 * Arma el PlanMejora de una habitación a partir de su resultado.
 *
 * Soluciones que propone (todas son ESTIMACIONES para comparar opciones,
 * no un cálculo exacto de la propagación; las que se comprueban con el
 * simulador están en ReubicadorPorPisos):
 *
 *  - MEJORAR_MATERIAL: cambiar el material de una superficie de la
 *    habitación por uno con más R_k. Como w = R_k - 10*log10(S/A), subir R_k
 *    en X dB sube la atenuación de ese tramo en X dB. Es una cota superior:
 *    solo vale entera si esa superficie está en el camino dominante.
 *  - AUMENTAR_ABSORCION: bajar el T60 del recinto. A = 0.16*V/T60, así que
 *    pasar de T60 a T60' suma 10*log10(T60/T60') dB de atenuación.
 *  - REDUCIR_EMISION: limitar la actividad o fuente que genera el ruido
 *    (horario, volumen). Cubre el exceso más un margen de seguridad.
 *
 * Largo plazo: cada solución se desgasta (dB que pierde por año) y tiene un
 * costo de mantenimiento. Mientras dure el margen sobre el exceso la
 * habitación sigue habitable; después hay que hacer mantenimiento.
 *
 * Los costos y los desgastes son valores de ejemplo en "unidades relativas":
 * el equipo debe ajustarlos (constantes de abajo).
 */
public class GeneradorSoluciones {

    /** Una mejora de material del catálogo. */
    private static class MejoraMaterial {
        final String nombre;
        final double deltaRk;                 // dB que suma
        final double costoPorM2;              // costo inicial
        final double degradacionDbPorAnio;    // dB que pierde cada año
        final double mantenimientoPorM2;      // costo de UN mantenimiento

        MejoraMaterial(String nombre, double deltaRk, double costoPorM2,
                double degradacionDbPorAnio, double mantenimientoPorM2) {
            this.nombre = nombre;
            this.deltaRk = deltaRk;
            this.costoPorM2 = costoPorM2;
            this.degradacionDbPorAnio = degradacionDbPorAnio;
            this.mantenimientoPorM2 = mantenimientoPorM2;
        }
    }

    // --- Constantes de ejemplo (ajustar) ---
    private static final MejoraMaterial[] CATALOGO = {
        new MejoraMaterial("doble capa de drywall con lana mineral", 8.0, 40.0, 0.5, 6.0),
        new MejoraMaterial("muro de concreto reforzado", 12.0, 90.0, 0.1, 2.0),
        new MejoraMaterial("vidrio laminado acústico", 6.0, 120.0, 0.3, 15.0),
    };
    private static final double FACTOR_T60 = 0.6;              // T60 nuevo = 60 % del actual
    private static final double COSTO_ABSORCION_POR_M3 = 8.0;
    private static final double DEGRADACION_ABSORCION = 0.15;  // dB/año (polvo, paneles caídos)
    private static final double MANTENIMIENTO_ABSORCION_POR_M3 = 1.5;
    private static final double COSTO_REDUCIR_EMISION = 500.0;
    private static final double MARGEN_EMISION_DB = 3.0;       // se limita 3 dB de más
    private static final double DEGRADACION_EMISION = 1.0;     // dB/año (se relaja el cumplimiento)
    private static final double MANTENIMIENTO_EMISION = 50.0;  // supervisión/ajuste

    private GeneradorSoluciones() {
        // Utilidad estática: no instanciable.
    }

    /** Igual que generar(resultado, horizonte) con el horizonte por defecto. */
    public static PlanMejora generar(ResultadoHabitabilidad resultado) {
        return generar(resultado, Solucion.HORIZONTE_POR_DEFECTO);
    }

    /**
     * @param horizonteAnios años a los que se calcula el costo total (con mantenimiento)
     * @return el plan con todas las candidatas, o null si la habitación ya es
     *         habitable o no se puede evaluar (no hay nada que mejorar)
     */
    public static PlanMejora generar(ResultadoHabitabilidad resultado, int horizonteAnios) {
        if (resultado == null) {
            throw new IllegalArgumentException("El resultado no puede ser null");
        }
        if (resultado.getEstado() != EstadoHabitabilidad.NO_HABITABLE) {
            return null;
        }
        Habitacion hab = resultado.getHabitacion();
        double exceso = resultado.getExceso();
        PlanMejora plan = new PlanMejora(hab.getId(), exceso);

        int n = 1;
        for (Superficie sup : hab.getSuperficies()) {
            for (MejoraMaterial mejora : CATALOGO) {
                plan.agregar(new Solucion(
                        hab.getId() + "-S" + n++,
                        Solucion.Tipo.MEJORAR_MATERIAL,
                        hab.getId(),
                        "Superficie " + sup.getId() + " (" + sup.getTipo() + "): " + mejora.nombre,
                        mejora.deltaRk,
                        mejora.costoPorM2 * sup.getArea(),
                        Solucion.aniosDeVida(mejora.deltaRk, exceso, mejora.degradacionDbPorAnio),
                        mejora.mantenimientoPorM2 * sup.getArea(),
                        horizonteAnios,
                        false));
            }
        }

        double t60Nuevo = hab.getT60() * FACTOR_T60;
        double reduccionAbs = 10 * Math.log10(hab.getT60() / t60Nuevo);
        plan.agregar(new Solucion(
                hab.getId() + "-ABS",
                Solucion.Tipo.AUMENTAR_ABSORCION,
                hab.getId(),
                String.format("Paneles absorbentes: T60 de %.2f s a %.2f s", hab.getT60(), t60Nuevo),
                reduccionAbs,
                COSTO_ABSORCION_POR_M3 * hab.calcularVolumen(),
                Solucion.aniosDeVida(reduccionAbs, exceso, DEGRADACION_ABSORCION),
                MANTENIMIENTO_ABSORCION_POR_M3 * hab.calcularVolumen(),
                horizonteAnios,
                false));

        double reduccionEmi = Math.max(exceso, 0.1) + MARGEN_EMISION_DB;
        plan.agregar(new Solucion(
                hab.getId() + "-EMI",
                Solucion.Tipo.REDUCIR_EMISION,
                hab.getId(),
                "Limitar volumen u horario de la fuente/actividad ruidosa",
                reduccionEmi,
                COSTO_REDUCIR_EMISION,
                Solucion.aniosDeVida(reduccionEmi, exceso, DEGRADACION_EMISION),
                MANTENIMIENTO_EMISION,
                horizonteAnios,
                false));

        return plan;
    }
}