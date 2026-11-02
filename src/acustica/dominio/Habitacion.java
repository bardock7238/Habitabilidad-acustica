package acustica.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import acustica.normativa.Normativa;

/**
 * Recinto del edificio que se evalúa acústicamente.
 *
 * Guarda sus dimensiones, las superficies que lo delimitan, las fuentes de
 * sonido que contiene y los parámetros acústicos usados por el modelo:
 * nivel de ruido de fondo y tiempo de reverberación T60.
 */
public class Habitacion {

    private final String id;
    private final String nombre;
    private final String tipo;
    private final double ancho;
    private final double largo;
    private final double alto;
    private final List<Superficie> superficies;
    private final List<FuenteSonido> fuentes;
    private double nivelFondo = 25.0;      // dBA
    private double t60 = 0.5;              // s
    private Actividad actividadPrevista;
    private double nivelRuido;
    private boolean habitable;

    public Habitacion(String id, String nombre, String tipo, double ancho, double largo, double alto) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.ancho = ancho;
        this.largo = largo;
        this.alto = alto;
        this.superficies = new ArrayList<>();
        this.fuentes = new ArrayList<>();
    }

    public double calcularVolumen() {
        return ancho * largo * alto;
    }

    /**
     * Absorción equivalente del recinto (Sabine):
     * {@code A = 0.16 · V / T60}, en m².
     */
    public double calcularAbsorcion() {
        return 0.16 * calcularVolumen() / t60;
    }

    public void agregarSuperficie(Superficie superficie) {
        superficies.add(superficie);
    }

    public void agregarFuente(FuenteSonido fuente) {
        fuentes.add(fuente);
    }

    /**
     * Evalúa la habitación contra los límites de {@link Normativa}.
     * Es un atajo para usos puntuales; el {@code Simulador} aplica además la
     * lógica de actividades y estados.
     */
    public boolean evaluarHabitabilidad(Normativa normativa, String horario) {
        setHabitable(normativa.cumple(nivelRuido, tipo, horario));
        return habitable;
    }

    public void setNivelRuido(double nivelRuido) {
        this.nivelRuido = nivelRuido;
    }

    public double getNivelRuido() {
        return nivelRuido;
    }

    public void setHabitable(boolean habitable) {
        this.habitable = habitable;
    }

    public boolean isHabitable() {
        return habitable;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public double getAncho() {
        return ancho;
    }

    public double getLargo() {
        return largo;
    }

    public double getAlto() {
        return alto;
    }

    /** Vista de solo lectura de las superficies registradas. */
    public List<Superficie> getSuperficies() {
        return Collections.unmodifiableList(superficies);
    }

    /** Vista de solo lectura de las fuentes registradas. */
    public List<FuenteSonido> getFuentes() {
        return Collections.unmodifiableList(fuentes);
    }

    public double getNivelFondo() {
        return nivelFondo;
    }

    public void setNivelFondo(double nivelFondo) {
        if (nivelFondo < 0) {
            throw new IllegalArgumentException("El nivel de fondo no puede ser negativo");
        }
        this.nivelFondo = nivelFondo;
    }

    public double getT60() {
        return t60;
    }

    public void setT60(double t60) {
        if (t60 <= 0) {
            throw new IllegalArgumentException("El T60 debe ser mayor que 0");
        }
        this.t60 = t60;
    }

    public void setActividadPrevista(Actividad actividad) {
        this.actividadPrevista = actividad;
    }

    public Actividad getActividadPrevista() {
        return actividadPrevista;
    }
}
