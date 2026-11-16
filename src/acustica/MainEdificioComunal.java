package acustica;

import acustica.dominio.Actividad;
import acustica.dominio.Edificio;
import acustica.dominio.Habitacion;
import acustica.dominio.Material;
import acustica.dominio.Superficie;
import acustica.mejora.GeneradorSoluciones;
import acustica.mejora.PlanMejora;
import acustica.mejora.Solucion;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.Normativa;
import acustica.normativa.ResultadoHabitabilidad;
import acustica.simulacion.ReporteHabitabilidad;
import acustica.simulacion.Simulador;
import acustica.visual.GraficadorArboles;
import java.util.List;


/**
 * Escenario de un edificio con zonas comunes, en tres niveles:
 *
 *   Nivel 3:  Salon comunal (FIESTA), encima del Dormitorio 202 y junto al pasillo
 *   Nivel 2:  Pasillo + Apto 201 (Dormitorio, Estudio) + Apto 202 (Dormitorio)
 *   Nivel 1:  Parqueadero (VEHICULOS) bajo el Apto 201, Cuarto de maquinas
 *             (MAQUINARIA) bajo el Dormitorio 202
 *
 * El pasillo no tiene actividad: se evalua con el limite del sector B
 * residencial de datos/sectores.csv (65 dB dia, 55 dB noche).
 *
 * Ejecutar desde la raiz del proyecto:
 *   java -cp out acustica.MainEdificioComunal
 */
public class MainEdificioComunal {

