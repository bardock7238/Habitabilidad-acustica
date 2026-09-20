package acustica.dominio;

public class Material {
    private final String nombre;
    private final double atenuacion;
    private final double espesor;
    public Material(String nombre, double atenuacion, double espesor){
        this.nombre = nombre;
        this.atenuacion = atenuacion;
        this.espesor = espesor;
    }
    public double obtenerAtenuacion(double frecuencia){
        return atenuacion;
    }
    public double getCoeficienteAtenuacion(){
        return atenuacion;
    }
    public String getNombre(){
        return nombre;
    }
    public double getEspesor(){
        return espesor;
    }
    @Override public String toString(){
        return "Material{" +"nombre='" + nombre + '\'' + ", atenuacion=" + atenuacion + " dB" + ", espesor=" + espesor + " m" +'}';
    }

}
