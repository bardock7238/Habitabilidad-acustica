package acustica.dominio;
 
// Representa una fuente de ruido dentro de una habitación.
public class FuenteSonido {
 
    // Tipo de fuente según su posición: fija o móvil.
    public enum Tipo {
        FIJA,
        MOVIL
    }
 
    //Horario en el que la fuente está activa (afecta el límite normativo aplicable). */
    public enum Horario {
        DIA,
        NOCHE
    }
 
    private String id;
    private Tipo tipo;
    private double nivelSonido; // en decibeles (dB)
    private double frecuencia;  // en Hercios (Hz)
    private Horario horario;
 
    public FuenteSonido(String id, Tipo tipo, double nivelSonido, double frecuencia, Horario horario) {
        this.id = id;
        this.tipo = tipo;
        this.nivelSonido = nivelSonido;
        this.frecuencia = frecuencia;
        this.horario = horario;
    }
 
    /**
     * Devuelve el nivel de sonido emitido por la fuente, en decibeles (dB).
     * Este es el valor que usará el motor de simulación (Dijkstra / RayTracing)
     * como punto de partida antes de aplicar la atenuación de las superficies.
     */
    public double obtenerNivelEmitido() {
        return nivelSonido;
    }
 
    // --- Getters y setters ---
 
    public String getId() {
        return id;
    }
 
    public void setId(String id) {
        this.id = id;
    }
 
    public Tipo getTipo() {
        return tipo;
    }
 
    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }
 
    public double getNivelSonido() {
        return nivelSonido;
    }
 
    public void setNivelSonido(double nivelSonido) {
        this.nivelSonido = nivelSonido;
    }
 
    public double getFrecuencia() {
        return frecuencia;
    }
 
    public void setFrecuencia(double frecuencia) {
        this.frecuencia = frecuencia;
    }
 
    public Horario getHorario() {
        return horario;
    }
 
    public void setHorario(Horario horario) {
        this.horario = horario;
    }
 
    @Override
    public String toString() {
        return "FuenteSonido{" +
                "id='" + id + '\'' +
                ", tipo=" + tipo +
                ", nivelSonido=" + nivelSonido + " dB" +
                ", frecuencia=" + frecuencia + " Hz" +
                ", horario=" + horario +
                '}';
    }
}