    public static void main(String[] args) {

        // ---------- Materiales ----------
        Material concreto = new Material("Concreto", 55, 0.2);   // muros
        Material losa = new Material("Losa de concreto", 50, 0.2); // entre niveles
        Material drywall = new Material("Drywall", 30, 0.1);
        Material madera = new Material("Puerta de madera", 25, 0.04);
        Material vidrio = new Material("Vidrio doble", 30, 0.01);
        Material metal = new Material("Porton metalico", 20, 0.01);

        // ---------- Habitaciones ----------
        Habitacion parqueadero = new Habitacion("PQ", "Parqueadero", "Parqueadero", 10, 6, 2.5);
        Habitacion cuartoMaquinas = new Habitacion("CM", "Cuarto de maquinas", "Cuarto de maquinas", 3, 3, 2.5);
        Habitacion pasillo = new Habitacion("PA", "Pasillo", "Pasillo", 8, 1.5, 2.5);
        Habitacion dormitorio201 = new Habitacion("D201", "Dormitorio 201", "Dormitorio", 3, 3.5, 2.5);
        Habitacion estudio201 = new Habitacion("E201", "Estudio 201", "Estudio", 3, 3, 2.5);
        Habitacion dormitorio202 = new Habitacion("D202", "Dormitorio 202", "Dormitorio", 3, 3.5, 2.5);
        Habitacion salonComunal = new Habitacion("SC", "Salon comunal", "Salon comunal", 6, 5, 3);

        // ---------- Pisos ----------
        parqueadero.setPiso(1);
        cuartoMaquinas.setPiso(1);
        pasillo.setPiso(2);
        dormitorio201.setPiso(2);
        estudio201.setPiso(2);
        dormitorio202.setPiso(2);
        salonComunal.setPiso(3);

        // ---------- Actividades ----------
        parqueadero.setActividadPrevista(Actividad.VEHICULOS);
        cuartoMaquinas.setActividadPrevista(Actividad.MAQUINARIA);
        salonComunal.setActividadPrevista(Actividad.FIESTA);
        dormitorio201.setActividadPrevista(Actividad.DORMIR);
        dormitorio202.setActividadPrevista(Actividad.DORMIR);
        estudio201.setActividadPrevista(Actividad.ESTUDIAR);
        // El pasillo no tiene actividad: se evalua con Normativa (sector B).

        Edificio edificio = new Edificio("Edificio con zonas comunes", "OMS + Res. 0627/2006");
        for (Habitacion h : List.of(parqueadero, cuartoMaquinas, pasillo,
                dormitorio201, estudio201, dormitorio202, salonComunal)) {
            edificio.agregarHabitacion(h);
        }

        // ---------- Superficies ----------
        // Entre niveles (losas)
        unir("LOSA-PQ-D201", "losa", 10.5, losa, parqueadero, dormitorio201);
        unir("LOSA-PQ-E201", "losa", 9.0, losa, parqueadero, estudio201);
        unir("LOSA-CM-D202", "losa", 9.0, losa, cuartoMaquinas, dormitorio202);
        unir("LOSA-SC-D202", "losa", 10.5, losa, salonComunal, dormitorio202);
        // Mismo nivel
        unir("M-CM-PQ", "muro", 7.5, concreto, cuartoMaquinas, parqueadero);
        unir("M-D201-E201", "muro", 7.5, drywall, dormitorio201, estudio201);
        unir("MED-201-202", "muro medianero", 8.75, concreto, dormitorio201, dormitorio202);
        // Puertas al pasillo y acceso al salon comunal
        unir("P-PA-D201", "puerta", 2.0, madera, pasillo, dormitorio201);
        unir("P-PA-E201", "puerta", 2.0, madera, pasillo, estudio201);
        unir("P-PA-D202", "puerta", 2.0, madera, pasillo, dormitorio202);
        unir("P-PA-SC", "puerta", 2.0, madera, pasillo, salonComunal);
        // Fachadas
        unir("V-D201", "ventana", 1.3, vidrio, dormitorio201, null);
        unir("V-D202", "ventana", 1.3, vidrio, dormitorio202, null);
        unir("PORTON", "porton", 12.0, metal, parqueadero, null);

        // ---------- Simulacion ----------
        Simulador simulador = new Simulador(edificio, new Normativa());
        GraficadorArboles grafico = new GraficadorArboles(edificio.getNombre());
        System.out.println("=== " + edificio.getNombre() + " (" + edificio.getNormativaAplicable() + ") ===");

        
        System.out.println("\n--- Evaluacion DIA ---");

        List<ResultadoHabitabilidad> resultadosDia =
                simulador.ejecutar("dia");

        imprimirResultados(resultadosDia);

        // Ahora el reporte recibe el simulador y el horario:
        // puede verificar realmente las mejoras de material.
        ReporteHabitabilidad.imprimir(
                resultadosDia, simulador, "dia");

        grafico.agregarEscenario(
                "Evaluación DÍA",
                resultadosDia,
                simulador,
                "dia");

        // Pruebas de aceptación del issue #40.
        verificarIssue40Dia(
                simulador, edificio, resultadosDia);

        System.out.println("\n--- Evaluacion NOCHE ---");

        List<ResultadoHabitabilidad> resultadosNoche =
                simulador.ejecutar("noche");

        imprimirResultados(resultadosNoche);

        ReporteHabitabilidad.imprimir(
                resultadosNoche, simulador, "noche");

        grafico.agregarEscenario(
                "Evaluación NOCHE",
                resultadosNoche,
                simulador,
                "noche");


        grafico.guardarYAbrir("out/arboles.html");
    }

    

