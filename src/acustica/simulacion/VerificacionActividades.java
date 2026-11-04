package acustica.simulacion;

import java.util.List;

import acustica.dominio.Actividad;
import acustica.dominio.Edificio;
import acustica.dominio.FuenteSonido;
import acustica.dominio.Habitacion;
import acustica.dominio.Material;
import acustica.dominio.Superficie;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.Normativa;
import acustica.normativa.ResultadoHabitabilidad;

/**
 * Verificación del árbol de decisión por actividad (Actividad + Simulador).
 *
 * Se ejecuta desde la raíz del proyecto con:
 *   javac -d out $(find src -name "*.java")
 *   java -cp out acustica.simulacion.VerificacionActividades
 */
public class VerificacionActividades {

    public static void main(String[] args) {
        int total = 0;
        int fallos = 0;

        // ---- Categorías (Nodos 1 y 3) ----
        total++; fallos += verificar("DORMIR, ESTUDIAR y LEER exigen silencio",
                Actividad.DORMIR.exigeSilencio() && Actividad.ESTUDIAR.exigeSilencio()
                        && Actividad.LEER.exigeSilencio());
        total++; fallos += verificar("CONVERSAR y LLAMADAS son moderadas",
                Actividad.CONVERSAR.categoria == Actividad.Categoria.MODERADA
                        && Actividad.LLAMADAS.categoria == Actividad.Categoria.MODERADA);
        total++; fallos += verificar("INSTRUMENTO, EJERCICIO y ENTRETENIMIENTO son de alto impacto",
                Actividad.INSTRUMENTO.esAltoImpacto() && Actividad.EJERCICIO.esAltoImpacto()
                        && Actividad.ENTRETENIMIENTO.esAltoImpacto());
        total++; fallos += verificar("Las que exigen silencio tienen limiteMax = 40 (Nodo 2)",
                Actividad.DORMIR.limiteMax == 40 && Actividad.ESTUDIAR.limiteMax == 40
                        && Actividad.LEER.limiteMax == 40);
        total++; fallos += verificar("limiteOptimo <= limiteMax en las que exigen silencio",
                Actividad.DORMIR.limiteOptimo <= Actividad.DORMIR.limiteMax
                        && Actividad.ESTUDIAR.limiteOptimo <= Actividad.ESTUDIAR.limiteMax);

        // ---- Exige silencio: se mide el ruido que llega (Nodo 2) ----
        // Fuente en la sala vecina; muro de drywall (R=30) o concreto (R=55).
        total++; fallos += verificar("DORMIR con fuente de 75 dB y muro de drywall -> NO_HABITABLE",
                estadoDeDormitorio(30, 75.0) == EstadoHabitabilidad.NO_HABITABLE);
        total++; fallos += verificar("DORMIR con fuente de 65 dB y muro de drywall -> TOLERABLE (30 < nivel <= 40)",
                estadoDeDormitorio(30, 65.0) == EstadoHabitabilidad.TOLERABLE);
        total++; fallos += verificar("DORMIR con fuente de 75 dB y muro de concreto -> HABITABLE",
                estadoDeDormitorio(55, 75.0) == EstadoHabitabilidad.HABITABLE);
        total++; fallos += verificar("DORMIR sin ruido de otros espacios -> HABITABLE",
                estadoDeDormitorio(55, 0.0) == EstadoHabitabilidad.HABITABLE);

        // ---- Moderada: habitable (Nodo 3 = NO) ----
        total++; fallos += verificar("CONVERSAR siempre es HABITABLE",
                estadoDeSala(Actividad.CONVERSAR, 30) == EstadoHabitabilidad.HABITABLE);

        // ---- Alto impacto: aislamiento = emiteDba - limite del vecino (Nodo 4) ----
        // INSTRUMENTO emite 85 dBA; el dormitorio vecino tolera 40 -> hacen falta ~45 dB.
        total++; fallos += verificar("INSTRUMENTO con muro de drywall (R=30) -> NO_HABITABLE",
                estadoDeSala(Actividad.INSTRUMENTO, 30) == EstadoHabitabilidad.NO_HABITABLE);
        total++; fallos += verificar("INSTRUMENTO con muro de concreto (R=55) -> HABITABLE",
                estadoDeSala(Actividad.INSTRUMENTO, 55) == EstadoHabitabilidad.HABITABLE);
        total++; fallos += verificar("ENTRETENIMIENTO (75) exige menos aislamiento que EJERCICIO (90)",
                estadoDeSala(Actividad.ENTRETENIMIENTO, 40) == EstadoHabitabilidad.HABITABLE
                        && estadoDeSala(Actividad.EJERCICIO, 40) == EstadoHabitabilidad.NO_HABITABLE);
        total++; fallos += verificar("Alto impacto sin vecinos que exijan nada -> HABITABLE",
                estadoAislada(Actividad.INSTRUMENTO) == EstadoHabitabilidad.HABITABLE);

        System.out.println("\n=== Resumen ===");
        System.out.println((total - fallos) + " / " + total + " verificaciones OK");
        if (fallos > 0) {
            System.out.println("HAY " + fallos + " VERIFICACION(ES) FALLIDA(S).");
        } else {
            System.out.println("Todas las verificaciones pasaron correctamente.");
        }
    }

