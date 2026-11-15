package acustica.normativa;

import acustica.dominio.Habitacion;

/**
 * Verificación de {@link ResultadoHabitabilidad}: la diferencia entre
 * {@code nivelRecibido} y {@code nivelFinal}, el caso en que el vecino tiene
 * exactamente el mismo nivel, las etiquetas de exceso y el trato de los
 * resultados NO_EVALUABLE.
 *
 * Se ejecuta desde la raíz del proyecto con:
 *   javac -d out $(find src -name "*.java")
 *   java -cp out acustica.normativa.VerificacionResultadoHabitabilidad
 */
public class VerificacionResultadoHabitabilidad {

    public static void main(String[] args) {
        int total = 0;
        int fallos = 0;

        Habitacion hab = new Habitacion("X", "Habitacion", "Dormitorio", 3, 3.5, 2.5);

        // ---- Límite propio: recibido == final ----
        ResultadoHabitabilidad propio =
                ResultadoHabitabilidad.propio(hab, 40.0, 30.0, EstadoHabitabilidad.NO_HABITABLE);
        total++; fallos += verificar("propio: nivelFinal = nivelRecibido",
                propio.getNivelFinal() == propio.getNivelRecibido());
        total++; fallos += verificar("propio: no usa nivel de vecino", !propio.usaNivelDeVecino());
        total++; fallos += verificar("propio: exceso = 40 - 30 = +10",
                Math.abs(propio.getExceso() - 10.0) < 1e-9);
        total++; fallos += verificar("propio: etiqueta del exceso",
                propio.etiquetaExceso().equals(String.format("%+.1f dB", 10.0)));

        // ---- Alto impacto: el exceso es del vecino, aunque el nivel coincida ----
        ResultadoHabitabilidad vecino =
                ResultadoHabitabilidad.conVecino(hab, 40.0, 40.0, 30.0,
                        EstadoHabitabilidad.NO_HABITABLE);
        total++; fallos += verificar("conVecino: usaNivelDeVecino sigue siendo true con niveles iguales",
                vecino.usaNivelDeVecino());
        total++; fallos += verificar("conVecino: exceso = 40 - 30 = +10",
                Math.abs(vecino.getExceso() - 10.0) < 1e-9);

        ResultadoHabitabilidad vecinoLejano =
                ResultadoHabitabilidad.conVecino(hab, 25.0, 58.2, 55.0,
                        EstadoHabitabilidad.NO_HABITABLE);
        total++; fallos += verificar("conVecino: nivelFinal es el del vecino y recibido el propio",
                Math.abs(vecinoLejano.getNivelFinal() - 58.2) < 1e-9
                        && Math.abs(vecinoLejano.getNivelRecibido() - 25.0) < 1e-9);

        // ---- NO_EVALUABLE y límite +infinito: mismo -infinito, distinta etiqueta ----
        ResultadoHabitabilidad noEvaluable =
                ResultadoHabitabilidad.propio(hab, 40.0, Double.NaN,
                        EstadoHabitabilidad.NO_EVALUABLE);
        total++; fallos += verificar("NO_EVALUABLE: exceso = -infinito",
                noEvaluable.getExceso() == Double.NEGATIVE_INFINITY);
        total++; fallos += verificar("NO_EVALUABLE: etiqueta 'no evaluable'",
                noEvaluable.etiquetaExceso().equals("no evaluable"));

        ResultadoHabitabilidad sinLimite =
                ResultadoHabitabilidad.propio(hab, 40.0, Double.POSITIVE_INFINITY,
                        EstadoHabitabilidad.HABITABLE);
        total++; fallos += verificar("Límite +infinito: exceso = -infinito",
                sinLimite.getExceso() == Double.NEGATIVE_INFINITY);
        total++; fallos += verificar("Límite +infinito: etiqueta 'sin limite'",
                sinLimite.etiquetaExceso().equals("sin limite"));

        System.out.println("\n=== Resumen ===");
        System.out.println((total - fallos) + " / " + total + " verificaciones OK");
        if (fallos > 0) {
            System.out.println("HAY " + fallos + " VERIFICACION(ES) FALLIDA(S).");
        } else {
            System.out.println("Todas las verificaciones pasaron correctamente.");
        }
    }

    private static int verificar(String descripcion, boolean condicion) {
        System.out.println((condicion ? "[OK]   " : "[FALLO]") + " " + descripcion);
        return condicion ? 0 : 1;
    }
}