    /**
     * Pruebas de aceptación del issue #40.
     *
     * Comprueba:
     * 1. Losa del salón +12 dB: 7/7 habitables.
     * 2. Puerta del salón: no resuelve todo el edificio.
     * 3. Ventana D202: no resuelve todo el edificio.
     * 4. El rojo-negro no marca esas dos superficies
     *    como soluciones suficientes.
     *
     * Todos los cambios de material son temporales.
     */
    private static void verificarIssue40Dia(
            Simulador simulador,
            Edificio edificio,
            List<ResultadoHabitabilidad> resultadosBase) {

        System.out.println(
                "\n=== PRUEBAS DE ACEPTACION ISSUE #40 ===");

        String[] superficies = {
            "LOSA-SC-D202",
            "P-PA-SC",
            "V-D202"
        };

        // ------------------------------------------
        // 1. Simular cada modificación temporalmente
        // ------------------------------------------

        for (String id : superficies) {

            Superficie objetivo = null;

            for (Habitacion habitacion :
                    edificio.getHabitaciones()) {

                for (Superficie superficie :
                        habitacion.getSuperficies()) {

                    if (id.equals(superficie.getId())) {
                        objetivo = superficie;
                        break;
                    }
                }

                if (objetivo != null) {
                    break;
                }
            }

            if (objetivo == null) {
                throw new IllegalStateException(
                        "No se encontro la superficie: " + id);
            }

            Material original = objetivo.getMaterial();
            long habitables;

            try {
                Material mejorado = new Material(
                        original.getNombre() + " (+12 dB prueba)",
                        original.getIndiceReduccionSonora() + 12.0,
                        original.getEspesor());

                objetivo.setMaterial(mejorado);

                List<ResultadoHabitabilidad> prueba =
                        simulador.ejecutar("dia");

                habitables = prueba.stream()
                        .filter(ResultadoHabitabilidad::isHabitable)
                        .count();

                System.out.println(
                        "  " + id + " (+12 dB): "
                                + habitables + "/"
                                + prueba.size() + " habitables");

            } finally {

                // Siempre devolver el material original.
                objetivo.setMaterial(original);

                // Restaurar también el estado del simulador.
                simulador.ejecutar("dia");
            }

            if ("LOSA-SC-D202".equals(id)) {

                if (habitables != 7) {
                    throw new IllegalStateException(
                            "ERROR: la losa +12 dB no produjo 7/7");
                }

            } else {

                if (habitables == 7) {
                    throw new IllegalStateException(
                            "ERROR: " + id
                                    + " aparece resolviendo todo el edificio");
                }
            }
        }

        // ------------------------------------------
        // 2. Revisar las soluciones del rojo-negro
        // ------------------------------------------

        boolean losaVerificadaYSuficiente = false;

        for (ResultadoHabitabilidad resultado :
                resultadosBase) {

            if (resultado.getEstado()
                    != EstadoHabitabilidad.NO_HABITABLE) {
                continue;
            }

            PlanMejora plan = GeneradorSoluciones.generar(
                    resultado, 10, simulador, "dia");

            if (plan == null) {
                throw new IllegalStateException(
                        "Falta el plan de mejora de "
                                + resultado.getHabitacion().getId());
            }

            for (Solucion solucion : plan.todas()) {

                // Una estimación nunca se acepta como
                // solución comprobada.
                if (!solucion.isVerificada()) {
                    continue;
                }

                boolean cubreExceso =
                        solucion.getReduccionDb()
                                >= plan.getExcesoDb();

                String descripcion =
                        solucion.getDescripcion();

                // Controles negativos.
                if (cubreExceso
                        && (descripcion.contains("P-PA-SC")
                            || descripcion.contains("V-D202"))) {

                    throw new IllegalStateException(
                            "ERROR: falsa solucion suficiente: "
                                    + descripcion);
                }

                // Control positivo para el salón comunal.
                if (cubreExceso
                        && resultado.getHabitacion()
                                .getId().equals("SC")
                        && descripcion.contains("LOSA-SC-D202")) {

                    losaVerificadaYSuficiente = true;
                }
            }
        }

        if (!losaVerificadaYSuficiente) {
            throw new IllegalStateException(
                    "ERROR: el rojo-negro no reconoce "
                            + "la losa del salon como solucion "
                            + "verificada y suficiente");
        }

        System.out.println(
                "  [OK] Losa del salon verificada en el rojo-negro");

        System.out.println(
                "  [OK] Puerta del salon y ventana D202 "
                        + "sin falsos cumplimientos");

        System.out.println(
                "  [OK] Materiales originales restaurados");

        System.out.println(
                "=== ISSUE #40: PRUEBAS SUPERADAS ===");
    }

    
    /** Crea la superficie y la registra en las habitaciones que separa. */
    private static void unir(String id, String tipo, double area, Material material,
            Habitacion a, Habitacion b) {
        Superficie superficie = (b == null)
                ? new Superficie(id, tipo, area, material, a)
                : new Superficie(id, tipo, area, material, a, b);
        a.agregarSuperficie(superficie);
        if (b != null) {
            b.agregarSuperficie(superficie);
        }
    }

    private static void imprimirResultados(List<ResultadoHabitabilidad> resultados) {
        for (ResultadoHabitabilidad resultado : resultados) {
            System.out.println(resultado);
        }
        long habitables = resultados.stream().filter(ResultadoHabitabilidad::isHabitable).count();
        System.out.println(String.format("Habitables: %d/%d (%.1f%%)", habitables, resultados.size(),
                resultados.isEmpty() ? 0.0 : 100.0 * habitables / resultados.size()));
    }
}
