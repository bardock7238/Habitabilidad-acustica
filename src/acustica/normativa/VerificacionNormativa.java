package acustica.normativa;

import java.util.Map;

import acustica.config.CargadorDatos;

/**
 * Verificación de la tabla de límites de {@link Normativa} y de su carga desde
 * {@code datos/normativa_oms.csv}.
 *
 * Se ejecuta desde la raíz del proyecto con:
 *   javac -d out $(find src -name "*.java")
 *   java -cp out acustica.normativa.VerificacionNormativa
 */
public class VerificacionNormativa {

    private static final double TOLERANCIA = 1e-9;

    public static void main(String[] args) {
        int total = 0;
        int fallos = 0;

        Normativa normativa = new Normativa();

        System.out.println("=== Límites cargados ===");
        normativa.getLimites().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.println(e.getKey() + " = " + e.getValue() + " dB"));

        System.out.println("\n=== Verificaciones ===");

        total++; fallos += verificar("Dormitorio de día = 35 dB",
                cercano(normativa.obtenerLimite("Dormitorio", "dia"), 35.0));
        total++; fallos += verificar("Dormitorio de noche = 30 dB",
                cercano(normativa.obtenerLimite("Dormitorio", "noche"), 30.0));
        total++; fallos += verificar("Sala de día = 35 dB",
                cercano(normativa.obtenerLimite("Sala", "dia"), 35.0));
        total++; fallos += verificar("Sala de noche = 30 dB",
                cercano(normativa.obtenerLimite("Sala", "noche"), 30.0));
        total++; fallos += verificar("Aula de día = 35 dB",
                cercano(normativa.obtenerLimite("Aula", "dia"), 35.0));
        total++; fallos += verificar("Hospital de día = 30 dB",
                cercano(normativa.obtenerLimite("Hospital", "dia"), 30.0));
        total++; fallos += verificar("Hospital de noche = 30 dB",
                cercano(normativa.obtenerLimite("Hospital", "noche"), 30.0));
        total++; fallos += verificar("El límite de noche no es más permisivo que el de día",
                normativa.obtenerLimite("Hospital", "noche")
                        <= normativa.obtenerLimite("Hospital", "dia"));

        total++; fallos += verificar("La clave no distingue mayúsculas/minúsculas",
                cercano(normativa.obtenerLimite("dormitorio", "NOCHE"), 30.0));
        total++; fallos += verificar("Cumple justo en el límite (nivel == límite)",
                normativa.cumple(30.0, "Dormitorio", "noche"));
        total++; fallos += verificar("No cumple por encima del límite",
                !normativa.cumple(30.1, "Dormitorio", "noche"));
        total++; fallos += verificar("Aula de noche no tiene límite definido",
                lanzaIllegalArgument(() -> normativa.obtenerLimite("Aula", "noche")));
        total++; fallos += verificar("Un tipo null lanza IllegalArgumentException",
                lanzaIllegalArgument(() -> normativa.obtenerLimite(null, "dia")));
        total++; fallos += verificar("Espacios comunes usan el sector B de sectores.csv (65 dia / 55 noche)",
                cercano(normativa.obtenerLimite("Parqueadero", "dia"), 65.0)
                        && cercano(normativa.obtenerLimite("Pasillo", "noche"), 55.0)
                        && cercano(normativa.obtenerLimite("Salon comunal", "dia"), 65.0)
                        && cercano(normativa.obtenerLimite("Cuarto de maquinas", "noche"), 55.0));
        total++; fallos += verificar("Estudio: 35 dB de dia y 30 dB de noche",
                cercano(normativa.obtenerLimite("Estudio", "dia"), 35.0)
                        && cercano(normativa.obtenerLimite("Estudio", "noche"), 30.0));
        total++; fallos += verificar("La tabla tiene al menos 7 combinaciones",
                normativa.getLimites().size() >= 7);

        total++; fallos += verificar("Los límites coinciden con datos/normativa_oms.csv",
                coincideConCSV(normativa));

        System.out.println("\n=== Resumen ===");
        System.out.println((total - fallos) + " / " + total + " verificaciones OK");
        if (fallos > 0) {
            System.out.println("HAY " + fallos + " VERIFICACION(ES) FALLIDA(S).");
        } else {
            System.out.println("Todas las verificaciones pasaron correctamente.");
        }
    }

    /**
     * Compara cada fila del CSV con el límite que expone {@link Normativa}.
     * Si el CSV no se puede leer, la verificación falla (hay que ejecutar
     * desde la raíz del proyecto).
     */
    private static boolean coincideConCSV(Normativa normativa) {
        try {
            Map<String, Map<String, Double>> csv = CargadorDatos.cargarNormativaOMS();
            for (Map.Entry<String, Map<String, Double>> recinto : csv.entrySet()) {
                for (Map.Entry<String, Double> horario : recinto.getValue().entrySet()) {
                    double limite = normativa.obtenerLimite(recinto.getKey(), horario.getKey());
                    if (!cercano(limite, horario.getValue())) {
                        return false;
                    }
                }
            }
            return true;
        } catch (RuntimeException sinCSV) {
            System.out.println("       (no se pudo leer datos/normativa_oms.csv: "
                    + sinCSV.getMessage() + ")");
            return false;
        }
    }

    private static boolean lanzaIllegalArgument(Runnable accion) {
        try {
            accion.run();
            return false;
        } catch (IllegalArgumentException esperado) {
            return true;
        }
    }

    private static boolean cercano(double a, double b) {
        return Math.abs(a - b) < TOLERANCIA;
    }

    private static int verificar(String descripcion, boolean condicion) {
        System.out.println((condicion ? "[OK]   " : "[FALLO]") + " " + descripcion);
        return condicion ? 0 : 1;
    }
}