    /** Dormitorio (DORMIR) con una sala vecina que tiene una fuente de {@code nivelFuente} dB de día. */
    private static EstadoHabitabilidad estadoDeDormitorio(double rMuro, double nivelFuente) {
        Habitacion dormitorio = new Habitacion("D", "Dormitorio", "Dormitorio", 3, 3.5, 2.5);
        Habitacion sala = new Habitacion("S", "Sala", "Sala", 3, 3.5, 2.5);
        dormitorio.setActividadPrevista(Actividad.DORMIR);
        if (nivelFuente > 0) {
            sala.agregarFuente(new FuenteSonido("F", FuenteSonido.Tipo.FIJA, nivelFuente, 500,
                    FuenteSonido.Horario.DIA));
        }
        return evaluar(dormitorio, sala, rMuro).get(0).getEstado();
    }

    /** Sala con la actividad dada, pegada a un dormitorio sin actividad (límite OMS de día: 35 dB). */
    private static EstadoHabitabilidad estadoDeSala(Actividad actividad, double rMuro) {
        Habitacion dormitorio = new Habitacion("D", "Dormitorio", "Dormitorio", 3, 3.5, 2.5);
        Habitacion sala = new Habitacion("S", "Sala", "Sala", 3, 3.5, 2.5);
        sala.setActividadPrevista(actividad);
        return evaluar(dormitorio, sala, rMuro).get(1).getEstado();
    }

    /** Sala con actividad, sin vecinos conectados. */
    private static EstadoHabitabilidad estadoAislada(Actividad actividad) {
        Habitacion sala = new Habitacion("S", "Sala", "Sala", 3, 3.5, 2.5);
        sala.setActividadPrevista(actividad);
        Edificio edificio = new Edificio("Aislada", "OMS");
        edificio.agregarHabitacion(sala);
        return new Simulador(edificio, new Normativa()).ejecutar("dia").get(0).getEstado();
    }

    /** Dos habitaciones separadas por un muro de 8 m² con el índice R dado. Devuelve [primera, segunda]. */
    private static List<ResultadoHabitabilidad> evaluar(Habitacion a, Habitacion b, double rMuro) {
        Material material = new Material("Muro", rMuro, 0.2);
        Superficie muro = new Superficie("M", "muro", 8.0, material, a, b);
        a.agregarSuperficie(muro);
        b.agregarSuperficie(muro);
        Edificio edificio = new Edificio("Prueba", "OMS");
        edificio.agregarHabitacion(a);
        edificio.agregarHabitacion(b);
        return new Simulador(edificio, new Normativa()).ejecutar("dia");
    }

    private static int verificar(String descripcion, boolean condicion) {
        System.out.println((condicion ? "[OK]   " : "[FALLO]") + " " + descripcion);
        return condicion ? 0 : 1;
    }
}
