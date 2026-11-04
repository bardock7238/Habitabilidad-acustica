package acustica;

import java.util.List;

import acustica.dominio.Actividad;
import acustica.dominio.Edificio;
import acustica.dominio.Habitacion;
import acustica.dominio.Material;
import acustica.dominio.Superficie;
import acustica.normativa.Normativa;
import acustica.normativa.ResultadoHabitabilidad;
import acustica.simulacion.Simulador;

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
        System.out.println("=== " + edificio.getNombre() + " (" + edificio.getNormativaAplicable() + ") ===");

        System.out.println("\n--- Evaluacion DIA ---");
        imprimirResultados(simulador.ejecutar("dia"));

        System.out.println("\n--- Evaluacion NOCHE ---");
        imprimirResultados(simulador.ejecutar("noche"));
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
