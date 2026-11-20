package acustica.visual;

import acustica.dominio.Habitacion;
import acustica.normativa.EstadoHabitabilidad;
import acustica.normativa.ResultadoHabitabilidad;
import java.util.Arrays;
import java.util.List;

/**
 * Verifica los datos para la vista 2D sin ejecutar el simulador.
 * Ejecutar desde la raiz del proyecto:
 * java -cp out acustica.visual.VerificacionDatosVistaEdificio
 */
public final class VerificacionDatosVistaEdificio {
    private static int pruebas = 0;

    private VerificacionDatosVistaEdificio() { }

    public static void main(String[] args) {
        Habitacion dormitorio = habitacion("D201", "Dormitorio 201", 2, 3.0, 4.0, 2.5);
        Habitacion salon = habitacion("SC", "Salon </script> & comunal", 3, 6.0, 5.0, 3.0);
        Habitacion pasillo = habitacion("PA", "Pasillo", 0, 8.0, 1.5, 2.5);
        Habitacion estudio = habitacion("E201", "Estudio 201", 2, 3.0, 3.0, 2.5);

        List<ResultadoHabitabilidad> dia = Arrays.asList(
                ResultadoHabitabilidad.propio(dormitorio, 31.0, 35.0, EstadoHabitabilidad.HABITABLE),
                ResultadoHabitabilidad.conVecino(salon, 47.0, 62.0, 30.0, EstadoHabitabilidad.NO_HABITABLE),
                ResultadoHabitabilidad.propio(pasillo, 44.0, Double.NaN, EstadoHabitabilidad.NO_EVALUABLE),
                ResultadoHabitabilidad.propio(estudio, 42.0, 35.0, EstadoHabitabilidad.TOLERABLE));

        List<ResultadoHabitabilidad> noche = Arrays.asList(
                ResultadoHabitabilidad.propio(dormitorio, 40.0, 30.0, EstadoHabitabilidad.NO_HABITABLE),
                ResultadoHabitabilidad.conVecino(salon, 20.0, 24.0, 30.0, EstadoHabitabilidad.HABITABLE),
                ResultadoHabitabilidad.propio(pasillo, 49.0, Double.NaN, EstadoHabitabilidad.NO_EVALUABLE),
                ResultadoHabitabilidad.propio(estudio, 38.0, 30.0, EstadoHabitabilidad.TOLERABLE));

        DatosVistaEdificio vista = new DatosVistaEdificio(dia, noche);

        comprobar("Escenarios separados para dia y noche",
                vista.getDia() != vista.getNoche()
                && vista.getDia().getHabitaciones().size() == 4
                && vista.getNoche().getHabitaciones().size() == 4
                && vista.getEscenario("DIA") == vista.getDia()
                && vista.getEscenario(" noche ") == vista.getNoche());

        comprobar("Pisos ordenados y consultas por piso",
                vista.getDia().getPisos().toString().equals("[0, 2, 3]")
                && vista.getDia().getHabitacionesDePiso(2).size() == 2
                && vista.getDia().getHabitacionesDePiso(999).isEmpty());

        DatosVistaEdificio.HabitacionVista salonDia = buscar(vista.getDia(), "SC");
        comprobar("Nivel recibido distinto del nivel final de un vecino",
                salonDia.getNivelRecibido() == 47.0
                && salonDia.getNivelFinal() == 62.0
                && salonDia.getLimiteNormativo() == 30.0
                && salonDia.usaNivelDeVecino());

        DatosVistaEdificio.HabitacionVista dormitorioDia = buscar(vista.getDia(), "D201");
        comprobar("Identificador, nombre, piso y dimensiones",
                dormitorioDia.getId().equals("D201")
                && dormitorioDia.getNombre().equals("Dormitorio 201")
                && dormitorioDia.getPiso() == 2
                && dormitorioDia.getAncho() == 3.0
                && dormitorioDia.getLargo() == 4.0
                && dormitorioDia.getAlto() == 2.5
                && !dormitorioDia.usaNivelDeVecino());

        comprobar("Se conservan los cuatro estados",
                buscar(vista.getDia(), "D201").getEstado() == EstadoHabitabilidad.HABITABLE
                && buscar(vista.getDia(), "E201").getEstado() == EstadoHabitabilidad.TOLERABLE
                && buscar(vista.getDia(), "SC").getEstado() == EstadoHabitabilidad.NO_HABITABLE
                && buscar(vista.getDia(), "PA").getEstado() == EstadoHabitabilidad.NO_EVALUABLE);

        comprobar("Limite ausente se representa como null",
                buscar(vista.getDia(), "PA").getLimiteNormativo() == null);

        comprobar("Cada horario conserva sus propios niveles y estados",
                buscar(vista.getNoche(), "D201").getNivelRecibido() == 40.0
                && buscar(vista.getDia(), "D201").getNivelRecibido() == 31.0
                && buscar(vista.getNoche(), "D201").getEstado() == EstadoHabitabilidad.NO_HABITABLE);

        // Simula cambios posteriores en el objeto mutable: la vista debe ser una copia.
        dormitorio.setNivelRuido(99.0);
        dormitorio.setPiso(8);
        comprobar("Los datos capturados no cambian al modificar Habitacion",
                buscar(vista.getDia(), "D201").getPiso() == 2
                && buscar(vista.getNoche(), "D201").getPiso() == 2
                && buscar(vista.getDia(), "D201").getNivelRecibido() == 31.0);

        boolean inmutable = false;
        try {
            vista.getDia().getHabitaciones().clear();
        } catch (UnsupportedOperationException esperada) {
            inmutable = true;
        }
        comprobar("Listas de la vista inmutables", inmutable);

        String json = vista.toJson();
        comprobar("JSON contiene ambos horarios y niveles por separado",
                json.startsWith("{\"dia\":")
                && json.contains("\"noche\":")
                && json.contains("\"nivelRecibido\":47.0")
                && json.contains("\"nivelFinal\":62.0")
                && json.contains("\"limiteNormativo\":null")
                && json.contains("\"estado\":\"NO_EVALUABLE\""));

        comprobar("JSON escapa texto peligroso para incrustar en HTML",
                !json.contains("</script>")
                && json.contains("\\u003c/script\\u003e")
                && json.contains("\\u0026"));

        System.out.println("TODAS LAS PRUEBAS PASARON (" + pruebas + "/" + pruebas + ")");
    }

    private static Habitacion habitacion(String id, String nombre, int piso,
            double ancho, double largo, double alto) {
        Habitacion h = new Habitacion(id, nombre, "Dormitorio", ancho, largo, alto);
        h.setPiso(piso);
        return h;
    }

    private static DatosVistaEdificio.HabitacionVista buscar(DatosVistaEdificio.Escenario escenario,
            String id) {
        for (DatosVistaEdificio.HabitacionVista h : escenario.getHabitaciones()) {
            if (h.getId().equals(id)) {
                return h;
            }
        }
        throw new AssertionError("No se encontro habitacion " + id);
    }

    private static void comprobar(String descripcion, boolean condicion) {
        if (!condicion) {
            throw new AssertionError("FALLO: " + descripcion);
        }
        pruebas++;
        System.out.println("[OK] " + descripcion);
    }
}
