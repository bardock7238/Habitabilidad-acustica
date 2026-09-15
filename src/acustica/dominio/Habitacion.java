package acustica.dominio;

import java.util.ArrayList;
import java.util.List;

public class Habitacion {

    private final String id;
    private final String nombre;
    private final String tipo;
    private final double ancho;
    private final double largo;
    private final double alto;
    private final List<Superficie> superficies;
    private final List<FuenteSonido> fuentes;

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

    public void agregarSuperficie(Superficie superficie) {
        superficies.add(superficie);
    }

    public void agregarFuente(FuenteSonido fuente) {
        fuentes.add(fuente);
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

    public List<Superficie> getSuperficies() {
        return superficies;
    }

    public List<FuenteSonido> getFuentes() {
        return fuentes;
    }
}