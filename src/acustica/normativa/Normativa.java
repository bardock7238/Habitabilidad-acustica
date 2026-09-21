package acustica.normativa;
 
import java.util.HashMap;
import java.util.Map;
 
/**
 * Guarda los límites de ruido (dB) según el tipo de recinto y el horario.
 * La clave del Map tiene la forma "tipo-horario" (ej: "dormitorio-noche")
 * y el valor es el límite en dB.
 * No depende de ninguna otra clase del proyecto.
 */
public class Normativa {
 
    private final Map<String, Double> limites;
 
    public Normativa() {
        limites = new HashMap<>();
        // Valores tomados de datos/normativa_oms.csv
        limites.put(construirClave("Dormitorio", "noche"), 30.0);
        limites.put(construirClave("Sala", "dia"), 35.0);
        limites.put(construirClave("Aula", "dia"), 35.0);
        limites.put(construirClave("Hospital", "dia"), 30.0);
        limites.put(construirClave("Hospital", "noche"), 40.0);
    }
 
    /** Arma la clave "tipo-horario" en minúsculas, para que no importe cómo se escriba. */
    private String construirClave(String tipo, String horario) {
        return tipo.trim().toLowerCase() + "-" + horario.trim().toLowerCase();
    }
 
    /**
     * Devuelve el límite en dB para ese tipo de recinto y horario.
     * Lanza IllegalArgumentException si la combinación no existe.
     */
    public double obtenerLimite(String tipo, String horario) {
        String clave = construirClave(tipo, horario);
        Double limite = limites.get(clave);
        if (limite == null) {
            throw new IllegalArgumentException("No hay límite definido para: " + clave);
        }
        return limite;
    }
 
    /** Devuelve true si el nivel medido no supera el límite (es decir, es habitable). */
    public boolean cumple(double nivel, String tipo, String horario) {
        return nivel <= obtenerLimite(tipo, horario);
    }
}
 

