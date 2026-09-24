package acustica;

import java.util.ArrayList;
import java.util.List;

import acustica.dominio.Edificio;
import acustica.dominio.FuenteSonido;
import acustica.dominio.Habitacion;
import acustica.dominio.Material;
import acustica.dominio.Superficie;
import acustica.normativa.Normativa;
import acustica.normativa.ResultadoHabitabilidad;
import acustica.simulacion.Simulador;

/**
 * Punto de entrada del programa (Tarea 5.2).
 *
 * Construye un edificio de ejemplo completo (habitaciones, materiales,
 * superficies y fuentes de sonido), selecciona la normativa a aplicar y
 * ejecuta el Simulador para el horario de día y de noche.
 *
 * Escenario: un edificio residencial de SEIS apartamentos vecinos en fila
 * (Apto 101 a 106), cada uno con dormitorio y sala, conectados entre sí
 * por sus paredes medianeras — el caso real de habitabilidad acústica
 * entre unidades vecinas, con propagación a varios saltos a lo largo de
 * todo el edificio.
 *
 * Las habitaciones y sus muros se generan con un bucle (en vez de
 * escribir cada apartamento a mano) porque son 6 unidades idénticas en
 * estructura; las fuentes de sonido sí se agregan una por una, porque
 * cada una representa una situación distinta.
 */
public class Main {

    private static final int CANTIDAD_APARTAMENTOS = 6;

