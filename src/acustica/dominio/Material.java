package acustica.dominio;

/**
 * Material de una superficie.
 *
 * {@code indiceReduccionSonora} es R_k, el índice de reducción sonora en dB a
 * la frecuencia de referencia (500 Hz). El modelo v2 trabaja en banda única,
 * así que no se modela la dependencia con la frecuencia.
 *
 * El espesor se guarda como dato descriptivo (para el informe y el UML); el
 * cálculo acústico no lo usa.
 */
public class Material {

    private final String nombre;
    private final double indiceReduccionSonora;
    private final double espesor;

    public Material(String nombre, double indiceReduccionSonora, double espesor) {
        if (indiceReduccionSonora < 0) {
            throw new IllegalArgumentException("R_k no puede ser negativo");
        }
        if (espesor < 0) {
            throw new IllegalArgumentException("El espesor no puede ser negativo");
        }
        this.nombre = nombre;
        this.indiceReduccionSonora = indiceReduccionSonora;
        this.espesor = espesor;
    }

    /**
     * Devuelve R_k en dB. El parámetro {@code frecuencia} se conserva por
     * compatibilidad con la API original, pero el modelo es de banda única.
     */
    public double obtenerAtenuacion(double frecuencia) {
        return indiceReduccionSonora;
    }

    /** R_k del material, en dB. */
    public double getIndiceReduccionSonora() {
        return indiceReduccionSonora;
    }

    /**
     * Alias histórico de {@link #getIndiceReduccionSonora()}.
     * Se mantiene para no romper el código de las otras ramas.
     */
    public double getCoeficienteAtenuacion() {
        return indiceReduccionSonora;
    }

    public String getNombre() {
        return nombre;
    }

    public double getEspesor() {
        return espesor;
    }

    @Override
    public String toString() {
        return "Material{" + "nombre='" + nombre + '\'' + ", R_k=" + indiceReduccionSonora
                + " dB" + ", espesor=" + espesor + " m" + '}';
    }
}
