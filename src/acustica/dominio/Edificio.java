package acustica.dominio;

import java.util.ArrayList;
import java.util.List;

public class Edificio {

    private final String nombre;
    private final String normativaAplicable;
    private final List<Habitacion> habitaciones;

    public Edificio(String nombre) {
        this(nombre, "");
    }

    public Edificio(String nombre, String normativaAplicable) {
        this.nombre = nombre;
        this.normativaAplicable = normativaAplicable;
        this.habitaciones = new ArrayList<>();
    }

    public void agregarHabitacion(Habitacion habitacion) {
        habitaciones.add(habitacion);
    }

    public Habitacion buscarHabitacion(String id) {
        for (Habitacion habitacion : habitaciones) {
            if (habitacion.getId().equals(id)) {
                return habitacion;
            }
        }
        return null;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNormativaAplicable() {
        return normativaAplicable;
    }

    public List<Habitacion> getHabitaciones() {
        return habitaciones;
    }
}