    public static void main(String[] args) {

        // ---------- 1. Materiales ----------
        Material concreto = new Material("Concreto", 6.0, 0.2);   // muros solidos: medianeras y fachadas
        Material drywall = new Material("Drywall", 4.0, 0.1);     // particiones internas de cada apartamento
        Material vidrio = new Material("Vidrio doble", 20.0, 0.01); // ventanas con buen aislamiento acustico

        // ---------- 2. Habitaciones + 3. Superficies (generadas en bucle) ----------
        List<Habitacion> dormitorios = new ArrayList<>();
        List<Habitacion> salas = new ArrayList<>();
        Edificio edificio = new Edificio("Edificio Ejemplo", "OMS");

        for (int i = 1; i <= CANTIDAD_APARTAMENTOS; i++) {
            String numeroApto = "10" + i; // 101, 102, ..., 106

            Habitacion dormitorio = new Habitacion(
                    "D" + numeroApto, "Dormitorio " + numeroApto, "Dormitorio", 3, 3.5, 2.5);
            Habitacion sala = new Habitacion(
                    "S" + numeroApto, "Sala " + numeroApto, "Sala", 4, 4.5, 2.5);

            // Particion interna del apartamento (dormitorio <-> sala, mismo apto)
            Superficie muroInterno = new Superficie(
                    "M-" + numeroApto, "muro", 7.0, drywall, sala, dormitorio);
            sala.agregarSuperficie(muroInterno);
            dormitorio.agregarSuperficie(muroInterno);

            // Fachadas: ventana del dormitorio y pared exterior de la sala
            Superficie ventana = new Superficie(
                    "V-" + numeroApto, "ventana", 1.3, vidrio, dormitorio);
            dormitorio.agregarSuperficie(ventana);

            Superficie fachadaSala = new Superficie(
                    "F-" + numeroApto, "pared exterior", 9.5, concreto, sala);
            sala.agregarSuperficie(fachadaSala);

            dormitorios.add(dormitorio);
            salas.add(sala);
            edificio.agregarHabitacion(dormitorio);
            edificio.agregarHabitacion(sala);
        }

        // Medianeras: cada apartamento comparte pared con el siguiente en la fila
        // (Dormitorio 101-102, 102-103, 103-104, 104-105, 105-106).
        for (int i = 0; i < CANTIDAD_APARTAMENTOS - 1; i++) {
            Habitacion actual = dormitorios.get(i);
            Habitacion siguiente = dormitorios.get(i + 1);

            Superficie medianera = new Superficie(
                    "M-MED" + (i + 1), "muro medianero", 8.0, concreto, actual, siguiente);
            actual.agregarSuperficie(medianera);
            siguiente.agregarSuperficie(medianera);
        }

        // ---------- 4. Fuentes de sonido ----------
        // Aqui es donde se GENERAN las fuentes: cada FuenteSonido guarda su propio
        // nivelSonido, y obtenerNivelEmitido() (Tarea 1.2) es el valor de partida
        // que despues usan Dijkstra/RayTracing para calcular cuanto llega a cada
        // habitacion tras restar la atenuacion de las superficies en el camino.
        // (dormitorios.get(0) = Apto 101, get(1) = Apto 102, ... get(5) = Apto 106)

        // F-TV = Televisor, en la sala del Apto 101, de dia.
        FuenteSonido televisor = new FuenteSonido(
                "F-TV", FuenteSonido.Tipo.FIJA, 65.0, 1000.0, FuenteSonido.Horario.DIA);
        salas.get(0).agregarFuente(televisor);

        // F-VE = Ventilador propio, en el dormitorio del Apto 101, de noche.
        FuenteSonido ventilador = new FuenteSonido(
                "F-VE", FuenteSonido.Tipo.FIJA, 28.0, 120.0, FuenteSonido.Horario.NOCHE);
        dormitorios.get(0).agregarFuente(ventilador);

        // F-AS = Aspiradora, en la sala del Apto 102, de dia.
        FuenteSonido aspiradora = new FuenteSonido(
                "F-AS", FuenteSonido.Tipo.MOVIL, 70.0, 400.0, FuenteSonido.Horario.DIA);
        salas.get(1).agregarFuente(aspiradora);

        // F-PL = Parlante del vecino ruidoso, en el dormitorio del Apto 103, de noche.
        FuenteSonido parlanteVecino = new FuenteSonido(
                "F-PL", FuenteSonido.Tipo.MOVIL, 55.0, 300.0, FuenteSonido.Horario.NOCHE);
        dormitorios.get(2).agregarFuente(parlanteVecino);

        // F-LV = Lavadora, en la sala del Apto 105, de dia.
        FuenteSonido lavadora = new FuenteSonido(
                "F-LV", FuenteSonido.Tipo.FIJA, 58.0, 150.0, FuenteSonido.Horario.DIA);
        salas.get(4).agregarFuente(lavadora);

        // F-CV = Conversacion, en el dormitorio del Apto 106, de noche.
        FuenteSonido conversacion = new FuenteSonido(
                "F-CV", FuenteSonido.Tipo.MOVIL, 45.0, 300.0, FuenteSonido.Horario.NOCHE);
        dormitorios.get(5).agregarFuente(conversacion);

        // ---------- 5. Normativa ----------
        Normativa normativa = new Normativa();

        // ---------- 6. Mostrar las fuentes generadas ----------
        // Aqui se ve explicitamente el "nacimiento" del ruido: cada fuente
        // emitiendo su nivel, antes de que la simulacion aplique atenuaciones.
        System.out.println("=== Fuentes de sonido del edificio (antes de propagarse) ===");
        for (Habitacion habitacion : edificio.getHabitaciones()) {
            for (FuenteSonido fuente : habitacion.getFuentes()) {
                System.out.println(
                        habitacion.getNombre() + " -> fuente " + fuente.getId()
                                + " (" + fuente.getTipo() + ", " + fuente.getHorario() + "): "
                                + "obtenerNivelEmitido() = " + fuente.obtenerNivelEmitido() + " dB");
            }
        }

        // ---------- 7. Simulacion ----------
        Simulador simulador = new Simulador(edificio, normativa);

        System.out.println("\n=== " + edificio.getNombre() + " (normativa: " + edificio.getNormativaAplicable()
                + ", " + CANTIDAD_APARTAMENTOS + " apartamentos) ===\n");

        System.out.println("--- Evaluacion DIA ---");
        List<ResultadoHabitabilidad> resultadosDia = simulador.ejecutar("dia");
        imprimirResultados(resultadosDia);

        System.out.println("\n--- Evaluacion NOCHE ---");
        List<ResultadoHabitabilidad> resultadosNoche = simulador.ejecutar("noche");
        imprimirResultados(resultadosNoche);
    }

    private static void imprimirResultados(List<ResultadoHabitabilidad> resultados) {
        for (ResultadoHabitabilidad resultado : resultados) {
            System.out.println(resultado);
        }

        resultados.stream()
                .max((a, b) -> Double.compare(a.getExceso(), b.getExceso()))
                .ifPresent(peorCaso -> System.out.println(
                        "Peor caso: " + peorCaso.getHabitacion().getNombre()
                                + " (exceso de " + String.format("%.1f", peorCaso.getExceso()) + " dB)"));
    }
}