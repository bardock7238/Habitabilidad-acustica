package acustica.mejora;

import acustica.dominio.Actividad;
import acustica.dominio.FuenteSonido;
import acustica.dominio.Habitacion;
import acustica.dominio.Material;
import acustica.dominio.Superficie;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.ResultadoHabitabilidad;
import acustica.simulacion.Simulador;
import acustica.simulacion.propagacion.Dijkstra;
import acustica.simulacion.propagacion.Grafo;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


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
        new MejoraMaterial("refuerzo constructivo de muro o losa", 12.0, 90.0, 0.1, 2.0),
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

    
    /**
     * Genera soluciones estimadas.
     * Conserva la compatibilidad con el código existente.
     */
    public static PlanMejora generar(
            ResultadoHabitabilidad resultado,
            int horizonteAnios) {

        return generar(resultado, horizonteAnios, null, null);
    }

    /**
     * Genera un plan de mejora.
     *
     * El simulador y el horario permitirán verificar
     * las soluciones mediante nuevas simulaciones.
     */
    public static PlanMejora generar(
            ResultadoHabitabilidad resultado,
            int horizonteAnios,
            Simulador simulador,
            String horario) {

        if ((simulador == null) != (horario == null)) {
            throw new IllegalArgumentException(
                    "Debes proporcionar simulador y horario juntos");
        }

        if (resultado == null) {
            throw new IllegalArgumentException(
                    "El resultado no puede ser null");
        }

        if (resultado.getEstado() != EstadoHabitabilidad.NO_HABITABLE) {
            return null;
        }

        Habitacion hab = resultado.getHabitacion();
        double exceso = resultado.getExceso();
        PlanMejora plan = new PlanMejora(hab.getId(), exceso);

        
        int n = 1;

        if (simulador == null) {
            // Mantener compatibilidad con las llamadas antiguas.
            // Estas propuestas siguen siendo estimaciones.

            for (Superficie sup : hab.getSuperficies()) {
                for (MejoraMaterial mejora : CATALOGO) {
                    plan.agregar(new Solucion(
                            hab.getId() + "-S" + n++,
                            Solucion.Tipo.MEJORAR_MATERIAL,
                            hab.getId(),
                            "Superficie " + sup.getId()
                                    + " (" + sup.getTipo() + "): "
                                    + mejora.nombre,
                            mejora.deltaRk,
                            mejora.costoPorM2 * sup.getArea(),
                            Solucion.aniosDeVida(
                                    mejora.deltaRk,
                                    exceso,
                                    mejora.degradacionDbPorAnio),
                            mejora.mantenimientoPorM2
                                    * sup.getArea(),
                            horizonteAnios,
                            false));
                }
            }

        } else {
            // Nueva versión: usar únicamente las superficies
            // del camino dominante y verificar cada mejora.

            Set<String> candidatas =
                    superficiesDelCaminoDominante(
                            resultado, simulador, horario);

            // Una superficie interior pertenece a dos habitaciones.
            // Evitamos probarla dos veces.
            Set<String> procesadas = new LinkedHashSet<>();

            for (Habitacion recinto :
                    simulador.getEdificio().getHabitaciones()) {

                for (Superficie sup : recinto.getSuperficies()) {

                    if (!candidatas.contains(sup.getId())) {
                        continue;
                    }

                    if (!procesadas.add(sup.getId())) {
                        continue;
                    }

                    for (MejoraMaterial mejora : CATALOGO) {

                        if (!compatible(sup, mejora)) {
                            continue;
                        }

                        probarYAgregarMaterial(
                                plan,
                                resultado,
                                simulador,
                                horario,
                                sup,
                                mejora,
                                hab.getId() + "-S" + n++,
                                horizonteAnios);
                    }
                }
            }
        }

        double t60Nuevo = hab.getT60() * FACTOR_T60;
        double reduccionAbs =
                10 * Math.log10(hab.getT60() / t60Nuevo);

        plan.agregar(new Solucion(
                hab.getId() + "-ABS",
                Solucion.Tipo.AUMENTAR_ABSORCION,
                hab.getId(),
                String.format(
                        "Paneles absorbentes: T60 de %.2f s a %.2f s",
                        hab.getT60(), t60Nuevo),
                reduccionAbs,
                COSTO_ABSORCION_POR_M3 * hab.calcularVolumen(),
                Solucion.aniosDeVida(
                        reduccionAbs,
                        exceso,
                        DEGRADACION_ABSORCION),
                MANTENIMIENTO_ABSORCION_POR_M3
                        * hab.calcularVolumen(),
                horizonteAnios,
                false));

        double reduccionEmi =
                Math.max(exceso, 0.1) + MARGEN_EMISION_DB;

        plan.agregar(new Solucion(
                hab.getId() + "-EMI",
                Solucion.Tipo.REDUCIR_EMISION,
                hab.getId(),
                "Limitar volumen u horario de la fuente/actividad ruidosa",
                reduccionEmi,
                COSTO_REDUCIR_EMISION,
                Solucion.aniosDeVida(
                        reduccionEmi,
                        exceso,
                        DEGRADACION_EMISION),
                MANTENIMIENTO_EMISION,
                horizonteAnios,
                false));

        return plan;
    }


    /**
     * Prueba una mejora de aislamiento sin modificar
     * permanentemente el edificio.
     *
     * Devuelve los resultados obtenidos durante la prueba.
     * Después restaura el material y el estado del simulador.
     */
    private static List<ResultadoHabitabilidad> simularMejoraMaterial(
            Simulador simulador,
            String horario,
            Superficie superficie,
            double deltaRk) {

        if (simulador == null || horario == null
                || superficie == null || deltaRk <= 0) {
            throw new IllegalArgumentException(
                    "Parametros invalidos para simular la mejora");
        }

        // 1. Guardar el material original.
        Material original = superficie.getMaterial();

        // 2. Crear un material con mayor aislamiento.
        Material mejorado = new Material(
                original.getNombre() + " (mejorado)",
                original.getIndiceReduccionSonora() + deltaRk,
                original.getEspesor()
        );

        try {
            // 3. Aplicar temporalmente la mejora.
            superficie.setMaterial(mejorado);

            // 4. Ejecutar la simulación con el material mejorado.
            return List.copyOf(simulador.ejecutar(horario));

        } finally {
            // 5. Restaurar siempre el material original.
            superficie.setMaterial(original);

            // 6. Recuperar el estado inicial del simulador.
            simulador.ejecutar(horario);
        }
    }

    /**
     * Busca el resultado de una habitación
     * dentro de una simulación.
     */
    private static ResultadoHabitabilidad buscarResultado(
            List<ResultadoHabitabilidad> resultados,
            String habitacionId) {

        for (ResultadoHabitabilidad r : resultados) {
            if (r.getHabitacion().getId().equals(habitacionId)) {
                return r;
            }
        }

        throw new IllegalStateException(
                "No se encontró el resultado de " + habitacionId);
    }

    /**
     * Mide cuánto disminuyó el exceso acústico
     * después de aplicar una mejora.
     *
     * Devuelve 0 si no hubo una mejora significativa.
     */
    private static double medirReduccionReal(
            ResultadoHabitabilidad original,
            List<ResultadoHabitabilidad> simulados) {

        ResultadoHabitabilidad actualizado =
                buscarResultado(
                        simulados,
                        original.getHabitacion().getId());

        double excesoAntes = original.getExceso();
        double excesoDespues = actualizado.getExceso();

        if (!Double.isFinite(excesoAntes)
                || !Double.isFinite(excesoDespues)) {
            return 0.0;
        }

        double reduccion = excesoAntes - excesoDespues;

        // Evitar considerar como mejora
        // diferencias mínimas por precisión decimal.
        return reduccion > 0.000001 ? reduccion : 0.0;
    }


    /**
     * Prueba una mejora de material mediante una nueva simulacion.
     *
     * Solo agrega la solucion al arbol rojo-negro si
     * disminuye realmente el exceso acustico.
     *
     * @return true si la solucion se agrego al plan.
     */
    private static boolean probarYAgregarMaterial(
            PlanMejora plan,
            ResultadoHabitabilidad original,
            Simulador simulador,
            String horario,
            Superficie superficie,
            MejoraMaterial mejora,
            String id,
            int horizonteAnios) {

        // 1. Simular la mejora temporal.
        List<ResultadoHabitabilidad> simulados =
                simularMejoraMaterial(
                        simulador,
                        horario,
                        superficie,
                        mejora.deltaRk);

        // 2. Medir la reduccion real del exceso.
        double reduccionReal =
                medirReduccionReal(original, simulados);

        // 3. Descartar cambios sin efecto.
        if (reduccionReal <= 0.0) {
            return false;
        }

        // 4. Calcular el costo de la intervencion.
        double costoInicial =
                mejora.costoPorM2 * superficie.getArea();

        double costoMantenimiento =
                mejora.mantenimientoPorM2 * superficie.getArea();

        // 5. Calcular el tiempo de vida usando
        // la reduccion medida, no la estimada.
        double aniosDeVida = Solucion.aniosDeVida(
                reduccionReal,
                original.getExceso(),
                mejora.degradacionDbPorAnio);

        // 6. Crear una solucion verificada.
        Solucion solucion = new Solucion(
                id,
                Solucion.Tipo.MEJORAR_MATERIAL,
                original.getHabitacion().getId(),
                "Superficie " + superficie.getId() + " (" + superficie.getTipo() + "): "
                    + mejora.nombre
                    + String.format(" (aumento de R_k: +%.1f dB)", mejora.deltaRk),
                reduccionReal,
                costoInicial,
                aniosDeVida,
                costoMantenimiento,
                horizonteAnios,
                true
        );

        // 7. Insertar en el arbol rojo-negro.
        return plan.agregar(solucion);
    }


    /**
     * Comprueba que la mejora tenga sentido
     * para el tipo de superficie.
     */
    private static boolean compatible(
            Superficie superficie,
            MejoraMaterial mejora) {

        String tipo = superficie.getTipo()
                .toLowerCase(Locale.ROOT);

        if (tipo.contains("ventana")
                || tipo.contains("vidrio")) {
            return mejora.nombre.contains("vidrio");
        }

        if (tipo.contains("losa")) {
            // Por ahora, solo el refuerzo de +12 dB.
            return mejora.deltaRk == 12.0;
        }

        if (tipo.contains("muro")
                || tipo.contains("pared")) {
            return !mejora.nombre.contains("vidrio");
        }

        // Las puertas y otros cerramientos necesitarán
        // un catálogo específico en la etapa 3.
        return false;
    }


    /**
     * Identifica las superficies del camino dominante
     * asociado al resultado de habitabilidad.
     */
    private static Set<String> superficiesDelCaminoDominante(
            ResultadoHabitabilidad resultado,
            Simulador simulador,
            String horario) {

        Grafo grafo =
                Grafo.desdeEdificio(simulador.getEdificio());

        Habitacion evaluada = resultado.getHabitacion();

        String origen = null;
        String destino = null;
        double mayorAporte = Double.NEGATIVE_INFINITY;

        if (resultado.usaNivelDeVecino()) {

            // ALTO IMPACTO:
            // Buscar el vecino más perjudicado.
            Actividad actividad =
                    evaluada.getActividadPrevista();

            Dijkstra.Resultado caminos =
                    Dijkstra.calcular(
                            grafo, evaluada.getId());

            for (Habitacion vecina :
                    simulador.getEdificio().getHabitaciones()) {

                if (vecina == evaluada) {
                    continue;
                }

                double limite =
                        limiteReceptor(vecina, horario, simulador);

                double atenuacion =
                        caminos.getAtenuacionHasta(vecina.getId());

                if (!Double.isFinite(limite)
                        || !Double.isFinite(atenuacion)) {
                    continue;
                }

                double excesoVecina =
                        actividad.emiteDba
                        - atenuacion
                        - limite;

                if (excesoVecina > mayorAporte) {
                    mayorAporte = excesoVecina;
                    origen = evaluada.getId();
                    destino = vecina.getId();
                }
            }

        } else {

            // RECEPTOR:
            // Buscar la actividad o fuente externa
            // que más ruido aporta a esta habitación.
            destino = evaluada.getId();

            for (Habitacion emisor :
                    simulador.getEdificio().getHabitaciones()) {

                if (emisor == evaluada) {
                    continue;
                }

                double atenuacion =
                        Dijkstra.calcular(
                                grafo, emisor.getId())
                                .getAtenuacionHasta(destino);

                if (!Double.isFinite(atenuacion)) {
                    continue;
                }

                Actividad actividad =
                        emisor.getActividadPrevista();

                if (actividad != null
                        && actividad.emiteDba > 0) {

                    double aporte =
                            actividad.emiteDba - atenuacion;

                    if (aporte > mayorAporte) {
                        mayorAporte = aporte;
                        origen = emisor.getId();
                    }
                }

                for (FuenteSonido fuente :
                        emisor.getFuentes()) {

                    if (fuente.getHorario() == null
                            || !fuente.getHorario().name()
                                    .equalsIgnoreCase(horario)) {
                        continue;
                    }

                    double aporte =
                            fuente.obtenerNivelEmitido()
                            - atenuacion;

                    if (aporte > mayorAporte) {
                        mayorAporte = aporte;
                        origen = emisor.getId();
                    }
                }
            }
        }

        if (origen == null || destino == null) {
            return Set.of();
        }

        Dijkstra.Resultado caminos =
                Dijkstra.calcular(grafo, origen);

        return new LinkedHashSet<>(
                caminos.reconstruirSuperficies(destino));
    }


    /**
     * Obtiene el límite acústico exigido
     * por una habitación receptora.
     */
    private static double limiteReceptor(
            Habitacion vecina,
            String horario,
            Simulador simulador) {

        Actividad actividad =
                vecina.getActividadPrevista();

        if (actividad != null) {
            return actividad.exigeSilencio()
                    ? actividad.limiteMax
                    : Double.NaN;
        }

        try {
            return simulador.getNormativa()
                    .obtenerLimite(
                            vecina.getTipo(), horario);

        } catch (IllegalArgumentException e) {
            return Double.NaN;
        }
    }


}
