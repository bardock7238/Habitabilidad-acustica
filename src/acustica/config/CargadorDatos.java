package acustica.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga los datos de normativa desde los archivos CSV en datos/.
 * No depende de otras clases del proyecto: devuelve estructuras simples
 * (Map, List) que otras clases (como Normativa) pueden consumir después.
 */
public class CargadorDatos {

    /** Representa una fila de sectores.csv (Resolución 0627/2006). */
    public static class SectorRuido {
        public final String sector;
        public final String subsector;
        public final double limiteDiaDb;
        public final double limiteNocheDb;

        public SectorRuido(String sector, String subsector, double limiteDiaDb, double limiteNocheDb) {
            this.sector = sector;
            this.subsector = subsector;
            this.limiteDiaDb = limiteDiaDb;
            this.limiteNocheDb = limiteNocheDb;
        }

        @Override
        public String toString() {
            return "Sector " + sector + " (" + subsector + "): dia=" + limiteDiaDb
                    + " dB, noche=" + limiteNocheDb + " dB";
        }
    }

    /**
     * Carga datos/normativa_oms.csv.
     * Estructura del CSV: recinto,horario,limite_db
     *
     * @return mapa recinto -> (horario -> limite en dB)
     *         ej: resultado.get("Dormitorio").get("noche") -> 30.0
     */
    public static Map<String, Map<String, Double>> cargarNormativaOMS() {
        Map<String, Map<String, Double>> resultado = new HashMap<>();

        List<String[]> filas = leerCSV("datos/normativa_oms.csv");
        for (String[] fila : filas) {
            String recinto = fila[0].trim();
            String horario = fila[1].trim();
            double limite = Double.parseDouble(fila[2].trim());

            resultado
                .computeIfAbsent(recinto, k -> new HashMap<>())
                .put(horario, limite);
        }

        return resultado;
    }

    /**
     * Carga datos/sectores.csv.
     * Estructura del CSV: sector,subsector,limite_dia_db,limite_noche_db
     *
     * @return lista de sectores con sus límites de día y noche
     */
    public static List<SectorRuido> cargarSectores() {
        List<SectorRuido> resultado = new ArrayList<>();

        List<String[]> filas = leerCSV("datos/sectores.csv");
        for (String[] fila : filas) {
            String sector = fila[0].trim();
            String subsector = fila[1].trim();
            double limiteDia = Double.parseDouble(fila[2].trim());
            double limiteNoche = Double.parseDouble(fila[3].trim());

            resultado.add(new SectorRuido(sector, subsector, limiteDia, limiteNoche));
        }

        return resultado;
    }

    /**
     * Lee un archivo CSV y devuelve sus filas como arreglos de columnas.
     * Salta la primera línea (encabezado) y respeta los valores entre
     * comillas que contienen comas (ej: "Zonas residenciales, hoteles...").
     *
     * @param rutaArchivo ruta relativa al archivo CSV (ej: "datos/sectores.csv")
     * @return lista de filas, cada una como arreglo de columnas (sin comillas)
     */
    private static List<String[]> leerCSV(String rutaArchivo) {
        List<String[]> filas = new ArrayList<>();

        try (BufferedReader lector = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            boolean primeraLinea = true;

            while ((linea = lector.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false; // saltar encabezado
                    continue;
                }
                if (linea.isBlank()) {
                    continue;
                }
                filas.add(parsearLineaCSV(linea));
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo: " + rutaArchivo, e);
        }

        return filas;
    }

    /**
     * Separa una línea de CSV por comas, respetando los campos entre comillas
     * dobles (que pueden contener comas internas, como en sectores.csv).
     */
    private static String[] parsearLineaCSV(String linea) {
        List<String> columnas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean dentroDeComillas = false;

        for (char c : linea.toCharArray()) {
            if (c == '"') {
                dentroDeComillas = !dentroDeComillas;
            } else if (c == ',' && !dentroDeComillas) {
                columnas.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        columnas.add(actual.toString());

        return columnas.toArray(new String[0]);
    }

    /**
     * Prueba rápida manual: corre esta clase directamente para ver los datos cargados.
     * java -cp out acustica.config.CargadorDatos
     */
    public static void main(String[] args) {
        System.out.println("=== Normativa OMS ===");
        cargarNormativaOMS().forEach((recinto, horarios) ->
            horarios.forEach((horario, limite) ->
                System.out.println(recinto + " (" + horario + "): " + limite + " dB")
            )
        );

        System.out.println("\n=== Sectores Res. 0627/2006 ===");
        cargarSectores().forEach(System.out::println);
    }
}